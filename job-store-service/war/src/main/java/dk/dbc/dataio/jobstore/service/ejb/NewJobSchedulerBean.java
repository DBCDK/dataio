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

/**
 * Created by ja7 on 11-04-16.
 *
 * Handle Chunk Scheduling.
 */
@Stateless
public class NewJobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewJobSchedulerBean.class);

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
        getProxyToSelf().submitIfPosibleForProcessing( chunk, sink );

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
    public void submitIfPosibleForProcessing(ChunkEntity chunk, Sink sink) {
        LOGGER.info(" void submitIfPosibleForProcessing(ChunkEntity chunk, Sink sink)");
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
        dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_DELEVERING);
    }

    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW )
    public void chunkDeleveringDone( Chunk chunk ) throws JSONBException, JobStoreException {
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
}
