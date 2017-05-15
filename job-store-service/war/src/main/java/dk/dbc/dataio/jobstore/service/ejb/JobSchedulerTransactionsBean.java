package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.getSinkStatus;

/**
 * Helper Bean for JobScheduler and JobSchedulerBulkSubmitterBean.
 * Methods needing to run in isolated transactions are pushed to this class.
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
     * Persists new dependency tracking entity in its own transaction
     * to ensure flush to disk before async submit.
     *
     * Finds matching keys in existing entities and updates waitingOn property.
     * Boosts lower priority entities blocking this one.
     *
     * @param e dependency tracking entity to persist
     * @param waitForKey Additional key to wait for.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void persistDependencyEntity(DependencyTrackingEntity e, String waitForKey) {
        getSinkStatus(e.getSinkid()).processingStatus.ready.incrementAndGet();

        final Set<String> matchKeys = new HashSet<>(e.getMatchKeys());
        if (waitForKey != null) {
            matchKeys.add(waitForKey);
        }

        final Set<DependencyTrackingEntity.Key> chunksToWaitFor = findChunksToWaitFor(e.getSinkid(), matchKeys);
        e.setWaitingOn(chunksToWaitFor);
        entityManager.persist(e);

        boostPriorities(chunksToWaitFor, e.getPriority());
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



        final List<DependencyTrackingEntity.Key> chunksToWaitFor = findJobBarrier(e.getSinkid(), e.getKey().getJobId(), e.getMatchKeys());

        e.setWaitingOn(chunksToWaitFor);
        e.setStatus( ChunkSchedulingStatus.BLOCKED );

        if( chunksToWaitFor.isEmpty() ) {
            e.setStatus(ChunkSchedulingStatus.READY_FOR_DELIVERY);
        }
        getSinkStatus(e.getSinkid()).deliveringStatus.ready.incrementAndGet();
        entityManager.persist(e);
    }



    /**
     * Send JMS message to Processing, if queue size is lower then MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
     * @param chunk chunk to send to JMS queue
     * @param sinkId sink ID
     * @param priority message priority
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Asynchronous
    public void submitToProcessingIfPossibleAsync(ChunkEntity chunk, long sinkId, int priority) {
        submitToProcessingIfPossible(chunk, sinkId, priority);
    }


    /**
     * Send JMS message to Processing, if queue size is lower then MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
     * @param chunk chunk to send to JMS queue
     * @param sinkId sink ID
     * @param priority message priority
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Stopwatch
    public void submitToProcessingIfPossible(ChunkEntity chunk, long sinkId, int priority) {
        final JobSchedulerSinkStatus.QueueStatus queueStatus = getSinkStatus(sinkId).processingStatus;

        if (!queueStatus.isDirectSubmitMode()) return;

        if (queueStatus.enqueued.intValue() >= MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
            queueStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
            return;
        }
        submitToProcessing(chunk, queueStatus, priority);
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToProcessing(ChunkEntity chunk, JobSchedulerSinkStatus.QueueStatus queueStatus, int priority) {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getKey());
        final DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        if (dependencyTrackingEntity == null) {
            LOGGER.error("Internal Error unable to lookup chunk {} in submitToProcessing", key);
            return;
        }

        // recheck check if chunk is found by bulk and DIRECT mode
        if ( dependencyTrackingEntity.getStatus() != ChunkSchedulingStatus.READY_FOR_PROCESSING) {
            return;
        }

        dependencyTrackingEntity.setStatus(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING);
        try {
            JobEntity jobEntity = entityManager.find(JobEntity.class, chunk.getKey().getJobId());
            jobProcessorMessageProducerBean.send(getChunkFrom(chunk), jobEntity, priority);
            queueStatus.enqueued.incrementAndGet();
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
        if( dependencyTrackingEntity.getStatus() != ChunkSchedulingStatus.READY_FOR_DELIVERY) return;

        LOGGER.info("Trying to submit {} to Delivering", dependencyTrackingEntity.getKey());
        JobSchedulerSinkStatus.QueueStatus sinkStatus = getSinkStatus(dependencyTrackingEntity.getSinkid()).deliveringStatus;

        if (!sinkStatus.isDirectSubmitMode()) return;


        int queuedToDelivering = sinkStatus.enqueued.intValue();
        if (queuedToDelivering >= MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK) {
            sinkStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
            LOGGER.info("chunk {} blocked by queue size {} ", dependencyTrackingEntity.getKey(), queuedToDelivering);
            return;
        }

        submitToDelivering(chunk, dependencyTrackingEntity, sinkStatus);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToDeliveringNewTransaction(Chunk chunk, JobSchedulerSinkStatus.QueueStatus sinkStatus) {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk);
        final DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        submitToDelivering( chunk, dependencyTrackingEntity, sinkStatus);

    }

    private void submitToDelivering(Chunk chunk, DependencyTrackingEntity dependencyTrackingEntity, JobSchedulerSinkStatus.QueueStatus sinkStatus) {
        // recheck with chunk status with chunk Locked before sending to
        if( dependencyTrackingEntity.getStatus() != ChunkSchedulingStatus.READY_FOR_DELIVERY) {
            return;
        }

        final JobEntity jobEntity = jobStoreRepository.getJobEntityById((int) chunk.getJobId());
        // Chunk is ready for Sink
        try {
            sinkMessageProducerBean.send(chunk, jobEntity, dependencyTrackingEntity.getPriority());
            sinkStatus.enqueued.incrementAndGet();
            LOGGER.info("chunk {} submitted for delivery for sink {}",
                    dependencyTrackingEntity.getKey(), dependencyTrackingEntity.getSinkid());
            dependencyTrackingEntity.setStatus(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY);
        } catch (JobStoreException e) {
            LOGGER.error("Unable to send chunk {} to sink JMS queue - update to BULK mode for retransmit", e);
            sinkStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
        }
    }

    private void boostPriorities(Set<DependencyTrackingEntity.Key> keys, int priority) {
        if (priority > Priority.LOW.getValue()) {
            for (DependencyTrackingEntity.Key key : keys) {
                final DependencyTrackingEntity dependency = entityManager.find(DependencyTrackingEntity.class, key);
                if (dependency.getPriority() < priority) {
                    dependency.setPriority(priority);
                    boostPriorities(dependency.getWaitingOn(), priority);
                }
            }
        }
    }

    /**
     * Finds chunks matching given keys
     * @param sinkId sinkId
     * @param matchKeys set of match keys
     * @return Returns set of chunks to wait for.
     */
    @SuppressWarnings("unchecked")
    Set<DependencyTrackingEntity.Key> findChunksToWaitFor(int sinkId, Set<String> matchKeys) {
        if (matchKeys.isEmpty()) {
            return Collections.emptySet();
        }

        final Query query = entityManager.createNativeQuery(
                buildFindChunksToWaitForQuery(sinkId, matchKeys), DependencyTrackingEntity.KEY_RESULT);
        return new HashSet<>((List<DependencyTrackingEntity.Key>) query.getResultList());
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

        Query query = entityManager.createNamedQuery(DependencyTrackingEntity.RELATED_CHUNKS_QUERY);
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
     * @param sinkId sink Id
     * @param matchKeys set of match keys
     * @return native query string to find chunks with matching keys
     */
    String buildFindChunksToWaitForQuery(int sinkId, Set<String> matchKeys) {
        final StringBuilder builder = new StringBuilder(1000);
        builder.append("select * from dependencyTracking where sinkId=");
        builder.append(sinkId);
        builder.append(" and ( ");

        Boolean first = true;
        for (String key : matchKeys) {
            if (!first) builder.append(" or ");
            builder.append("matchKeys @> '[\"");
            builder.append(escapeSql(key));
            builder.append("\"]'");
            first = false;
        }
        builder.append(" )");
        builder.append(" ORDER BY jobId, chunkId for no key update");
        return builder.toString();
    }

    private String escapeSql(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("'", "''");
    }
}
