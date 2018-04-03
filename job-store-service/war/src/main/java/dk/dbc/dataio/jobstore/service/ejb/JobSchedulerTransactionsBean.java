package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jpa.converter.PgIntArray;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.lang.Hashcode;
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
     * @param entity dependency tracking entity to persist
     * @param barrierMatchKey Additional barrier key to wait for.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void persistDependencyEntity(DependencyTrackingEntity entity, String barrierMatchKey) {
        getSinkStatus(entity.getSinkid()).processingStatus.ready.incrementAndGet();

        final Set<DependencyTrackingEntity.Key> chunksToWaitFor = findChunksToWaitFor(entity, barrierMatchKey);
        entity.setWaitingOn(chunksToWaitFor);
        entityManager.persist(entity);

        boostPriorities(chunksToWaitFor, entity.getPriority());
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

        // recheck if chunk is found by BULK and DIRECT mode
        if ( dependencyTrackingEntity.getStatus() != ChunkSchedulingStatus.READY_FOR_PROCESSING) {
            return;
        }

        dependencyTrackingEntity.setStatus(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING);
        try {
            JobEntity jobEntity = entityManager.find(JobEntity.class, chunk.getKey().getJobId());
            jobProcessorMessageProducerBean.send(getChunkFrom(chunk), jobEntity, priority);
            queueStatus.enqueued.incrementAndGet();
            LOGGER.info("submitToProcessing: chunk {}/{} scheduled for processing", key.getJobId(), key.getChunkId());
        } catch (JobStoreException e) {
            LOGGER.error("submitToProcessing: unable to send chunk {}/{} to JMS queue",
                    key.getJobId(), key.getChunkId(), e);
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

        JobSchedulerSinkStatus.QueueStatus sinkStatus = getSinkStatus(dependencyTrackingEntity.getSinkid()).deliveringStatus;

        if (!sinkStatus.isDirectSubmitMode()) return;

        int queuedToDelivering = sinkStatus.enqueued.intValue();
        if (queuedToDelivering >= MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK) {
            sinkStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
            LOGGER.info("submitToDeliveringIfPossible: chunk {}/{} blocked by queue size {}",
                    chunk.getJobId(), chunk.getChunkId(), queuedToDelivering);
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
        // recheck with chunk status with chunk locked before sending
        if( dependencyTrackingEntity.getStatus() != ChunkSchedulingStatus.READY_FOR_DELIVERY) {
            return;
        }

        final JobEntity jobEntity = jobStoreRepository.getJobEntityById((int) chunk.getJobId());
        // chunk is ready for sink
        try {
            sinkMessageProducerBean.send(chunk, jobEntity, dependencyTrackingEntity.getPriority());
            sinkStatus.enqueued.incrementAndGet();
            LOGGER.info("submitToDelivering: chunk {}/{} scheduled for delivery for sink {}",
                    chunk.getJobId(), chunk.getChunkId(), dependencyTrackingEntity.getSinkid());
            dependencyTrackingEntity.setStatus(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY);
        } catch (JobStoreException e) {
            LOGGER.error("submitToDelivering: unable to send chunk {}/{} to JMS queue - update to BULK mode for retransmit",
                    chunk.getJobId(), chunk.getChunkId(), e);
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
     * Finds chunks matching keys in given dependency tracking entity
     * and given barrier match key
     * @param entity dependency tracking entity
     * @param barrierMatchKey special key for barrier chunks
     * @return Returns set of chunks to wait for.
     */
    @SuppressWarnings("unchecked")
    Set<DependencyTrackingEntity.Key> findChunksToWaitFor(DependencyTrackingEntity entity, String barrierMatchKey) {
        if (entity.getMatchKeys().isEmpty() && barrierMatchKey == null) {
            return Collections.emptySet();
        }

        final Query query = entityManager.createNamedQuery(DependencyTrackingEntity.CHUNKS_TO_WAIT_FOR_QUERY);
        query.setParameter(1, entity.getSinkid());
        query.setParameter(2, entity.getSubmitterNumber());
        if (barrierMatchKey != null) {
            query.setParameter(3, PgIntArray.toPgString(entity.getHashes(), Hashcode.of(barrierMatchKey)));
        } else {
            query.setParameter(3, PgIntArray.toPgString(entity.getHashes()));
        }
        return new HashSet<>((List<DependencyTrackingEntity.Key>) query.getResultList());
    }

    /**
     * Finding lists of chunks in dependency Tracking with this jobId and MatchKey.
     * Note only First key in waitForKey is checked.
     *
     * @param sinkId sinkId
     * @param jobId jobId for witch chunks to wait for barrier
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

    public DependencyTrackingEntity merge(DependencyTrackingEntity dependencyTrackingEntity) {
        return entityManager.merge(dependencyTrackingEntity);
    }
}
