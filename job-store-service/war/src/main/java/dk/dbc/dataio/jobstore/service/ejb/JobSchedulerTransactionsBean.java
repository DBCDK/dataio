package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
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

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;

/**
 * Helper Bean for JobScheduler and JobSchedulerBulkSubmitterBean.
 * Methods needing to run in isolated transactions are pushed to this class.
 * <p>
 * The two {@code ...IfPossible} methods here are the direct dispatch paths, reached the moment a
 * chunk becomes ready. Both defer to the bulk sweep when the head of the sink's parked queue
 * outranks the chunk they hold, so that finding a free queue slot is not on its own enough to be
 * dispatched, see {@link DispatchOrder} for why the order has to hold on this path too.
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

    @EJB
    DeliveryDispatchRepository deliveryDispatchRepository;

    @Inject
    DependencyTrackingService dependencyTrackingService;

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
     * Attempt direct dispatch to processing, off the partitioning thread.
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
     * Send JMS message to Processing, if the sink's processor queue has room and nothing parked
     * ahead of this chunk outranks it.
     *
     * @param chunk    chunk to send to JMS queue
     * @param sinkId   sink ID
     * @param priority message priority
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Stopwatch
    public void submitToProcessingIfPossible(ChunkEntity chunk, int sinkId, int priority) {
        TrackingKey key = chunk.getKey().toTrackingKey();
        if (dependencyTrackingService.capacity(sinkId, QUEUED_FOR_PROCESSING) <= 0) {
            dependencyTrackingService.setStatus(key, SCHEDULED_FOR_PROCESSING);
            return;
        }
        if (isOutrankedForProcessing(sinkId, key, priority)) {
            dependencyTrackingService.setStatus(key, SCHEDULED_FOR_PROCESSING);
            return;
        }
        submitToProcessing(chunk, priority);
    }

    /**
     * Whether a chunk about to be dispatched directly should stand down for the head of the sink's
     * parked queue.
     * <p>
     * A free queue slot says the sink has room, not that this chunk is the one entitled to it. The
     * bulk sweep fills slots in {@code (priority DESC, jobId ASC, chunkId ASC)} order, and without
     * this the direct path would take a freed slot on sight, letting a job that happens to be
     * partitioning right now stream past a backlog that outranks it for as long as the burst lasts.
     * <p>
     * One {@code LIMIT 1} read of the same ordered query the sweep uses, which the ordering index
     * answers from its first entry. Only reached when the chunk would otherwise be sent, so an idle
     * sink pays it once and dispatches, and direct mode keeps the latency it exists for.
     *
     * @param sinkId   sink the chunk belongs to
     * @param key      chunk about to be dispatched
     * @param priority the chunk's dispatch priority
     * @return true if the chunk should be parked for the sweep instead
     */
    private boolean isOutrankedForProcessing(int sinkId, TrackingKey key, int priority) {
        List<DependencyTrackingRepository.ProcessingCandidate> parked =
                dependencyTrackingService.findProcessingCandidates(sinkId, 1);
        if (parked.isEmpty()) {
            return false;
        }
        DependencyTrackingRepository.ProcessingCandidate head = parked.get(0);
        if (DispatchOrder.outranks(priority, key, head.priority(), head.key())) {
            return false;
        }
        LOGGER.info("submitToProcessingIfPossible: chunk {} deferring to parked chunk {}",
                key.toChunkIdentifier(), head.key().toChunkIdentifier());
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    public void submitToProcessing(ChunkEntity chunk, int priority) {
        TrackingKey key = new TrackingKey(chunk.getKey().getJobId(), chunk.getKey().getId());
        if(dependencyTrackingService.setValidatedStatus(key, QUEUED_FOR_PROCESSING).isEmpty()) {
            // WARN rather than ERROR: nothing is stranded. The chunk is already queued, already
            // processed, or deliberately gone, and an abort or a re-entry into scheduleChunk makes
            // that a normal outcome. WARN rather than INFO because this path selected the chunk and
            // then found the table changed under it, unlike a callback telling us what we knew.
            LOGGER.warn("submitToProcessing: chunk {} was not awaiting processing, not sending",
                    key.toChunkIdentifier());
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
        if (!dependencyTracking.isGateOpen()) {
            dependencyTrackingService.setStatus(trackingKey, SCHEDULED_FOR_DELIVERY);
            LOGGER.info("submitToDeliveringIfPossible: chunk {}/{} held back by a closed gate", trackingKey.getJobId(), trackingKey.getChunkId());
            return;
        }

        // Last of the three guards, so the read is only paid on the path that would otherwise send.
        if (isOutrankedForDelivery(dependencyTracking.getSinkId(), trackingKey, dependencyTracking.getPriority())) {
            dependencyTrackingService.setStatus(trackingKey, SCHEDULED_FOR_DELIVERY);
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
     * Whether a chunk about to be delivered directly should stand down for the head of the sink's
     * parked queue.
     * <p>
     * The delivery counterpart of {@link #isOutrankedForProcessing}, and the more consequential of
     * the two. Delivery order is what puts a live MARC head at a sink ahead of the volumes
     * referencing it, since the broker serialises a hierarchy into one group and delivers it in
     * send order, see {@link DispatchOrder}.
     * <p>
     * The candidate query filters on {@code gate_open}, so a gated chunk is not a candidate and
     * holds nothing back. That matters for a full-width barrier, where a whole job's data chunks
     * sit parked with closed gates and must not stall the sink's other traffic.
     * <p>
     * A chunk cannot normally meet itself here, since it is compared against
     * {@code SCHEDULED_FOR_DELIVERY} rows while holding {@code READY_FOR_DELIVERY}. A caller that
     * hands over an already parked chunk finds it at the head of its own order and stands down,
     * which leaves the chunk exactly where it already was for the sweep to take. No chunk is
     * stranded that way, so the case is left to fall out rather than be tested for.
     *
     * @param sinkId   sink the chunk belongs to
     * @param key      chunk about to be dispatched
     * @param priority the chunk's dispatch priority
     * @return true if the chunk should be parked for the sweep instead
     */
    private boolean isOutrankedForDelivery(int sinkId, TrackingKey key, int priority) {
        List<DeliveryDispatchRepository.DeliveryCandidate> parked =
                deliveryDispatchRepository.findDeliveryCandidates(sinkId, 1);
        if (parked.isEmpty()) {
            return false;
        }
        DeliveryDispatchRepository.DeliveryCandidate head = parked.get(0);
        if (DispatchOrder.outranks(priority, key, head.priority(), head.key())) {
            return false;
        }
        LOGGER.info("submitToDeliveringIfPossible: chunk {} deferring to parked chunk {}",
                key.toChunkIdentifier(), head.key().toChunkIdentifier());
        return true;
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
        // gate leaves the scheduler" true by construction rather than by enumerating callers. Read
        // off the row fetched just above, in this transaction, so it is as current as the status
        // read from the same row. The bulk path has already filtered in SQL, so this is a near
        // certain pass for it.
        if (!dependencyTracking.isGateOpen()) {
            LOGGER.info("submitToDelivering: chunk {}/{} held back by a closed gate", trackingKey.getJobId(), trackingKey.getChunkId());
            dependencyTrackingService.setStatus(trackingKey, SCHEDULED_FOR_DELIVERY);
            return;
        }

        JobEntity jobEntity = jobStoreRepository.getJobEntityById(trackingKey.getJobId());
        if(jobEntity.getState().isAborted() || JobsBean.isAborted(jobEntity.getId())) return;
        // chunk is ready for sink
        try {
            // Validated, so the claim on the chunk and the check that it is still claimable are one
            // statement. The guard at the top of this method reads the same predecessor set, and a
            // read followed by a write is a check-then-act two dispatch paths can both pass, which
            // sends the chunk's items to the sink twice.
            if (dependencyTrackingService.setValidatedStatus(trackingKey, QUEUED_FOR_DELIVERY).isEmpty()) {
                LOGGER.warn("submitToDelivering: chunk {}/{} was claimed by another dispatch, not sending",
                        trackingKey.getJobId(), trackingKey.getChunkId());
                return;
            }
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
