package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.*;
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
 * Handle Chunk Scheduling.
 */
// Proc kan vis ikke være singelton Bean managet da vi bruge entityManager
//     public ConcurrentHashMap<Long,AtomicInteger> queuedToProcessingCounter = new ConcurrentHashMap<>(16, 0.9F, 1);
// skal flytte ud i sin egen Klasse.
@Stateless
public class NewJobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewJobSchedulerBean.class);

    // Or simulate by filling up counter ?
    // Make
    //static final int MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK = 1000;
    static final int MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK = 1000;

    // Hash use to keep a count of pending jobs in JMS queus pr sink.
    // Smalle EJB virolation for performance.
    // If the Application is run in multible jvm's the limiter is pr jvm not pr application

    static final ConcurrentHashMap<Long,AtomicInteger> queuedToProcessingCounter = new ConcurrentHashMap<>(16, 0.9F, 1);
    static final ConcurrentHashMap<Long,AtomicInteger> queudToDeliveringCounter = new ConcurrentHashMap<>(16, 0.9F, 1);


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
     * @throws JobStoreException if unable to setup monitoring
     */
    @Stopwatch
    public void scheduleChunk(ChunkEntity chunk, Sink sink) throws  JobStoreException{
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");

        getProxyToSelf().persistDependencyEntity(chunk, sink);
        getProxyToSelf().submitIfPosibleForProcessingAsync( chunk, sink.getId() );

    }

    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void persistDependencyEntity(ChunkEntity chunk, Sink sink) {
        int sinkId=(int)sink.getId();

        DependencyTrackingEntity e=new DependencyTrackingEntity( chunk, sinkId );
        e.setWaitingOn( findChunksToWaitFor( sinkId, e.getMatchKeys()));
        entityManager.persist( e );
    }

    @Asynchronous
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW)
    public void submitIfPosibleForProcessingAsync( ChunkEntity chunk, long sinkId ) {
        submitIfPosibleForProcessing( chunk, sinkId );
    }

    @Stopwatch
    public void submitIfPosibleForProcessing(ChunkEntity chunk, long sinkId) {
        LOGGER.info(" void submitIfPosibleForProcessing(ChunkEntity chunk, Sink sink)");

        if( incrementAndReturnCurrentQueuedToProcessing( sinkId ) > MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
            decrementAndReturnCurrentQueuedToProcessing( sinkId );
            return ;
        }
        DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getKey());
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_PROCESS);
        try {
            jobProcessorMessageProducerBean.send( getChunkFrom(chunk));
        } catch (JobStoreException e) {
            LOGGER.error("Unable to send processing notification for {}", chunk.getKey().toString(), e);
        }
    }

    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW )
    public void chunkProcessingDone( Chunk chunk ) throws JobStoreException {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getJobId(), chunk.getChunkId() );
        DependencyTrackingEntity dependencyTrackingEntity=entityManager.find( DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        if( dependencyTrackingEntity.getWaitingOn().size() != 0) {
            dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkProcessStatus.BLOCKED);
            LOGGER.debug("chunk ");
            return ;
        }

        int queuedToProcessing=decrementAndReturnCurrentQueuedToProcessing( dependencyTrackingEntity.getSinkid() );
        if( queuedToProcessing < MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
            LOGGER.info("Space for more jobs");
            Query query=entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkid and e.status=:state")
            .setParameter("sinkid", dependencyTrackingEntity.getSinkid())
            .setParameter("state", DependencyTrackingEntity.ChunkProcessStatus.READY_TO_PROCESS)
            .setMaxResults(MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK -queuedToProcessing);
            List<DependencyTrackingEntity> chunks=query.getResultList();
            for( DependencyTrackingEntity toSchedule : chunks ) {
                DependencyTrackingEntity.Key toScheduleKey=toSchedule.getKey();
                LOGGER.info(" Chunk ready to schedule {}",toScheduleKey);
                ChunkEntity ch=entityManager.find( ChunkEntity.class, new ChunkEntity.Key( toScheduleKey.getChunkId(), toScheduleKey.getJobId()));
                getProxyToSelf().submitIfPosibleForProcessing(ch, dependencyTrackingEntity.getSinkid());
            }
        }

        sendChunkToDelevering(chunk, dependencyTrackingEntity);
    }

    /**
     * Send JMS message to Sink with chunk.
     *
     * TODO: extend to handel sink rate limiting
     * @param chunk
     * @param dependencyTrackingEntity
     * @throws JobStoreException if unable to
     */
    private void sendChunkToDelevering(Chunk chunk, DependencyTrackingEntity dependencyTrackingEntity) throws JobStoreException {
        final JobEntity jobEntity = jobStoreRepository.getJobEntityById((int) chunk.getJobId());
        // Chunk is ready for Sink
        sinkMessageProducerBean.send( chunk, jobEntity );
        dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_DELIVERY);
    }

    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW )
    public void chunkDeliveringDone(Chunk chunk ) throws JSONBException, JobStoreException {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getJobId(), chunk.getChunkId() );
        DependencyTrackingEntity doneChunk=entityManager.find( DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        List<DependencyTrackingEntity.Key> chunksWaitingForMe=findChunksWaitingForMe( doneChunk.getKey());

        for( DependencyTrackingEntity.Key blockChunkKey: chunksWaitingForMe) {
            DependencyTrackingEntity blockedChunk=entityManager.find( DependencyTrackingEntity.class, blockChunkKey, LockModeType.PESSIMISTIC_WRITE);

            blockedChunk.getWaitingOn().remove( doneChunk.getKey());

            if( blockedChunk.getWaitingOn().size() == 0 ) {
                sendChunkToDelevering( getProcessedChunkFrom( blockedChunk ),blockedChunk);
            }
        }

        entityManager.remove( doneChunk );
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
     * Retuens List of Chunks To wait for.
     * @param matchKeys
     * @return
     */
    List<DependencyTrackingEntity.Key> findChunksToWaitFor(int sinkId, Set<String> matchKeys ) {
        if( matchKeys.isEmpty() ) return new ArrayList<>();

        Query query=entityManager.createNativeQuery( buildFindChunksToWaitForQuery( sinkId, matchKeys ), "JobIdChunkIdResult");
        List<DependencyTrackingEntity.Key> result=query.getResultList();
        return result;
    }


    List<DependencyTrackingEntity.Key> findChunksWaitingForMe( DependencyTrackingEntity.Key key ) throws JSONBException {
        String keyAsJson= ConverterJSONBContext.getInstance().marshall(key);
        Query query=entityManager.createNativeQuery("select jobid, chunkid from dependencyTracking where waitingon @> '["+ keyAsJson +"]'" , "JobIdChunkIdResult");
        return query.getResultList();
    }

    /**
     * @param sinkId  Sink Id to find
     * @param matchKeys  Set of keys any chunk with any key is returned
     * @return  NativeQuery for find chunks to wait for using @>
     */
    String buildFindChunksToWaitForQuery( int sinkId, Set<String> matchKeys ) {
        StringBuilder builder= new StringBuilder(1000);
        builder.append("select jobid, chunkid from dependencyTracking where sinkId=");
        builder.append( sinkId );
        builder.append(" and ( ");


        Boolean first=true;
        for( String key: matchKeys ) {
            if( ! first ) builder.append(" or ");
            builder.append("matchkeys @> '[\"");
            builder.append(key);
            builder.append("\"]'");
            first=false;
        }
        builder.append(" )");
        builder.append(" for update");
        return builder.toString();
    }

    static int incrementAndReturnCurrentQueuedToDelivering(Sink sink) {
        AtomicInteger sinkCounter=queudToDeliveringCounter.computeIfAbsent(sink.getId(), k -> new AtomicInteger(0) );
        return sinkCounter.incrementAndGet();
    }

    static int decrementAndReturnCurrentQueuedToDelivering(Sink sink) {
        AtomicInteger sinkCounter=queudToDeliveringCounter.computeIfAbsent(sink.getId(), k -> new AtomicInteger(0) );
        return sinkCounter.decrementAndGet();
    }


    static int incrementAndReturnCurrentQueuedToProcessing(long sinkId) {
        AtomicInteger sinkCounter=queuedToProcessingCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.incrementAndGet();
    }

    static int decrementAndReturnCurrentQueuedToProcessing(long sinkId) {
        AtomicInteger sinkCounter=queuedToProcessingCounter.computeIfAbsent(sinkId, k -> new AtomicInteger(0) );
        return sinkCounter.decrementAndGet();
    }

}
