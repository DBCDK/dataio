package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;

/**
 * Handles chunk scheduling as chunks pass through partitioning, processing and delivery phases.
 * <p>
 * Enqueueing is rate limited per sink, by a cap on how many of that sink's chunks may sit in the
 * queued states at once. {@code QUEUED_FOR_PROCESSING} and {@code QUEUED_FOR_DELIVERY} each carry
 * that cap on {@link dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus}, and
 * {@code DependencyTrackingService.capacity} answers what is left of it from the sink's chunk
 * counts.
 * <p>
 * There are two ways a chunk reaches a JMS queue, and which one it takes is decided per chunk:
 * <ul>
 * <li>directly, when {@code JobSchedulerTransactionsBean.submitToProcessingIfPossible} or
 *     {@code submitToDeliveringIfPossible} finds capacity at the moment the chunk becomes ready,</li>
 * <li>otherwise the chunk is left in {@code SCHEDULED_FOR_PROCESSING} or
 *     {@code SCHEDULED_FOR_DELIVERY}, and {@link JobSchedulerBulkSubmitterBean}'s timers pick it up
 *     once the sink has room. Those timers call {@link #bulkScheduleToProcessingForSink} and
 *     {@link #bulkScheduleToDeliveringForSink} here.</li>
 * </ul>
 * A chunk that finds a full queue simply waits where it is for the next sweep.
 * <p>
 * The counts behind the cap are held in a distributed map, so the cap applies across job-store
 * instances rather than per JVM. The timer-driven work, {@link #updateSinks} here and both sweeps
 * in {@link JobSchedulerBulkSubmitterBean}, returns early on every instance but one, each guarded
 * by {@code Hazelcast.isSlave}.
 */
