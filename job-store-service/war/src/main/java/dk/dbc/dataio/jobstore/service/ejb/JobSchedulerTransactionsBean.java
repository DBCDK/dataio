package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.distributed.QueueSubmitMode;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;

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

    @Inject
    DependencyTrackingService dependencyTrackingService;

    /**
     * Persists new dependency tracking entity in its own transaction
     * to ensure flush to disk before async submit.
     * <p>
     * Finds matching keys in existing entities and updates waitingOn property.
     * Boosts lower priority entities blocking this one.
     *
     * @param entity          dependency tracking entity to persist
     * @param barrierMatchKey Additional barrier key to wait for.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void persistDependencyEntity(DependencyTracking entity, String barrierMatchKey) {
        Set<TrackingKey> chunksToWaitFor = dependencyTrackingService.findChunksToWaitFor(entity, barrierMatchKey);
        entity.setWaitingOn(chunksToWaitFor);
        dependencyTrackingService.add(entity);
        dependencyTrackingService.boostPriorities(chunksToWaitFor, entity.getPriority());
    }

    /**
     * Force new Chunk to Store before Async SubmitIfPossibleForProcessing.
     * New Transaction to ensure Record is on Disk before async submit
     * <p>
     * Updates WaitingOn with chunks with matching keys
     *
     * @param e Dependency tracking Entity
     */
    @Stopwatch
    public void addDependencies(DependencyTrackingRO e) {
        Set<TrackingKey> chunksToWaitFor = dependencyTrackingService.findJobBarrier(e.getSinkId(), e.getKey().getJobId(), e.getMatchKeys());
        dependencyTrackingService.addToChunksToWaitFor(e.getKey(), chunksToWaitFor);
    }


    /**
     * Send JMS message to Processing, if queue size is lower than MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
     *
     * @param chunk    chunk to send to JMS queue
     * @param sinkId   sink ID
     * @param priority message priority
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Asynchronous
    public void submitToProcessingIfPossibleAsync(ChunkEntity chunk, int sinkId, int priority) {
        if(JobsBean.isAborted(chunk.getKey().getJobId())) return;
        submitToProcessingIfPossible(chunk, sinkId, priority);
    }


    /**
     * Send JMS message to Processing, if queue size is lower than MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
     *
     * @param chunk    chunk to send to JMS queue
     * @param sinkId   sink ID
     * @param priority message priority
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Stopwatch
    public void submitToProcessingIfPossible(ChunkEntity chunk, int sinkId, int priority) {
        JobSchedulerSinkStatus.QueueStatus queueStatus = dependencyTrackingService.getSinkStatus(sinkId).getProcessingStatus();

        if (!queueStatus.isDirectSubmitMode()) return;

        if (dependencyTrackingService.capacity(sinkId, QUEUED_FOR_PROCESSING) <= 0) {
            queueStatus.setMode(QueueSubmitMode.BULK);
            return;
        }
        submitToProcessing(chunk, priority);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToProcessing(ChunkEntity chunk, int priority) {
        TrackingKey key = new TrackingKey(chunk.getKey().getJobId(), chunk.getKey().getId());
        StatusChangeEvent changeEvent = dependencyTrackingService.setStatus(key, READY_FOR_PROCESSING, QUEUED_FOR_PROCESSING);
        if(changeEvent == null) {
            LOGGER.error("Tracker state could not be set to QUEUED_FOR_PROCESSING current status is {}", key);
            return;
        }
        try {
            JobEntity jobEntity = entityManager.find(JobEntity.class, chunk.getKey().getJobId());
            jobProcessorMessageProducerBean.send(getChunkFrom(chunk), jobEntity, priority);
            LOGGER.info("submitToProcessing: chunk {}/{} scheduled for processing", key.getJobId(), key.getChunkId());
        } catch (JobStoreException e) {
            LOGGER.error("submitToProcessing: unable to send chunk {}/{} to JMS queue", key.getJobId(), key.getChunkId(), e);
        }

    }

    /**
     * Send JMS message to Sink with chunk.
     *
     * @param chunk       The chunk to submit to delivering queue
     * @param trackingKey Tracking Key for chunk
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Stopwatch
    public void submitToDeliveringIfPossible(Chunk chunk, TrackingKey trackingKey) {
        DependencyTrackingRO dependencyTracking = dependencyTrackingService.get(trackingKey);
        if (dependencyTracking == null || dependencyTracking.getStatus() != ChunkSchedulingStatus.READY_FOR_DELIVERY) return;

        JobSchedulerSinkStatus.QueueStatus sinkStatus = dependencyTrackingService.getSinkStatus(dependencyTracking.getSinkId()).getDeliveringStatus();

        if (!sinkStatus.isDirectSubmitMode()) return;


        int capacity = dependencyTrackingService.capacity(dependencyTracking.getSinkId(), ChunkSchedulingStatus.QUEUED_FOR_DELIVERY);
        if (capacity <= 0) {
            sinkStatus.setMode(QueueSubmitMode.BULK);
            LOGGER.info("submitToDeliveringIfPossible: chunk {}/{} blocked by queue capacity {}", chunk.getJobId(), chunk.getChunkId(), capacity);
            return;
        }

        submitToDelivering(chunk, trackingKey, sinkStatus);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToDeliveringNewTransaction(Chunk chunk, JobSchedulerSinkStatus.QueueStatus sinkStatus) {
        TrackingKey key = new TrackingKey(chunk.getJobId(), (int)chunk.getChunkId());
        submitToDelivering(chunk, key, sinkStatus);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void attemptToUnblockChunk(TrackingKey chunkBlockedKey, JobSchedulerSinkStatus.QueueStatus sinkQueueStatus) {
        if (sinkQueueStatus.isDirectSubmitMode()) {
            submitToDeliveringIfPossible(getProcessedChunkFrom(chunkBlockedKey), chunkBlockedKey);
        }
    }

    private void submitToDelivering(Chunk chunk, TrackingKey trackingKey, JobSchedulerSinkStatus.QueueStatus sinkStatus) {
        // recheck with chunk status with chunk locked before sending
        DependencyTrackingRO dependencyTracking = dependencyTrackingService.get(trackingKey);
        if (dependencyTracking.getStatus() != ChunkSchedulingStatus.READY_FOR_DELIVERY) {
            return;
        }

        JobEntity jobEntity = jobStoreRepository.getJobEntityById(chunk.getJobId());
        if(jobEntity.getState().isAborted() || JobsBean.isAborted(jobEntity.getId())) return;
        // chunk is ready for sink
        try {
            dependencyTrackingService.setStatus(trackingKey, ChunkSchedulingStatus.QUEUED_FOR_DELIVERY);
            sinkMessageProducerBean.send(chunk, jobEntity, dependencyTracking.getPriority());
            LOGGER.info("submitToDelivering: chunk {}/{} scheduled for delivery for sink {}",
                    chunk.getJobId(), chunk.getChunkId(), dependencyTracking.getSinkId());
        } catch (JobStoreException e) {
            LOGGER.error("submitToDelivering: unable to send chunk {}/{} to JMS queue - update to BULK mode for retransmit",
                    chunk.getJobId(), chunk.getChunkId(), e);
            sinkStatus.setMode(QueueSubmitMode.BULK);
            dependencyTrackingService.setStatus(dependencyTracking.getKey(), ChunkSchedulingStatus.READY_FOR_DELIVERY);
        }
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

    public Chunk getProcessedChunkFrom(TrackingKey dtKey) {
        try {
            ChunkEntity.Key chunkKey = new ChunkEntity.Key(dtKey.getChunkId(), dtKey.getJobId());

            return jobStoreRepository.getChunk(Chunk.Type.PROCESSED, chunkKey.getJobId(), chunkKey.getId());
        } catch (RuntimeException ex) {
            LOGGER.error("Internal error Unable to get PROCESSED items for {}", dtKey, ex);
            throw ex;
        }
    }
}
