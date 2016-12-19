package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.getPrSinkStatusForSinkId;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ja7 on 03-07-16.
 *
 * Helper Bean for JobScheduler and JobSchedulerBulkSubmitterBean
 *
 * Methods needing its Own Transaction is pushed to this class
 *
 */
@Stateless
public class JobSchedulerTransactionsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerTransactionsBean.class);


    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    PgJobStoreRepository jobStoreRepository;

    @EJB
    SinkMessageProducerBean sinkMessageProducerBean;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducerBean;




    /**
     * Force new Chunk to Store before Async SubmitIfPossibleForProcessing.
     * New Transaction to ensure Record is on Disk before async submit
     *
     * Updates WaitingOn with chunks with matching keys
     *
     * @param e Dependency tracking Entity
     * @param waitForKey Extra Key not part of this dependencyTrackingEntry, but we need to wait for chunks with this key.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void persistDependencyEntity(DependencyTrackingEntity e, String waitForKey) {

        getPrSinkStatusForSinkId(e.getSinkid()).processingStatus.readyForQueue.incrementAndGet();

        final List<DependencyTrackingEntity.Key> chunksToWaitFor = findChunksToWaitFor(e.getSinkid(), e.getMatchKeys(), waitForKey);

        e.setWaitingOn(chunksToWaitFor);
        entityManager.persist(e);
    }

    /**
     * Force new Chunk to Store before Async SubmitIfPossibleForProcessing.
     * New Transaction to ensure Record is on Disk before async submit
     *
     * Updates WaitingOn with chunks with matching keys
     *
     * @param e Dependency tracking Entity
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void persistJobTerminationDependencyEntity(DependencyTrackingEntity e) {

        getPrSinkStatusForSinkId(e.getSinkid()).processingStatus.readyForQueue.incrementAndGet();

        final List<DependencyTrackingEntity.Key> chunksToWaitFor = findJobBarrier(e.getSinkid(), e.getKey().getJobId(), e.getMatchKeys());

        e.setWaitingOn(chunksToWaitFor);
        e.setStatus( DependencyTrackingEntity.ChunkProcessStatus.BLOCKED );

        if( chunksToWaitFor.isEmpty() ) {
            e.setStatus(DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER);
        }
        entityManager.persist(e);
    }



    /**
     * Send JMS message to Processing, if queue size is lower then MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
     *
     * @param chunk  Chunk to send to JMS queue
     * @param sinkId SinkId
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Asynchronous
    public void submitToProcessingIfPossibleAsync(ChunkEntity chunk, long sinkId) {
        submitToProcessingIfPossible( chunk, sinkId);
    }


    /**
     * Send JMS message to Processing, if queue size is lower then MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
     *
     * @param chunk                    Chunk to send to JMS queue
     * @param sinkId                   SinkId
     */
    @TransactionAttribute( TransactionAttributeType.REQUIRED)
    @Stopwatch
    public void submitToProcessingIfPossible( ChunkEntity chunk, long sinkId) {
        LOGGER.info(" void submitToProcessingIfPossible(ChunkEntity chunk, Sink sink)");


        JobSchedulerPrSinkQueueStatuses.QueueStatus prSinkQueueStatus = getPrSinkStatusForSinkId(sinkId).processingStatus;

        if (! prSinkQueueStatus.isDirectSubmitMode() ) return;

        if (prSinkQueueStatus.jmsEnqueued.intValue() >= MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
            prSinkQueueStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
            return;
        }
        submitToProcessing( chunk, prSinkQueueStatus );
    }


    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW )
    @Stopwatch
    public void submitToProcessing( ChunkEntity chunk, JobSchedulerPrSinkQueueStatuses.QueueStatus prSinkQueueStatus) {


        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getKey());
        final DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        if (dependencyTrackingEntity == null) {
            LOGGER.error("Internal Error unable to lookup chunk {} in submitToProcessing", key);
            return;
        }

        // recheck check if chunk is found by bulk and DIRECT mode
        if ( dependencyTrackingEntity.getStatus() != DependencyTrackingEntity.ChunkProcessStatus.READY_TO_PROCESS ) {
            return;
        }

        dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_PROCESS);
        try {
            jobProcessorMessageProducerBean.send(getChunkFrom(chunk));
            prSinkQueueStatus.jmsEnqueued.incrementAndGet();
        } catch (JobStoreException e) {
            LOGGER.error("Unable to send processing notification for {}", chunk.getKey().toString(), e);
        }
    }


    /**
     * Send JMS message to Sink with chunk.
     *
     * @param chunk The chunk to submit to delivering queue
     * @param dependencyTrackingEntity Tracking Entity for chunk
     */
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    @Stopwatch
    public void submitToDeliveringIfPossible(Chunk chunk, DependencyTrackingEntity dependencyTrackingEntity)  {
        LOGGER.info("Trying to submit {} to Delivering", dependencyTrackingEntity.getKey());
        dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER);

        JobSchedulerPrSinkQueueStatuses.QueueStatus sinkStatus = getPrSinkStatusForSinkId(dependencyTrackingEntity.getSinkid()).deliveringStatus;

        if (!sinkStatus.isDirectSubmitMode()) return;


        int queuedToDelivering = sinkStatus.jmsEnqueued.intValue();
        if (queuedToDelivering >= MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK) {
            sinkStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
            LOGGER.info("chunk {} blocked by queue size {} ", dependencyTrackingEntity.getKey(), queuedToDelivering);
            return;
        }

        submitToDelivering(chunk, dependencyTrackingEntity, sinkStatus);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToDeliveringNewTransaction(Chunk chunk, JobSchedulerPrSinkQueueStatuses.QueueStatus sinkStatus) {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk);
        final DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        submitToDelivering( chunk, dependencyTrackingEntity, sinkStatus);

    }


    private void submitToDelivering(Chunk chunk, DependencyTrackingEntity dependencyTrackingEntity, JobSchedulerPrSinkQueueStatuses.QueueStatus sinkStatus) {
        // recheck with chunk status with chunk Locked before sending to
        if( dependencyTrackingEntity.getStatus() != DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER ) {
            return;
        }

        final JobEntity jobEntity = jobStoreRepository.getJobEntityById((int) chunk.getJobId());
        // Chunk is ready for Sink
        try {
            sinkMessageProducerBean.send(chunk, jobEntity);
            sinkStatus.jmsEnqueued.incrementAndGet();
            LOGGER.info("chunk {} submitted to Delivering", dependencyTrackingEntity.getKey());
            dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_DELIVERY);
        } catch (JobStoreException e) {
            LOGGER.error("Unable to send chunk {} to jmsQueue Sink Set to BULK for retransmit", e);
            sinkStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
        }
    }


    /**
     * Finding lists with which contains any of chunks keys
     *
     * @param sinkId sinkId
     * @param matchKeys Set of match keys
     * @param waitForKey Extra key to check for.
     * @return Returns List of Chunks To wait for.
     */
    List<DependencyTrackingEntity.Key> findChunksToWaitFor(int sinkId, Set<String> matchKeys, String waitForKey) {
        if (matchKeys.isEmpty() && waitForKey==null) return new ArrayList<>();

        HashSet extraKeys=null;
        if (waitForKey != null) {
            extraKeys = new HashSet<>();
            extraKeys.add( waitForKey );
        }
        Query query = entityManager.createNativeQuery(buildFindChunksToWaitForQuery(sinkId, matchKeys, extraKeys), "JobIdChunkIdResult");
        return query.getResultList();
    }

    /**
     * Finding lists of chunks in dependency Tracking with this jobId and MatchKey.
     * Note only First key in waitForKey is checked.
     *
     * @param sinkId sinkId
     * @param jobId jobId for witch chunks to wait for  barrier
     * @param waitForKey dataSetID
     * @return Returns List of Chunks To wait for.
     */
    List<DependencyTrackingEntity.Key> findJobBarrier(int sinkId, int jobId, Set<String> waitForKey) {

        Query query = entityManager.createNamedQuery(DependencyTrackingEntity.CHUNKS_PR_SINK_JOBID);
        query.setParameter(1, sinkId);
        query.setParameter(2, jobId);
        query.setParameter(3, waitForKey );

        return query.getResultList();
    }



    public Chunk getChunkFrom(ChunkEntity chunk) {
        try {
            ChunkEntity.Key chunkKey = chunk.getKey();
            return jobStoreRepository.getChunk(Chunk.Type.PARTITIONED, chunkKey.getJobId(), chunkKey.getId());
        } catch (RuntimeException ex) {
            LOGGER.error("Internal error Unable to get PARTITIONED items for {}", chunk.getKey());
            throw ex;
        }
    }

    public Chunk getProcessedChunkFrom(DependencyTrackingEntity dependencyTrackingEntity) {
        try {
            DependencyTrackingEntity.Key dtKey = dependencyTrackingEntity.getKey();
            ChunkEntity.Key chunkKey = new ChunkEntity.Key(dtKey.getChunkId(), dtKey.getJobId());

            return jobStoreRepository.getChunk(Chunk.Type.PROCESSED, chunkKey.getJobId(), chunkKey.getId());
        } catch (RuntimeException ex) {
            LOGGER.error("Internal error Unable to get PROCESSED items for {}", dependencyTrackingEntity.getKey(), ex);
            throw ex;
        }
    }

    /**
     * @param sinkId    Sink Id to find
     * @param matchKeys Set of keys any chunk with any key is returned
     * @param alsoWaitForKeys extra keys to wait for
     * @return NativeQuery for find chunks to wait for using @>
     */
    String buildFindChunksToWaitForQuery(int sinkId, Set<String> matchKeys, Set<String> alsoWaitForKeys) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append("select jobId, chunkId from dependencyTracking where sinkId=");
        builder.append(sinkId);
        builder.append(" and ( ");

        Set<String> keys=matchKeys;
        if( alsoWaitForKeys != null) {
            keys=new HashSet<>(matchKeys);
            keys.addAll(alsoWaitForKeys);
        }

        Boolean first = true;
        for (String key : keys) {
            if (!first) builder.append(" or ");
            builder.append("matchKeys @> '[\"");
            builder.append(escapeSql(key));
            builder.append("\"]'");
            first = false;
        }
        builder.append(" )");
        builder.append(" ORDER BY jobId, chunkId for update");
        return builder.toString();
    }

    private String escapeSql(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("'", "''");
    }
}
