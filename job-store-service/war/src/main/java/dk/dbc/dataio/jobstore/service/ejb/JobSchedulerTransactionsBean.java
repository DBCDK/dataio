package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.distributed.QueueSubmitMode;
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

import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK;

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
        dependencyTrackingService.getSinkStatus(entity.getSinkId()).getProcessingStatus().ready.incrementAndGet();

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
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void addDependencies(DependencyTracking e) {
        List<TrackingKey> chunksToWaitFor = dependencyTrackingService.findJobBarrier(e.getSinkId(), e.getKey().getJobId(), e.getMatchKeys());
        e.setWaitingOn(chunksToWaitFor);
        e.setStatus(ChunkSchedulingStatus.BLOCKED);

        if (chunksToWaitFor.isEmpty()) {
            e.setStatus(ChunkSchedulingStatus.READY_FOR_DELIVERY);
        }
    }


    /**
     * Send JMS message to Processing, if queue size is lower then MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
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
     * Send JMS message to Processing, if queue size is lower then MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK
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

        if (queueStatus.enqueued.intValue() >= MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
            queueStatus.setMode(QueueSubmitMode.BULK);
            return;
        }
        submitToProcessing(chunk, queueStatus, priority);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToProcessing(ChunkEntity chunk, JobSchedulerSinkStatus.QueueStatus queueStatus, int priority) {
        TrackingKey key = new TrackingKey(chunk.getKey().getJobId(), chunk.getKey().getId());
        dependencyTrackingService.modify(key, dependencyTracking -> {
            if (dependencyTracking == null) {
                LOGGER.error("Internal Error unable to lookup chunk {} in submitToProcessing", key);
                return;
            }

            // recheck if chunk is found by BULK and DIRECT mode
            if (dependencyTracking.getStatus() != ChunkSchedulingStatus.READY_FOR_PROCESSING) {
                return;
            }

            dependencyTracking.setStatus(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING);
            try {
                JobEntity jobEntity = entityManager.find(JobEntity.class, chunk.getKey().getJobId());
                jobProcessorMessageProducerBean.send(getChunkFrom(chunk), jobEntity, priority);
                queueStatus.enqueued.incrementAndGet();
                LOGGER.info("submitToProcessing: chunk {}/{} scheduled for processing", key.getJobId(), key.getChunkId());
            } catch (JobStoreException e) {
                LOGGER.error("submitToProcessing: unable to send chunk {}/{} to JMS queue", key.getJobId(), key.getChunkId(), e);
            }
        });
    }

    /**
     * Send JMS message to Sink with chunk.
     *
     * @param chunk                    The chunk to submit to delivering queue
     * @param dependencyTracking Tracking Entity for chunk
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Stopwatch
    public void submitToDeliveringIfPossible(Chunk chunk, DependencyTracking dependencyTracking) {
        if (dependencyTracking.getStatus() != ChunkSchedulingStatus.READY_FOR_DELIVERY) return;

        JobSchedulerSinkStatus.QueueStatus sinkStatus = dependencyTrackingService.getSinkStatus(dependencyTracking.getSinkId()).getDeliveringStatus();

        if (!sinkStatus.isDirectSubmitMode()) return;

        int queuedToDelivering = sinkStatus.enqueued.intValue();
        if (queuedToDelivering >= MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK) {
            sinkStatus.setMode(QueueSubmitMode.BULK);
            LOGGER.info("submitToDeliveringIfPossible: chunk {}/{} blocked by queue size {}",
                    chunk.getJobId(), chunk.getChunkId(), queuedToDelivering);
            return;
        }

        submitToDelivering(chunk, dependencyTracking, sinkStatus);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToDeliveringNewTransaction(Chunk chunk, JobSchedulerSinkStatus.QueueStatus sinkStatus) {
        TrackingKey key = new TrackingKey(chunk.getJobId(), (int)chunk.getChunkId());
        dependencyTrackingService.modify(key, dt -> submitToDelivering(chunk, dt, sinkStatus));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void attemptToUnblockChunk(TrackingKey chunkBlockedKey, TrackingKey chunkDoneKey,
                                      JobSchedulerSinkStatus.QueueStatus sinkQueueStatus) {

        dependencyTrackingService.modify(chunkBlockedKey, blockedChunk -> {
            blockedChunk.getWaitingOn().remove(chunkDoneKey);

            if (blockedChunk.getWaitingOn().isEmpty() && blockedChunk.getStatus() == ChunkSchedulingStatus.BLOCKED) {
                blockedChunk.setStatus(ChunkSchedulingStatus.READY_FOR_DELIVERY);
                sinkQueueStatus.ready.incrementAndGet();
                if (sinkQueueStatus.isDirectSubmitMode()) {
                    submitToDeliveringIfPossible(getProcessedChunkFrom(blockedChunk.getKey()), blockedChunk);
                }
            }
        });

    }

    private void submitToDelivering(Chunk chunk, DependencyTracking dependencyTracking, JobSchedulerSinkStatus.QueueStatus sinkStatus) {
        // recheck with chunk status with chunk locked before sending
        if (dependencyTracking.getStatus() != ChunkSchedulingStatus.READY_FOR_DELIVERY) {
            return;
        }

        final JobEntity jobEntity = jobStoreRepository.getJobEntityById(chunk.getJobId());
        if(jobEntity.getState().isAborted() || JobsBean.isAborted(jobEntity.getId())) return;
        // chunk is ready for sink
        try {
            sinkMessageProducerBean.send(chunk, jobEntity, dependencyTracking.getPriority());
            sinkStatus.enqueued.incrementAndGet();
            LOGGER.info("submitToDelivering: chunk {}/{} scheduled for delivery for sink {}",
                    chunk.getJobId(), chunk.getChunkId(), dependencyTracking.getSinkId());
            dependencyTracking.setStatus(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY);
        } catch (JobStoreException e) {
            LOGGER.error("submitToDelivering: unable to send chunk {}/{} to JMS queue - update to BULK mode for retransmit",
                    chunk.getJobId(), chunk.getChunkId(), e);
            sinkStatus.setMode(QueueSubmitMode.BULK);
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
