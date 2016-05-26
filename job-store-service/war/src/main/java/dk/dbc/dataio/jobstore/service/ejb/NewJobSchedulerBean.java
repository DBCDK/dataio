package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkProcessStatus;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ja7 on 11-04-16.
 *
 * Handle Chunk Scheduling. Chunks Travels thu the ChunkProcessStatus stages.
 *
 * HACK:
 * Limits is pr jvm process.
 */
@Stateless
public class NewJobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewJobSchedulerBean.class);

    // Max JMS Size pr Sink
    static final int MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK = 1000;
    static final int MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK = 100;

    // Hash use to keep a count of pending jobs in JMS queues pr sink.
    // Small EJB violation for performance.
    // If the Application is run in multiple JVM's the limiter is pr jvm not pr application

    static final ConcurrentHashMap<Long,AtomicInteger> queuedToProcessingCounter = new ConcurrentHashMap<>(16, 0.9F, 1);
    static final ConcurrentHashMap<Long,AtomicInteger> queuedToDeliveringCounter = new ConcurrentHashMap<>(16, 0.9F, 1);


    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducerBean;
    @EJB
    SinkMessageProducerBean sinkMessageProducerBean;
    @EJB
    private PgJobStoreRepository jobStoreRepository;

    @Resource
    private SessionContext sessionContext;


    private NewJobSchedulerBean getProxyToSelf() {
        return sessionContext.getBusinessObject(NewJobSchedulerBean.class);
    }

    /**
     * ScheduleChunk

     * Passes given chunk collision detection element and sink on to the
     * sequence analyser and notifies pipeline of next available workload (if any)
     * @param chunk next chunk element to enter into sequence analysis
     * @param sink sink associated with chunk
     * @throws NullPointerException if given any null-valued argument
     */
    @Stopwatch
    public void scheduleChunk(ChunkEntity chunk, Sink sink) {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");

        getProxyToSelf().persistDependencyEntity(chunk, sink);
        getProxyToSelf().submitToProcessingIfPossibleAsync( chunk, sink.getId() );

    }

    /**
     * Force new Chunk to Store before Async SubmitIfPossibleForProcessing.
     * New Transaction to ensure Record is on Disk before async submit
     * @param chunk new Chunk
     * @param sink Destination Sink
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void persistDependencyEntity(ChunkEntity chunk, Sink sink) {
        int sinkId=(int)sink.getId();

        DependencyTrackingEntity e=new DependencyTrackingEntity( chunk, sinkId );
        e.setWaitingOn( findChunksToWaitFor( sinkId, e.getMatchKeys()));
        entityManager.persist( e );
    }

    /**
     * Send JMS message to Processing, if queue size is lower then MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
     * @param chunk ???
     * @param sinkId ???
     */
    @Asynchronous
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW)
    public void submitToProcessingIfPossibleAsync(ChunkEntity chunk, long sinkId ) {
        submitToProcessingIfPossible( chunk, sinkId );
    }

    @Stopwatch
    public void submitToProcessingIfPossible(ChunkEntity chunk, long sinkId) {
        LOGGER.info(" void submitToProcessingIfPossible(ChunkEntity chunk, Sink sink)");

        if( incrementAndReturnCurrentQueuedToProcessing( sinkId ) > MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
            decrementAndReturnCurrentQueuedToProcessing( sinkId );
            return ;
        }
        DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getKey());
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        dependencyTrackingEntity.setStatus(ChunkProcessStatus.QUEUED_TO_PROCESS);
        try {
            jobProcessorMessageProducerBean.send( getChunkFrom(chunk));
        } catch (JobStoreException e) {
            LOGGER.error("Unable to send processing notification for {}", chunk.getKey().toString(), e);
        }
    }

    /**
     * Register Chunk Processing is Done.
     * Chunks not i state QUEUED_TO_PROCESS is ignored.
     *
     * @param chunk Chunk completed from processing
     * @throws JobStoreException if Unable to Load Chunk
     */
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    public void chunkProcessingDone( Chunk chunk ) throws JobStoreException {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getJobId(), chunk.getChunkId() );
        DependencyTrackingEntity dependencyTrackingEntity=entityManager.find( DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        if( dependencyTrackingEntity.getStatus() != ChunkProcessStatus.QUEUED_TO_PROCESS ) {
            LOGGER.info( "chunkProcessingDone called with chunk not in state QUEUED_TO_PROCESS {} was {} ", key, dependencyTrackingEntity.getStatus());
            return ;
        }


        if( dependencyTrackingEntity.getWaitingOn().size() != 0) {
            dependencyTrackingEntity.setStatus(ChunkProcessStatus.BLOCKED);
            LOGGER.debug("chunk {} blocked by {} ", key, dependencyTrackingEntity.getWaitingOn());
            return ;
        }

        // Send chunk to Delivering
        submitToDeliveringIfPossible(chunk, dependencyTrackingEntity);

        // Check for more READY_TO_PROCESS chunks.
        int sinkId = dependencyTrackingEntity.getSinkid();
        int queuedToProcessing=decrementAndReturnCurrentQueuedToProcessing(sinkId);
        if( queuedToProcessing < MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
            LOGGER.info("Space for more jobs");
            Query query=entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkId and e.status=:state")
            .setParameter("sinkId", sinkId)
            .setParameter("state", ChunkProcessStatus.READY_TO_PROCESS)
            .setMaxResults(MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK -queuedToProcessing);
            List<DependencyTrackingEntity> chunks=query.getResultList();
            for( DependencyTrackingEntity toSchedule : chunks ) {
                DependencyTrackingEntity.Key toScheduleKey=toSchedule.getKey();
                LOGGER.info(" Chunk ready to schedule {} to Processing",toScheduleKey);
                ChunkEntity ch=entityManager.find( ChunkEntity.class, new ChunkEntity.Key( toScheduleKey.getChunkId(), toScheduleKey.getJobId()));
                submitToProcessingIfPossible(ch, sinkId);
            }
        }
    }

    /**
     * Send JMS message to Sink with chunk.
     *

     * @param dependencyTrackingEntity Tracking Entity for chunk*
     * @throws JobStoreException if unable to
     */
    private void submitToDeliveringIfPossible(Chunk chunk, DependencyTrackingEntity dependencyTrackingEntity) throws JobStoreException {
        LOGGER.info("Trying to submit {} to Delivering", dependencyTrackingEntity.getKey());
        dependencyTrackingEntity.setStatus( ChunkProcessStatus.READY_TO_DELIVER );

        int queuedToDelivering=incrementAndReturnCurrentQueuedToDelivering( dependencyTrackingEntity.getSinkid() );
        if( queuedToDelivering > MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK) {
            decrementAndReturnCurrentQueuedToDelivering( dependencyTrackingEntity.getSinkid() );
            LOGGER.info("chunk {} blocked by queue size {} ", dependencyTrackingEntity.getKey(), queuedToDelivering);
            return ;
        }

        final JobEntity jobEntity = jobStoreRepository.getJobEntityById((int) chunk.getJobId());
        // Chunk is ready for Sink
        sinkMessageProducerBean.send( chunk, jobEntity );
        LOGGER.info("chunk {} submitted to Delivering", dependencyTrackingEntity.getKey());
        dependencyTrackingEntity.setStatus(ChunkProcessStatus.QUEUED_TO_DELIVERY);
    }


    /**
     * Register a chunk as Delivered, and remove it from dependency tracking.
     *
     * If called Multiple times with the same chunk, or chunk not in QUEUED_TO_DELIVERY the chunk is ignored
     *
     * @param chunk Chunk Done
     * @throws JSONBException on failure to queue other chunks
     * @throws JobStoreException on failure to queue other chunks
     */
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    public void chunkDeliveringDone(Chunk chunk ) throws JSONBException, JobStoreException {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getJobId(), chunk.getChunkId() );
        DependencyTrackingEntity doneChunk=entityManager.find( DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        if (doneChunk == null) {
            LOGGER.info( "chunkDeliveringDone called with unknown Chunk {} - Assuming it is already completed ", key);
            return ;
        }
        if( doneChunk.getStatus() != ChunkProcessStatus.QUEUED_TO_DELIVERY ) {
            LOGGER.info( "chunkDeliveringDone called with chunk {}, not in state QUEUED_TO_DELIVERY {} -- chunk Ignored", key, doneChunk.getStatus());
            return ;
        }

        // Decrement early to make space for in queue.  -- most important when queue size is 1, when unit testing

        long doneChunkSinkId=doneChunk.getSinkid();
        int queuedToDelivering=decrementAndReturnCurrentQueuedToDelivering( doneChunkSinkId );
        LOGGER.info("After chunk {} returned from delivering {} is queuedToDelivering", doneChunk.getKey(), queuedToDelivering);

        List<DependencyTrackingEntity.Key> chunksWaitingForMe=findChunksWaitingForMe( doneChunk.getKey());

        for( DependencyTrackingEntity.Key blockChunkKey: chunksWaitingForMe) {
            DependencyTrackingEntity blockedChunk=entityManager.find( DependencyTrackingEntity.class, blockChunkKey, LockModeType.PESSIMISTIC_WRITE);

            blockedChunk.getWaitingOn().remove( doneChunk.getKey());

            if( blockedChunk.getWaitingOn().size() == 0 ) {
                submitToDeliveringIfPossible( getProcessedChunkFrom( blockedChunk ),blockedChunk);
            }
        }


        entityManager.remove( doneChunk );

        if( queuedToDelivering <= MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK) {
            LOGGER.info("Space for more jobs");
            Query query=entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkId and e.status=:state")
            .setParameter("sinkId", doneChunkSinkId )
            .setParameter("state", ChunkProcessStatus.READY_TO_DELIVER)
            .setMaxResults(MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK -queuedToDelivering+1);
            List<DependencyTrackingEntity> chunks=query.getResultList();
            for( DependencyTrackingEntity toSchedule : chunks ) {
                DependencyTrackingEntity.Key toScheduleKey=toSchedule.getKey();
                LOGGER.info(" Chunk ready to schedule {} for Delivering" ,toScheduleKey);

                submitToDeliveringIfPossible( getProcessedChunkFrom( toSchedule ), toSchedule );
            }
        }

    }


    private Chunk getChunkFrom(ChunkEntity chunk) {
        ChunkEntity.Key chunkKey=chunk.getKey();
        return jobStoreRepository.getChunk( Chunk.Type.PARTITIONED, chunkKey.getJobId(), chunkKey.getId() );
    }

    private Chunk getProcessedChunkFrom(DependencyTrackingEntity dependencyTrackingEntity )
    {
        DependencyTrackingEntity.Key dtKey= dependencyTrackingEntity.getKey();
        ChunkEntity.Key chunkKey=new ChunkEntity.Key( dtKey.getChunkId(), dtKey.getJobId() );

        return jobStoreRepository.getChunk( Chunk.Type.PROCESSED, chunkKey.getJobId(), chunkKey.getId() );
    }


    /**
     * Finding lists with which contains any of chunks keys
     * @param matchKeys Set of match keys
     * @return Returns List of Chunks To wait for.
     */
    List<DependencyTrackingEntity.Key> findChunksToWaitFor(int sinkId, Set<String> matchKeys ) {
        if( matchKeys.isEmpty() ) return new ArrayList<>();

        Query query=entityManager.createNativeQuery( buildFindChunksToWaitForQuery( sinkId, matchKeys ), "JobIdChunkIdResult");
        return query.getResultList();
    }


    List<DependencyTrackingEntity.Key> findChunksWaitingForMe( DependencyTrackingEntity.Key key ) throws JSONBException {
        String keyAsJson= ConverterJSONBContext.getInstance().marshall(key);
        Query query=entityManager.createNativeQuery("select jobid, chunkid from dependencyTracking where waitingOn @> '["+ keyAsJson +"]'" , "JobIdChunkIdResult");
        return query.getResultList();
    }

    /**
     * @param sinkId  Sink Id to find
     * @param matchKeys  Set of keys any chunk with any key is returned
     * @return  NativeQuery for find chunks to wait for using @>
     */
    String buildFindChunksToWaitForQuery( int sinkId, Set<String> matchKeys ) {
        StringBuilder builder= new StringBuilder(1000);
        builder.append("select jobId, chunkId from dependencyTracking where sinkId=");
        builder.append( sinkId );
        builder.append(" and ( ");


        Boolean first=true;
        for( String key: matchKeys ) {
            if( ! first ) builder.append(" or ");
            builder.append("matchKeys @> '[\"");
            builder.append(key);
            builder.append("\"]'");
            first=false;
        }
        builder.append(" )");
        builder.append(" for update");
        return builder.toString();
    }

    static int incrementAndReturnCurrentQueuedToDelivering(long sinkId) {
        AtomicInteger sinkCounter= queuedToDeliveringCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.incrementAndGet();
    }

    static int decrementAndReturnCurrentQueuedToDelivering(long sinkId) {
        AtomicInteger sinkCounter= queuedToDeliveringCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.decrementAndGet();
    }

    static int lookupQueuedToDelivering( long sinkId) {
        AtomicInteger sinkCounter= queuedToDeliveringCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.intValue();
    }

    static void resetQueuedToDelivering( long sinkId, int newValue) {
        AtomicInteger sinkCounter= queuedToDeliveringCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        sinkCounter.set( newValue );
    }


    static int incrementAndReturnCurrentQueuedToProcessing(long sinkId) {
        AtomicInteger sinkCounter=queuedToProcessingCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.incrementAndGet();
    }

    static int decrementAndReturnCurrentQueuedToProcessing(long sinkId) {
        AtomicInteger sinkCounter=queuedToProcessingCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.decrementAndGet();
    }

    static int lookupQueuedToProcessing( long sinkId ) {
        AtomicInteger sinkCounter=queuedToProcessingCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.intValue();
    }

    static void resetQueuedToProcessing( long sinkId, int newValue) {
        AtomicInteger sinkCounter=queuedToProcessingCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
         sinkCounter.set( newValue );
    }



}
