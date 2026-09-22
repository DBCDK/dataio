package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;

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
 *     {@code submitToDeliveringIfPossible} finds the sink has capacity and nothing waiting ahead of
 *     the chunk at the moment it becomes ready,</li>
 * <li>otherwise the chunk is left in {@code SCHEDULED_FOR_PROCESSING} or
 *     {@code SCHEDULED_FOR_DELIVERY}, and {@link JobSchedulerBulkSubmitterBean}'s timers pick it up
 *     once the sink has room. Those timers call {@link #bulkScheduleToProcessingForSink} and
 *     {@link #bulkScheduleToDeliveringForSink} here.</li>
 * </ul>
 * A chunk turned away for either reason waits where it is for the next sweep.
 * <p>
 * <b>Capacity alone does not entitle a chunk to a free slot.</b> Both sweeps dispatch in
 * {@code (priority DESC, jobId ASC, chunkId ASC)} order, and the direct paths keep to it by
 * standing down when a waiting chunk ranks above the one they hold. Otherwise a job partitioning
 * right now takes every slot freed between two sweeps, and a backlog ranked above it waits for the
 * burst to end. See {@link DispatchOrder}.
 * <p>
 * The counts behind the cap are held in a distributed map, so the cap applies across job-store
 * instances rather than per JVM. The timer-driven work, both sweeps in
 * {@link JobSchedulerBulkSubmitterBean}, returns early on every instance but one, each guarded by
 * {@code Hazelcast.isSlave}.
 */
@Stateless
@SuppressWarnings("PMD.TooManyStaticImports")
public class JobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBean.class);
    /** Counts the chunks whose work completed while their scheduling row stayed behind. */
    private static final String STUCK_CHUNKS = "dataio_stuck_chunks";
    private static final Tag PROC_TAG = new Tag("state", "processing");
    private static final Tag DEL_TAG = new Tag("state", "delivering");

    private static final Set<SinkContent.SinkType> REQUIRES_TERMINATION_CHUNK = new HashSet<>(Set.of(SinkContent.SinkType.MARCCONV, SinkContent.SinkType.PERIODIC_JOBS, SinkContent.SinkType.TICKLE));

    /**
     * Sink types whose job-end work is not scoped to its own job, so a later job's data must not
     * reach the sink until the earlier job's termination chunk has been delivered.
     * <p>
     * A subset of {@link #REQUIRES_TERMINATION_CHUNK}, since holding a later job behind a barrier
     * is meaningless for a sink type that raises no barrier. Tickle is the case: {@code createBatch}
     * marks and {@code closeBatch} sweeps the whole dataset rather than one batch, so two open
     * batches on one dataset delete each other's records. Marcconv and periodic-jobs finalize by job
     * id and need only the narrower guarantee that job-end events reach the sink in order.
     * <p>
     * Kept here rather than on {@code SinkContent} because width follows from what the sink
     * implementation does at job end, not from an operator choice, and because {@code SinkContent}
     * is cached per job, so a new field would need a defensible default for every cached sink
     * already written.
     */
    private static final Set<SinkContent.SinkType> REQUIRES_FULL_WIDTH_BARRIER = Set.of(SinkContent.SinkType.TICKLE);

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
     * Registers given chunk with the scheduler and schedules it for processing
     *
     * @param chunk next chunk element to schedule
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

        TrackingKey key = new TrackingKey(chunk.getKey().getJobId(), chunk.getKey().getId());
        int submitter = (int) job.getSpecification().getSubmitterId();
        Priority priority = chunk.getContainsLiveHeadOrSectionRecord() ? Priority.HIGH : job.getPriority();
        int inserted = jobGateBean.insertDataChunkRow(key, sinkId, submitter, READY_FOR_PROCESSING,
                priority.getValue(), isBlockedByEarlierBarrier(job, sinkId, submitter));
        if (inserted > 0) {
            dependencyTrackingService.countInsertedChunk(sinkId, READY_FOR_PROCESSING);
        }
        jobSchedulerTransactionsBean.submitToProcessingIfPossibleAsync(chunk, sinkId, priority.getValue());
    }

    /**
     * Whether a data chunk of this job has to be held back because an earlier job in the same
     * barrier scope still holds an unlifted barrier.
     * <p>
     * Only for the sink types in {@link #REQUIRES_FULL_WIDTH_BARRIER}. For every other type a data
     * chunk is dispatchable as soon as it is processed, which is the narrower guarantee the gate
     * gives by default, and no barrier is read at all.
     * <p>
     * Unlocked, and the whole cost in the common case, see
     * {@link JobGateBean#isBlockedByEarlierBarrier}. Only a chunk that has something to wait for
     * pays for the locked re-read in {@link JobGateBean#insertDataChunkRow}.
     * <p>
     * <b>Insert time is the only evaluation site this needs</b>, unlike a job's own termination
     * gate, and the reason sits outside the gate: the jobqueue partitions jobs with the same
     * submitter on the same sink strictly one at a time in job id order
     * ({@code NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER}), which is the same scope the barrier uses. By
     * the time this job's chunks are inserted the earlier job is fully partitioned, so its barrier
     * either stands or has already been lifted, and there is no window like the one that forces a
     * termination gate to be evaluated in two places. If that jobqueue invariant is ever relaxed, a
     * data chunk's gate needs evaluating in two places too.
     */
    private boolean isBlockedByEarlierBarrier(JobEntity job, int sinkId, int submitter) {
        return requiresFullWidthBarrier(job.getCachedSink().getSink().getContent().getSinkType())
                && jobGateBean.isBlockedByEarlierBarrier(sinkId, submitter, job.getId());
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

        if (requiresTerminationChunk(jobEntity.getCachedSink().getSink().getContent().getSinkType())) {
            final Sink sink = jobEntity.getCachedSink().getSink();

            ChunkItem.Status terminationStatus = ChunkItem.Status.SUCCESS;
            if (jobEntity.hasFatalDiagnostics()) {
                terminationStatus = ChunkItem.Status.FAILURE;
            }

            createAndScheduleTerminationChunk(jobEntity, sink, jobEntity.getNumberOfChunks(), terminationStatus);
        }
    }

    /**
     * @param sinkType sink type to ask about
     * @return true if jobs for this sink type get a termination chunk, and so raise a barrier that
     * orders their job-end against other jobs from the same submitter
     */
    static boolean requiresTerminationChunk(SinkContent.SinkType sinkType) {
        return REQUIRES_TERMINATION_CHUNK.contains(sinkType);
    }

    /**
     * @param sinkType sink type to ask about
     * @return true if the barrier for this sink type is full width, holding back every chunk of a
     * later job rather than only that job's termination chunk
     */
    static boolean requiresFullWidthBarrier(SinkContent.SinkType sinkType) {
        return REQUIRES_FULL_WIDTH_BARRIER.contains(sinkType);
    }

    /**
     * Adds special job termination barrier chunk to given job
     *
     * @param jobEntity       job being marked as partitioned
     * @param sink            ID of sink for the job
     * @param chunkId         ID of termination chunk, which is also the job's data-chunk count and
     *                        therefore the value the per-job gate counts up to
     * @param ItemStatus      status for termination chunk item
     * @throws JobStoreException on failure to create special job termination chunk
     */
    void createAndScheduleTerminationChunk(JobEntity jobEntity, Sink sink, int chunkId,
                                           ChunkItem.Status ItemStatus) throws JobStoreException {
        int sinkId = sink.getId();
        TrackingKey key = new TrackingKey(jobEntity.getId(), chunkId);
        // Carries what createJobTerminationChunkEntity writes into the row, which is the only
        // insert of it.
        //
        // READY_FOR_DELIVERY is set explicitly, and it has to be. A termination chunk is created
        // already processed, its item carrying both a PARTITIONING and a PROCESSING outcome, so it
        // never passes through the processing phase that would otherwise advance it. Left at
        // READY_FOR_PROCESSING it is silently undeliverable, because submitToDeliveringIfPossible
        // declines it: QUEUED_FOR_DELIVERY is not a valid change from there.
        //
        // Unconditional, and that is the whole point of the gate: what holds a termination chunk
        // back until its own job's data chunks are acknowledged is gate_open on the row written
        // below, not a status.
        DependencyTracking terminationRow = new DependencyTracking(key, sinkId, (int)jobEntity.getSpecification().getSubmitterId())
                .setPriority(Priority.HIGH.getValue())
                .setStatus(READY_FOR_DELIVERY)
                .setTermination(true);
        // chunkId is numberOfChunks as read in markJobAsPartitioned before this call, which is
        // exactly the job's data-chunk count. Passing it rather than re-reading it downstream is
        // what keeps data_chunks_expected reachable: createJobTerminationChunkEntity increments
        // numberOfChunks itself.
        pgJobStoreRepository.createJobTerminationChunkEntity(jobEntity.getId(), chunkId, "dummyDatafileId", ItemStatus, chunkId, terminationRow);
        // Unconditional, unlike the data chunk path, because the insert above is an upsert and
        // always writes a row. It is sound for the same reason the upsert's conflict clause is
        // unreachable: the chunk row is persisted first and its primary key rejects a second call
        // for one job, see JobGateRepository#insertTerminationRow.
        dependencyTrackingService.countInsertedChunk(sinkId, READY_FOR_DELIVERY);
        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(key);
    }

    /**
     * Runs a stale chunk's completion call again, in a transaction of its own.
     * <p>
     * The stale sweep re-drives every chunk whose phase has already finished, and both completion
     * calls write: one advances the scheduling row and dispatches, the other removes the row and
     * counts it against its job's gate. Run in the sweep's own transaction, one chunk that throws
     * would roll back the whole sweep, including the rescue that ran before it, and no chunk would
     * be resent. The same chunk is stale again a minute later, so the sweep would stay dead and
     * nothing is watching it.
     * <p>
     * Its own transaction is what bounds a failure to the chunk that caused it, and the caller then
     * only has to catch the exception. Safe against the sweep's uncommitted writes because the two
     * touch disjoint rows, the rescue moving only the {@code READY_*} statuses and this only the
     * {@code QUEUED_*} ones, and because nothing on either dispatch path locks a row it reads.
     *
     * @param chunk chunk to re-drive, typed for the phase it has finished
     * @param phase phase the chunk's row already reports finished
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void advanceCompletedChunk(Chunk chunk, State.Phase phase) {
        if (phase == State.Phase.PROCESSING) {
            chunkProcessingDone(chunk);
        } else {
            chunkDeliveringDone(chunk);
        }
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
        if(dependencyTrackingService.setValidatedStatus(key, READY_FOR_DELIVERY).isEmpty()) {
            logUnadvanced(key);
            return;
        }
        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(key);
    }

    /**
     * Logs why the advance to {@code READY_FOR_DELIVERY} moved no row, and counts the one case
     * that leaves a chunk stuck.
     * <p>
     * Three things bring a caller here. The row is gone, or it has already reached the delivery
     * half, and in both the chunk needed nothing from this call. Or the row is still in the
     * processing half, which means the chunk's items carry their processing outcome while the row
     * that would take them into delivery has not moved. Nothing further arrives for that chunk,
     * so it waits for the stale sweep, and here is where that becomes true.
     * <p>
     * Costs one read, on a path that is rare by construction.
     */
    private void logUnadvanced(TrackingKey key) {
        DependencyTrackingRO tracking = dependencyTrackingService.get(key);
        if (tracking == null) {
            LOGGER.info("chunkProcessingDone: called with unknown chunk {} - assuming it is already completed",
                    key.toChunkIdentifier());
            return;
        }
        if (isPastProcessing(tracking.getStatus())) {
            LOGGER.info("chunkProcessingDone: ignoring chunk {} already past processing in state {}",
                    key.toChunkIdentifier(), tracking.getStatus());
            return;
        }
        metricRegistry.counter(STUCK_CHUNKS, PROC_TAG).inc();
        LOGGER.warn("chunkProcessingDone: chunk {} is processed and its row is still {}, so nothing will carry it into delivery",
                key.toChunkIdentifier(), tracking.getStatus());
    }

    /**
     * Whether a chunk's scheduling row has left the processing half.
     * <p>
     * Exhaustive rather than defaulted, so a status added later is a compile error here instead of
     * a chunk quietly counted on the wrong side.
     *
     * @param status status the chunk's row holds
     * @return true if the row is in the delivery half
     */
    private static boolean isPastProcessing(ChunkSchedulingStatus status) {
        return switch (status) {
            case READY_FOR_DELIVERY, SCHEDULED_FOR_DELIVERY, QUEUED_FOR_DELIVERY -> true;
            case READY_FOR_PROCESSING, SCHEDULED_FOR_PROCESSING, QUEUED_FOR_PROCESSING -> false;
        };
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
        long startTime = System.currentTimeMillis();
        TrackingKey key = new TrackingKey(chunk.getJobId(), (int)chunk.getChunkId());

        // One statement asks the question and takes the answer away with it. The row it returns is
        // this acknowledgement's once-only token: two concurrent acknowledgements of one chunk
        // cannot both delete it, and a counter cannot survive being told twice, since each call
        // adds exactly 1 so the count still lands on data_chunks_expected, only too early, and the
        // gate opens with data chunks still in flight.
        //
        // advanceGateState then runs in this same transaction, so the delete and the increment
        // commit together. A failure after the delete rolls the row back with the count, and the
        // redelivery that follows finds the chunk still there to be counted.
        Optional<DependencyTracking> delivered = dependencyTrackingService.acknowledgeDelivery(key);
        if (delivered.isEmpty()) {
            logUnacknowledged(key);
            return;
        }
        jobGateBean.advanceGateState(delivered.get());

        long thisDuration = System.currentTimeMillis() - startTime;
        maxDeliveryDurations.merge(delivered.get().getSinkId(), thisDuration, Math::max);
    }

    /**
     * Logs why the acknowledgement removed no row, and counts the one case that leaves a chunk
     * stuck.
     * <p>
     * Three things bring a caller here. A missing row is the ordinary case: the row is this
     * acknowledgement's once-only token, and a redelivery re-triggers the call for a chunk already
     * counted. A row the stale sweep has put back for another attempt, which is any status other
     * than {@code QUEUED_FOR_DELIVERY}, means this report belongs to an attempt that has been
     * superseded, and the chunk is on its way rather than stuck. Both stay at info.
     * <p>
     * A row still in {@code QUEUED_FOR_DELIVERY} is the case worth a warning. The acknowledgement
     * removes exactly that status, so a delete that matched nothing against a row still holding it
     * means the sink has reported the chunk delivered and nothing recorded it.
     * <p>
     * Costs one read, on a path that is rare by construction.
     */
    private void logUnacknowledged(TrackingKey key) {
        DependencyTrackingRO tracking = dependencyTrackingService.get(key);
        if (tracking == null) {
            LOGGER.info("chunkDeliveringDone: called with unknown chunk {} - assuming it is already completed",
                    key.toChunkIdentifier());
            return;
        }
        if (tracking.getStatus() != ChunkSchedulingStatus.QUEUED_FOR_DELIVERY) {
            LOGGER.info("chunkDeliveringDone: ignoring chunk {}, sent again since and now in state {}",
                    key.toChunkIdentifier(), tracking.getStatus());
            return;
        }
        metricRegistry.counter(STUCK_CHUNKS, DEL_TAG).inc();
        LOGGER.warn("chunkDeliveringDone: chunk {} is delivered and its row is still {}, so it will be sent again",
                key.toChunkIdentifier(), tracking.getStatus());
    }

    /**
     * Fills a sink's processor queue in {@code (priority DESC, jobId ASC, chunkId ASC)} order.
     * <p>
     * The candidate query supplies the priority the dispatch needs, so there is no per-chunk read,
     * and it does not look at {@code gate_open} at all, see
     * {@link DependencyTrackingRepository#findProcessingCandidates}.
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Future<Integer> bulkScheduleToProcessingForSink(int sinkId) {
        int chunksPushedToQueue = 0;
        try {
            int spaceLeftInQueue = dependencyTrackingService.capacity(sinkId, QUEUED_FOR_PROCESSING);
            if (spaceLeftInQueue > 0) {
                List<DependencyTrackingRepository.ProcessingCandidate> chunks =
                        dependencyTrackingService.findProcessingCandidates(sinkId, spaceLeftInQueue);

                if(!chunks.isEmpty()) LOGGER.info("bulk scheduling for processing - found {} chunks ready for processing for sink {}", chunks.size(), sinkId);
                for (DependencyTrackingRepository.ProcessingCandidate toSchedule : chunks) {
                    if(!JobsBean.isAborted(toSchedule.key().getJobId())) {
                        TrackingKey toScheduleKey = toSchedule.key();
                        LOGGER.info("bulk scheduling for processing - chunk {} to be scheduled for processing for sink {}", toScheduleKey, sinkId);
                        ChunkEntity chunk = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(toScheduleKey.getChunkId(), toScheduleKey.getJobId()));
                        jobSchedulerTransactionsBean.submitToProcessing(chunk, toSchedule.priority());
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
     * dispatching a chunk whose gate is closed, see {@link DeliveryDispatchRepository}.
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<Integer> bulkScheduleToDeliveringForSink(int sinkId) {
        int chunksPushedToQueue = 0;
        try {
            int spaceLeftInQueue = dependencyTrackingService.capacity(sinkId, QUEUED_FOR_DELIVERY);
            if (spaceLeftInQueue > 0) {
                LOGGER.debug("bulk scheduling for delivery - sink {} has space left in queue for {} chunks", sinkId, spaceLeftInQueue);

                List<DeliveryDispatchRepository.DeliveryCandidate> candidates = deliveryDispatchRepository.findDeliveryCandidates(sinkId, spaceLeftInQueue);

                if (!candidates.isEmpty()) {
                    LOGGER.info("bulk scheduling for delivery - found {} candidate chunks for sink {}", candidates.size(), sinkId);
                }
                for (DeliveryDispatchRepository.DeliveryCandidate candidate : candidates) {
                    TrackingKey toSchedule = candidate.key();
                    if(JobsBean.isAborted(toSchedule.getJobId())) continue;
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
     * Reload and reset counters for sinks
     * Set all sinks to BULK mode to ensure progress on redeploy of service
     */
    @Stopwatch
    public void loadSinkStatusOnBootstrap(Set<Integer> sinkIds) {
        dependencyTrackingService.recountSinkStatus(sinkIds);
        LOGGER.info("Reset sink counters");
    }
}
