package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
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

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;

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

    @EJB
    DeliveryDispatchRepository deliveryDispatchRepository;

    public JobSchedulerTransactionsBean() {
    }

    public JobSchedulerTransactionsBean(EntityManager entityManager, PgJobStoreRepository jobStoreRepository, SinkMessageProducerBean sinkMessageProducerBean, JobProcessorMessageProducerBean jobProcessorMessageProducerBean, DependencyTrackingService dependencyTrackingService, DeliveryDispatchRepository deliveryDispatchRepository) {
        this.entityManager = entityManager;
        this.jobStoreRepository = jobStoreRepository;
        this.sinkMessageProducerBean = sinkMessageProducerBean;
        this.jobProcessorMessageProducerBean = jobProcessorMessageProducerBean;
        this.dependencyTrackingService = dependencyTrackingService;
        this.deliveryDispatchRepository = deliveryDispatchRepository;
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
        if (dependencyTrackingService.capacity(sinkId, QUEUED_FOR_PROCESSING) <= 0) {
            dependencyTrackingService.setStatus(chunk.getKey().toTrackingKey(), SCHEDULED_FOR_PROCESSING);
            return;
        }
        submitToProcessing(chunk, priority);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToProcessing(ChunkEntity chunk, int priority) {
        TrackingKey key = new TrackingKey(chunk.getKey().getJobId(), chunk.getKey().getId());
        StatusChangeEvent changeEvent = dependencyTrackingService.setValidatedStatus(key, QUEUED_FOR_PROCESSING);
        if(changeEvent == null) {
            LOGGER.error("Tracker state could not be set to QUEUED_FOR_PROCESSING: {}", key);
            return;
        }
        try {
            JobEntity jobEntity = entityManager.find(JobEntity.class, chunk.getKey().getJobId());
            jobProcessorMessageProducerBean.send(getChunkFrom(chunk), jobEntity, priority);
            LOGGER.info("submitToProcessing: chunk {}/{} queued for processing", key.getJobId(), key.getChunkId());
        } catch (JobStoreException e) {
            LOGGER.error("submitToProcessing: unable to send chunk {}/{} to JMS queue", key.getJobId(), key.getChunkId(), e);
        }

    }

    /**
     * Send one JMS message per item of the chunk to the sink.
     *
     * @param trackingKey Tracking Key for chunk
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Stopwatch
    public void submitToDeliveringIfPossible(TrackingKey trackingKey) {
        DependencyTrackingRO dependencyTracking = dependencyTrackingService.get(trackingKey);
        if (dependencyTracking == null || dependencyTracking.getStatus().isInvalidStatusChange(QUEUED_FOR_DELIVERY)) return;

        int capacity = dependencyTrackingService.capacity(dependencyTracking.getSinkId(), QUEUED_FOR_DELIVERY);
        if (capacity <= 0) {
            dependencyTrackingService.setStatus(trackingKey, SCHEDULED_FOR_DELIVERY);
            LOGGER.info("submitToDeliveringIfPossible: chunk {}/{} blocked by queue capacity {}", trackingKey.getJobId(), trackingKey.getChunkId(), capacity);
            return;
        }

        // Park rather than return, exactly as the capacity branch above does. The bulk submitter
        // only looks at SCHEDULED_FOR_DELIVERY, so a chunk left in READY_FOR_DELIVERY would not be
        // reconsidered when its gate opens until the five minute stale sweep noticed it.
        if (deliveryDispatchRepository.hasClosedGate(trackingKey)) {
            dependencyTrackingService.setStatus(trackingKey, SCHEDULED_FOR_DELIVERY);
            LOGGER.info("submitToDeliveringIfPossible: chunk {}/{} held back by a closed gate", trackingKey.getJobId(), trackingKey.getChunkId());
            return;
        }

        List<ItemEntity> items = getProcessedItemsFrom(trackingKey);
        if (items.isEmpty()) {
            LOGGER.error("submitToDeliveringIfPossible: chunk {}/{} has no items to deliver", trackingKey.getJobId(), trackingKey.getChunkId());
            return;
        }
        submitToDelivering(items, trackingKey);
    }

    /**
     * Send one JMS message per item of the chunk to the sink, in its own transaction.
     *
     * @param trackingKey Tracking Key for chunk
     * @return false if the chunk holds no items at all, in which case nothing was sent and
     * the caller is expected to drop the chunk from dependency tracking
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public boolean submitToDeliveringNewTransaction(TrackingKey trackingKey) {
        List<ItemEntity> items = getProcessedItemsFrom(trackingKey);
        if (items.isEmpty()) {
            return false;
        }
        submitToDelivering(items, trackingKey);
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void attemptToUnblockChunk(TrackingKey chunkBlockedKey) {
        submitToDeliveringIfPossible(chunkBlockedKey);
    }

    private void submitToDelivering(List<ItemEntity> items, TrackingKey trackingKey) {
        // recheck with chunk status with chunk locked before sending
        DependencyTrackingRO dependencyTracking = dependencyTrackingService.get(trackingKey);
        if (dependencyTracking == null) {
            LOGGER.info("submitToDelivering: chunk {}/{} is no longer tracked, nothing to send", trackingKey.getJobId(), trackingKey.getChunkId());
            return;
        }
        if (dependencyTracking.getStatus().isInvalidStatusChange(QUEUED_FOR_DELIVERY)) return;

        // The choke point every dispatch path reaches, which is what makes "no chunk with a closed
        // gate leaves the scheduler" true by construction rather than by enumerating callers. The
        // bulk path has already filtered in SQL, so this is a near certain pass for it.
        if (deliveryDispatchRepository.hasClosedGate(trackingKey)) {
            LOGGER.info("submitToDelivering: chunk {}/{} held back by a closed gate", trackingKey.getJobId(), trackingKey.getChunkId());
            dependencyTrackingService.setStatus(trackingKey, SCHEDULED_FOR_DELIVERY);
            return;
        }

        JobEntity jobEntity = jobStoreRepository.getJobEntityById(trackingKey.getJobId());
        if(jobEntity.getState().isAborted() || JobsBean.isAborted(jobEntity.getId())) return;
        // chunk is ready for sink
        try {
            dependencyTrackingService.setStatus(trackingKey, QUEUED_FOR_DELIVERY);
            sinkMessageProducerBean.send(items, jobEntity, dependencyTracking.getPriority());
            LOGGER.info("submitToDelivering: chunk {}/{} scheduled for delivery for sink {}",
                    trackingKey.getJobId(), trackingKey.getChunkId(), dependencyTracking.getSinkId());
        } catch (JobStoreException e) {
            // Log before the status update. JobStoreException is @ApplicationException
            // (rollback = true), so the transaction is already marked rollback-only here
            // and setStatus fails with "Client's transaction aborted", which would
            // otherwise replace this exception and leave no trace of the real cause.
            LOGGER.error("submitToDelivering: unable to send chunk {}/{} to JMS queue - chunk has been scheduled for delivery",
                    trackingKey.getJobId(), trackingKey.getChunkId(), e);
            dependencyTrackingService.setStatus(trackingKey, SCHEDULED_FOR_DELIVERY);
        }
    }


    public Chunk getChunkFrom(ChunkEntity chunk) {
        try {
            ChunkEntity.Key chunkKey = chunk.getKey();
            return jobStoreRepository.getChunk(Chunk.Type.PARTITIONED, chunkKey.getJobId(), chunkKey.getId());
        } catch (RuntimeException ex) {
            LOGGER.warn("Internal error Unable to get PARTITIONED items for {}", chunk.getKey());
            throw ex;
        }
    }

    /**
     * @param dtKey Tracking Key for chunk
     * @return the chunk's item entities in ascending item ID order, which is the order the
     * sink message producer must send them in
     */
    public List<ItemEntity> getProcessedItemsFrom(TrackingKey dtKey) {
        try {
            return jobStoreRepository.getChunkItemEntities(dtKey.getJobId(), dtKey.getChunkId());
        } catch (RuntimeException ex) {
            LOGGER.warn("Internal error Unable to get PROCESSED items for {}", dtKey, ex);
            dependencyTrackingService.setStatus(dtKey, SCHEDULED_FOR_DELIVERY);
            throw ex;
        }
    }
}