@Stateless
@SuppressWarnings("PMD.TooManyStaticImports")
public class JobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBean.class);
    private static final Tag PROC_TAG = new Tag("state", "processing");
    private static final Tag DEL_TAG = new Tag("state", "delivering");

    private static final Set<SinkContent.SinkType> REQUIRES_TERMINATION_CHUNK = new HashSet<>(Set.of(SinkContent.SinkType.MARCCONV, SinkContent.SinkType.PERIODIC_JOBS, SinkContent.SinkType.TICKLE));

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    protected JobSchedulerTransactionsBean jobSchedulerTransactionsBean;

    @EJB
    protected PgJobStoreRepository pgJobStoreRepository;

    @EJB
    protected JobGateBean jobGateBean;

    @EJB
    protected DeliveryDispatchRepository deliveryDispatchRepository;

    @Inject
    MetricRegistry metricRegistry;
    @EJB
    FlowStoreServiceConnectorBean flowStore;
    @Inject
    DependencyTrackingService dependencyTrackingService;

    private static final Map<String, Integer> blockedCounts = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> maxDeliveryDurations = new ConcurrentHashMap<>();

    public JobSchedulerBean() {
    }

    public JobSchedulerBean(EntityManager entityManager, JobSchedulerTransactionsBean jobSchedulerTransactionsBean, PgJobStoreRepository pgJobStoreRepository, FlowStoreServiceConnectorBean flowStore, DependencyTrackingService dependencyTrackingService, JobGateBean jobGateBean, DeliveryDispatchRepository deliveryDispatchRepository) {
        this.entityManager = entityManager;
        this.jobSchedulerTransactionsBean = jobSchedulerTransactionsBean;
        this.pgJobStoreRepository = pgJobStoreRepository;
        this.flowStore = flowStore;
        this.dependencyTrackingService = dependencyTrackingService;
        this.jobGateBean = jobGateBean;
        this.deliveryDispatchRepository = deliveryDispatchRepository;
    }

    public void registerMetrics() {
        try {
            for (Sink sink : flowStore.getConnector().findAllSinks()) {
                Tag sinkTag = new Tag("sink_name", sink.getContent().getName());
                MetricID metricID = new MetricID("dataio_longest_running_delivery_in_ms", sinkTag);
                Gauge<?> gauge = metricRegistry.getGauge(metricID);
                if (gauge == null) metricRegistry.gauge(metricID, () -> getLongestRunningChunkDuration(sink.getId()));
                LOGGER.info("Registered gauge for longest_running_delivery_in_ms -> {}", metricID);
                metricRegistry.gauge("dataio_status_map", () -> dependencyTrackingService.getCount(sink.getId(), QUEUED_FOR_PROCESSING), sinkTag, PROC_TAG);
                metricRegistry.gauge("dataio_status_map", () -> dependencyTrackingService.getCount(sink.getId(), QUEUED_FOR_DELIVERY), sinkTag, DEL_TAG);
                LOGGER.info("Registered status map metrics for sink -> {}", sink.getContent().getName());
            }
            metricRegistry.gauge("dataio_master", () -> Hazelcast.isMaster() ? 1 : 0);
        } catch (FlowStoreServiceConnectorException e) {
            LOGGER.error("Unable to get sinks list from flowstore:", e);
        } catch (ProcessingException e1) {
            LOGGER.error("Flowstore unavailable:", e1);
        }
    }

    private long getLongestRunningChunkDuration(int sinkId) {
        Long l = maxDeliveryDurations.computeIfAbsent(sinkId, k -> 0L);
        maxDeliveryDurations.put(sinkId, 0L);
        return l;
    }

    public JobSchedulerBean withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    public JobSchedulerBean withJobGateBean(JobGateBean jobGateBean) {
        this.jobGateBean = jobGateBean;
        return this;
    }

    public JobSchedulerBean withDeliveryDispatchRepository(DeliveryDispatchRepository deliveryDispatchRepository) {
        this.deliveryDispatchRepository = deliveryDispatchRepository;
        return this;
    }

    /**
     * Registers given chunk for sequence analysis and schedules it for processing
     *
     * @param chunk next chunk element to enter into sequence analysis
     * @param job   job associated with given chunk
     * @throws NullPointerException if given any null-valued argument
     */
    @Stopwatch
    @Timed(name = "chunks", tags = "status=scheduled")
    public void scheduleChunk(ChunkEntity chunk, JobEntity job) {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(job, "job");
        if(job.getState().isAborted() || JobsBean.isAborted(job.getId())) return;
        int sinkId = job.getCachedSink().getSink().getId();
        String barrierMatchKey = getBarrierMatchKey(job);

        TrackingKey key = new TrackingKey(chunk.getKey().getJobId(), chunk.getKey().getId());
        DependencyTracking e = new DependencyTracking(key, sinkId, (int)job.getSpecification().getSubmitterId(), chunk.getKey().getId() == 0 ? barrierMatchKey : null, chunk.getSequenceAnalysisData().getData());
        Priority priority = chunk.getContainsLiveHeadOrSectionRecord() ? Priority.HIGH : job.getPriority();
        e.setPriority(priority.getValue());
        dependencyTrackingService.addAndBuildDependencies(e, barrierMatchKey);
        jobSchedulerTransactionsBean.submitToProcessingIfPossibleAsync(chunk, sinkId, e.getPriority());
    }

    @SuppressWarnings("unused")
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void updateSinks() {
        if(Hazelcast.isSlave()) return;
        try {
            LOGGER.debug("Updating chunks.blocked metrics");
            List<Sink> sinks = flowStore.getConnector().findAllSinks();
            Map<Integer, Integer> counts = dependencyTrackingService.sinkBlockedCount();
            Map<String, Integer> bc = sinks.stream().collect(Collectors.toMap(s -> s.getContent().getName(), s -> counts.getOrDefault(s.getId(), 0)));
            blockedCounts.putAll(bc);
            for (String sinkName : bc.keySet()) {
                MetricID metricID = getBlockedMetricID(sinkName);
                Gauge<?> gauge = metricRegistry.getGauge(metricID);
                if (gauge == null) metricRegistry.gauge(metricID, () -> blockedCounts.get(sinkName));
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e);
        } catch (ProcessingException e1) {
            LOGGER.error("Flowstore unavailable:", e1);
        }
    }

    /**
     * Ensures that the last committed chunk for the given job ID is scheduled
     * for processing if it hasn't been already.
     *
     * @param jobId ID of job to ensure
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void ensureLastChunkIsScheduled(int jobId) {
        try {
            JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
            if (jobEntity.getState().isAborted() || JobsBean.isAborted(jobId)) return;
            int chunkId = Math.max(0, jobEntity.getNumberOfChunks() - 1);
            ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(chunkId, jobId));
            if (chunkEntity != null && !dependencyTrackingService.isScheduled(chunkEntity)) {
                LOGGER.info("Ensuring chunk {}/{} is scheduled", jobId, chunkId);
                scheduleChunk(chunkEntity, jobEntity);
            }
        } catch (Exception e) {
            LOGGER.error("ensureLastChunkIsScheduled failed for {}", jobId, e);
        }
    }

    /**
     * Adds special job termination barrier chunk to given job if it requires barrier chunks
     *
     * @param jobEntity job being marked as partitioned
     * @throws JobStoreException on failure to create special job termination chunk
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void markJobAsPartitioned(JobEntity jobEntity) throws JobStoreException {
        if (jobEntity.getNumberOfChunks() == 1 && jobEntity.hasFatalError()) {
            // TODO: 22-03-18 The getSucceeded() test below is too restrictive
            /* Consider the case where the first chunk fails fatally in its
               partitioning phase, but not on the first item in the chunk.
               The getSucceeded() == 0 test will be false causing a termination
               chunk to be enqueued even though it is unnecessary. Could an
               existence check for a dependency entity for chunk 0 be used
               instead? */

            // on fatal error and only one chunk - the chunk is probably not submitted for processing,
            // check the database for 0 succeeded from partitioning.
            final JobEntity dbJobEntity = entityManager.find(JobEntity.class, jobEntity.getId());
            entityManager.refresh(dbJobEntity);
            if (dbJobEntity.getState().getPhase(State.Phase.PARTITIONING).getSucceeded() == 0)
                return;
        }

        final String barrierMatchKey = getBarrierMatchKey(jobEntity);
        if (barrierMatchKey != null) {
            final Sink sink = jobEntity.getCachedSink().getSink();

            ChunkItem.Status terminationStatus = ChunkItem.Status.SUCCESS;
            if (jobEntity.hasFatalDiagnostics()) {
                terminationStatus = ChunkItem.Status.FAILURE;
            }

            createAndScheduleTerminationChunk(jobEntity, sink, jobEntity.getNumberOfChunks(),
                    barrierMatchKey, terminationStatus);
        }
    }

    private String getBarrierMatchKey(JobEntity job) {
        if (REQUIRES_TERMINATION_CHUNK.contains(job.getCachedSink().getSink().getContent().getSinkType())) {
            return String.valueOf(job.getSpecification().getSubmitterId());
        }
        return null;
    }

    /**
     * Adds special job termination barrier chunk to given job
     *
     * @param jobEntity       job being marked as partitioned
     * @param sink            ID of sink for the job
     * @param chunkId         ID of termination chunk, which is also the job's data-chunk count and
     *                        therefore the value the per-job gate counts up to
     * @param barrierMatchKey Additional barrier key to wait for
     * @param ItemStatus      status for termination chunk item
     * @throws JobStoreException on failure to create special job termination chunk
     */
    void createAndScheduleTerminationChunk(JobEntity jobEntity, Sink sink, int chunkId, String barrierMatchKey,
                                           ChunkItem.Status ItemStatus) throws JobStoreException {
        int sinkId = sink.getId();
        TrackingKey key = new TrackingKey(jobEntity.getId(), chunkId);
        // Built before the chunk entity so that createJobTerminationChunkEntity can write the row
        // to PostgreSQL with a closed gate in the same transaction that writes the counters, ahead
        // of the map add below. A termination chunk carries no sequence analysis data, so its match
        // keys are the barrier key alone.
        DependencyTracking endTracker = new DependencyTracking(key, sinkId, (int)jobEntity.getSpecification().getSubmitterId(), barrierMatchKey, Set.of())
                .setPriority(Priority.HIGH.getValue());
        // chunkId is numberOfChunks as read in markJobAsPartitioned before this call, which is
        // exactly the job's data-chunk count. Passing it rather than re-reading it downstream is
        // what keeps data_chunks_expected reachable: createJobTerminationChunkEntity increments
        // numberOfChunks itself.
        pgJobStoreRepository.createJobTerminationChunkEntity(jobEntity.getId(), chunkId, "dummyDatafileId", ItemStatus, chunkId, endTracker);
        TrackingKey jobEndKey = dependencyTrackingService.add(endTracker);
        jobSchedulerTransactionsBean.addDependencies(endTracker);
        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(jobEndKey);
    }

    /**
     * Register Chunk Processing is Done.
     * Chunks not i state QUEUED_FOR_PROCESSING is ignored.
     *
     * @param chunk Chunk completed from processing
     */
    @Stopwatch
    @Timed(name = "chunks", tags = "status=processed")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void chunkProcessingDone(Chunk chunk) {
        TrackingKey key = new TrackingKey(chunk.getJobId(), (int)chunk.getChunkId());
        StatusChangeEvent changeEvent = dependencyTrackingService.setValidatedStatus(key, READY_FOR_DELIVERY);
        if(changeEvent == null) {
            LOGGER.info("chunkProcessingDone: Conditional status update got undesirable result skipping");
            return;
        }
        if(changeEvent.getNewStatus() != READY_FOR_DELIVERY) {
            LOGGER.info("chunkProcessingDone: Conditional status update got undesirable result: {}, skipping", changeEvent);
            return;
        }
        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(key);
    }


    /**
     * Registers a chunk as delivered and removes it from dependency tracking
     * <p>
     * If called Multiple times with the same chunk,
     * or a chunk not in QUEUED_FOR_DELIVERY the chunk is ignored.
     * </p>
     *
     * @param chunk chunk having been delivered
     */
    @Stopwatch
    @Timed(name = "chunks", tags = "status=delivered")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void chunkDeliveringDone(Chunk chunk) {
        TrackingKey chunkDoneKey = new TrackingKey(chunk.getJobId(), (int)chunk.getChunkId());
        DependencyTrackingRO chunkDone = dependencyTrackingService.get(chunkDoneKey);

        if (chunkDone == null) {
            LOGGER.info("chunkDeliveringDone: called with unknown chunk {}/{} - assuming it is already completed",
                    chunk.getJobId(), chunk.getChunkId());
            return;
        }
        if (chunkDone.getStatus() != QUEUED_FOR_DELIVERY) {
            LOGGER.info("chunkDeliveringDone: ignoring chunk {}/{} not in state QUEUED_FOR_DELIVERY - was {}",
                    chunk.getJobId(), chunk.getChunkId(), chunkDone.getStatus());
            return;
        }

        long startTime = System.currentTimeMillis();

        int chunkDoneSinkId = chunkDone.getSinkId();
        dependencyTrackingService.remove(chunkDoneKey);

        // Per-job gate: counts this delivery against the job's own gate, or lifts the job's
        // barrier and re-evaluates later jobs if the chunk was its termination chunk.
        jobGateBean.advanceGateState(chunkDoneKey, chunkDoneSinkId, chunkDone.getSubmitter());

        StopWatch findChunksWaitingForMeStopWatch = new StopWatch();
        Set<TrackingKey> unblocked = dependencyTrackingService.removeFromWaitingOn(chunkDoneKey);

        LOGGER.info("chunkDeliveringDone: findChunksWaitingForMe for {} took {} ms unblocked {} chunks", chunkDone.getKey(), findChunksWaitingForMeStopWatch.getElapsedTime(), unblocked.size());

        for (TrackingKey chunkBlockedKey : inDispatchOrder(unblocked)) {
            // Attempts to unblock all chunks found waiting for "me" must happen
            // in separate transactions or else there is a risk of exhausting the
            // JMS connection pool and also of ending up stuck in DIRECT mode when
            // it should be BULK causing the sink delivery to stall because changes
            // to ready state will be seen to late by the bulk submitter.
            if(JobsBean.isAborted(chunk.getJobId())) throw new JobAborted(chunk.getJobId());
            jobSchedulerTransactionsBean.attemptToUnblockChunk(chunkBlockedKey);

        }
        if (!unblocked.isEmpty()) {
            LOGGER.info("chunkDeliveringDone: removing {}", chunkDone.getKey());
        }

        long thisDuration = System.currentTimeMillis() - startTime;
        maxDeliveryDurations.merge(chunkDoneSinkId, thisDuration, Math::max);
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Future<Integer> bulkScheduleToProcessingForSink(int sinkId) {
        int chunksPushedToQueue = 0;
        try {
            int spaceLeftInQueue = dependencyTrackingService.capacity(sinkId, QUEUED_FOR_PROCESSING);
            if (spaceLeftInQueue > 0) {
                Collection<DependencyTracking> chunks = dependencyTrackingService.findDependencies(SCHEDULED_FOR_PROCESSING, sinkId, spaceLeftInQueue);

                if(!chunks.isEmpty()) LOGGER.info("bulk scheduling for processing - found {} chunks ready for processing for sink {}", chunks.size(), sinkId);
                for (DependencyTracking toSchedule : chunks) {
                    if(!JobsBean.isAborted(toSchedule.getKey().getJobId())) {
                        TrackingKey toScheduleKey = toSchedule.getKey();
                        LOGGER.info("bulk scheduling for processing - chunk {} to be scheduled for processing for sink {}", toScheduleKey, sinkId);
                        ChunkEntity chunk = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(toScheduleKey.getChunkId(), toScheduleKey.getJobId()));
                        jobSchedulerTransactionsBean.submitToProcessing(chunk, toSchedule.getPriority());
                        chunksPushedToQueue++;
                    }
                }
            } else LOGGER.info("bulk scheduling for processing - sink {} capacity={}", sinkId, spaceLeftInQueue);
        } catch (Exception ex) {
            LOGGER.error("Error in bulk scheduling for processing for sink {}", sinkId, ex);
        }
        return new AsyncResult<>(chunksPushedToQueue);
    }

    /**
     * Fills a sink's delivery queue in {@code (priority DESC, jobId ASC, chunkId ASC)} order, never
     * dispatching a chunk whose gate is closed.
     * <p>
     * Candidates come from SQL rather than from the Hazelcast map, because no predicate over the map
     * can see {@code gate_open}, see {@link DeliveryDispatchRepository}. The table is not the
     * authority on {@code status} though, so each candidate is re-checked against the map before it
     * is acted on: a row still reading {@code SCHEDULED_FOR_DELIVERY} may belong to a chunk already
     * dispatched, or already delivered and gone.
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<Integer> bulkScheduleToDeliveringForSink(int sinkId) {
        int chunksPushedToQueue = 0;
        try {
            int spaceLeftInQueue = dependencyTrackingService.capacity(sinkId, QUEUED_FOR_DELIVERY);
            if (spaceLeftInQueue > 0) {
                LOGGER.debug("bulk scheduling for delivery - sink {} has space left in queue for {} chunks", sinkId, spaceLeftInQueue);

                List<TrackingKey> chunks = deliveryDispatchRepository.findDeliveryCandidates(
                        sinkId, spaceLeftInQueue + staleCandidateSlack());

                if(!chunks.isEmpty()) LOGGER.info("bulk scheduling for delivery - found {} candidate chunks for sink {}", chunks.size(), sinkId);
                for (TrackingKey toSchedule : chunks) {
                    if(chunksPushedToQueue == spaceLeftInQueue) break;
                    if(JobsBean.isAborted(toSchedule.getJobId())) continue;
                    if(!isStillAwaitingDelivery(toSchedule)) continue;
                    LOGGER.info("bulk scheduling for delivery - chunk {} to be scheduled for delivery for sink {}", toSchedule, sinkId);
                    if(jobSchedulerTransactionsBean.submitToDeliveringNewTransaction(toSchedule)) {
                        chunksPushedToQueue++;
                    } else dependencyTrackingService.remove(toSchedule);
                }
            } else LOGGER.info("bulk scheduling for delivery - sink {} capacity={}", sinkId, spaceLeftInQueue);
        } catch (Exception ex) {
            LOGGER.error("Error in bulk scheduling for delivery for sink {}", sinkId, ex);
        }
        return new AsyncResult<>(chunksPushedToQueue);
    }

    /**
     * Puts the chunks one delivery unblocked into the same order the bulk submitter dispatches in.
     * <p>
     * Each of these is handed to its own transaction and each can take the last free slot of
     * {@code QUEUED_FOR_DELIVERY}, so the order they are visited in decides which of them reach the
     * sink now and which wait for the next sweep. Unordered, that was whatever order the set
     * happened to iterate in.
     * <p>
     * Priority is not carried by the keys, so it is read per key from dependency tracking. A key
     * whose entry has gone in the meantime drops out, which is correct, there is nothing left to
     * unblock.
     *
     * @param keys chunks whose last dependency was just cleared
     * @return the same chunks in {@code (priority DESC, jobId ASC, chunkId ASC)} order
     */
    private List<TrackingKey> inDispatchOrder(Set<TrackingKey> keys) {
        return keys.stream()
                .map(dependencyTrackingService::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.<DependencyTrackingRO>comparingInt(DependencyTrackingRO::getPriority).reversed()
                        .thenComparingInt(dt -> dt.getKey().getJobId())
                        .thenComparingInt(dt -> dt.getKey().getChunkId()))
                .map(DependencyTrackingRO::getKey)
                .toList();
    }

    /**
     * How far past the free queue slots the candidate query reaches, to see past rows whose status
     * is stale.
     * <p>
     * The MapStore writes {@code status} write-behind, so a chunk that has been dispatched keeps a
     * row reading {@code SCHEDULED_FOR_DELIVERY} until the next flush. Those rows sort first, since
     * they were queued earliest and so carry the lowest job ids, and under steady load there are far
     * more of them than there are free slots. A window of exactly {@code spaceLeftInQueue} rows would
     * be filled almost entirely by chunks that have already gone, and the sink would deliver close to
     * nothing until the flush caught up.
     * <p>
     * The stale set is bounded by the chunks that have left {@code SCHEDULED_FOR_DELIVERY} since the
     * last flush, and the cap on {@code QUEUED_FOR_DELIVERY} bounds how many of those can still be in
     * flight. Rows for chunks removed inside the window are additional, so this is a good bound
     * rather than a proof, and a sweep that comes up short costs one second: the next one runs
     * immediately after.
     * <p>
     * <b>This method disappears with the Hazelcast MapStore.</b> Once {@code dependencytracking} is
     * the sole store, {@code status} is written in the transaction that decides it, a window of
     * exactly {@code spaceLeftInQueue} is correct, and there is no lag left to reach past. Delete
     * this method, the call to it, {@link #isStillAwaitingDelivery} and
     * {@code DeliveryDispatchStaleStatusIT} in the same commit as the MapStore.
     */
    private static int staleCandidateSlack() {
        return QUEUED_FOR_DELIVERY.getMax();
    }

    /**
     * @param key candidate returned by the SQL query
     * @return true if the map, which is the authority on status, still has this chunk waiting for
     * delivery
     * <p>
     * <b>This method disappears with the Hazelcast MapStore</b>, which is the only reason the table's
     * {@code status} can be stale, see {@link #staleCandidateSlack}.
     */
    private boolean isStillAwaitingDelivery(TrackingKey key) {
        DependencyTrackingRO tracker = dependencyTrackingService.get(key);
        return tracker != null && tracker.getStatus() == SCHEDULED_FOR_DELIVERY;
    }

    /**
     * Reload and reset counters for sinks
     * Set all sinks to BULK mode to ensure progress on redeploy of service
     */
    @Stopwatch
    public void loadSinkStatusOnBootstrap(Set<Integer> sinkIds) {
        dependencyTrackingService.recountSinkStatus(sinkIds);
        LOGGER.info("Reset sink counters");
    }

    private MetricID getBlockedMetricID(String sinkName) {
        return new MetricID("chunks.blocked", new Tag("sink_name", sinkName));
    }
}
