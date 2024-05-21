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
import dk.dbc.dataio.jobstore.distributed.JobSchedulerSinkStatus;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.BLOCKED;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;

/**
 * Handles chunk scheduling as they pass through partitioning, processing and delivery phases.
 * <p>
 * Three modes of operations exist for sink and queue type combinations:
 * <p>
 * DIRECT : When no rate limiting is needed, chunks are enqueued directly
 * (this is the default mode).
 * </p>
 * <p>
 * BULK   : If MAX_NUMBER_OF_.. chunks are enqueued, the scheduler transitions
 * to BULK mode and it is up to the {@link JobSchedulerBulkSubmitterBean}
 * to handle enqueueing.
 * </p>
 * <p>
 * TRANSITION_TO_DIRECT : The transition back from BULK to DIRECT mode must take
 * at least 2 seconds to allow time for all chunks to be picked
 * up by the DIRECT mode, meaning chunks are enqueued directly,
 * but at the same time the {@link JobSchedulerBulkSubmitterBean}
 * is also scanning for records to pickup chunks added during mode
 * switch.
 * </p>
 * <p>
 * Note: Queue limits are handled per JVM process posing a hindrance for distributed scheduling.
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

    @Inject
    MetricRegistry metricRegistry;
    @EJB
    FlowStoreServiceConnectorBean flowStore;
    @Inject
    DependencyTrackingService dependencyTrackingService;

    private static final Map<String, Integer> blockedCounts = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> maxDeliveryDurations = new ConcurrentHashMap<>();


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
                metricRegistry.gauge("dataio_sink_mode", () -> dependencyTrackingService.getSinkStatus(sink.getId()).getProcessingStatus().getMode().ordinal(), PROC_TAG);
                metricRegistry.gauge("dataio_sink_mode", () -> dependencyTrackingService.getSinkStatus(sink.getId()).getDeliveringStatus().getMode().ordinal(), DEL_TAG);
                LOGGER.info("Registered status map metrics for sink -> {}", sink.getContent().getName());
            }
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

        DependencyTracking e = new DependencyTracking(chunk.getKey().getJobId(), chunk.getKey().getId(), sinkId, chunk.getKey().getId() == 0 ? barrierMatchKey : null, chunk.getSequenceAnalysisData().getData());
        e.setSubmitter(Math.toIntExact(job.getSpecification().getSubmitterId()));
        e.setPriority(job.getPriority().getValue());

        jobSchedulerTransactionsBean.persistDependencyEntity(e, barrierMatchKey);

        // check before submit to avoid unnecessary Async call.
        if (dependencyTrackingService.getSinkStatus(sinkId).isProcessingModeDirectSubmit()) {
            jobSchedulerTransactionsBean.submitToProcessingIfPossibleAsync(chunk, sinkId, e.getPriority());
        }
    }

    @SuppressWarnings("unused")
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void updateSinks() {
        if(Hazelcast.isSlave()) return;
        try {
            LOGGER.info("Updating chunks.blocked metrics");
            List<Sink> sinks = flowStore.getConnector().findAllSinks();
            Map<Integer, Integer> counts = dependencyTrackingService.sinkStatusCount(BLOCKED);
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
     * @param chunkId         ID of termination chunk
     * @param barrierMatchKey Additional barrier key to wait for
     * @param ItemStatus      status for termination chunk item
     * @throws JobStoreException on failure to create special job termination chunk
     */
    void createAndScheduleTerminationChunk(JobEntity jobEntity, Sink sink, int chunkId, String barrierMatchKey,
                                           ChunkItem.Status ItemStatus) throws JobStoreException {
        int sinkId = sink.getId();
        ChunkEntity chunkEntity = pgJobStoreRepository.createJobTerminationChunkEntity(jobEntity.getId(), chunkId, "dummyDatafileId", ItemStatus);
        DependencyTracking endTracker = new DependencyTracking(chunkEntity.getKey().getJobId(), chunkId, sinkId, barrierMatchKey, chunkEntity.getSequenceAnalysisData().getData())
                .setSubmitter(Math.toIntExact(jobEntity.getSpecification().getSubmitterId()))
                .setPriority(Priority.HIGH.getValue());
        TrackingKey jobEndKey = dependencyTrackingService.add(endTracker);
        jobSchedulerTransactionsBean.addDependencies(endTracker);
        Chunk processedChunk = jobSchedulerTransactionsBean.getProcessedChunkFrom(jobEndKey);
        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(processedChunk, jobEndKey);
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
        StatusChangeEvent changeEvent = dependencyTrackingService.setStatus(key, QUEUED_FOR_PROCESSING, READY_FOR_DELIVERY);
        if(changeEvent == null || changeEvent.getNewStatus() != READY_FOR_DELIVERY) {
            LOGGER.info("chunkProcessingDone: Conditional status update got undesirable result: {}, skipping", changeEvent);
            return;
        }
        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(chunk, key);
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

        StopWatch findChunksWaitingForMeStopWatch = new StopWatch();
        Set<TrackingKey> unblocked = dependencyTrackingService.removeFromWaitingOn(chunkDoneKey);

        LOGGER.info("chunkDeliveringDone: findChunksWaitingForMe for {} took {} ms unblocked {} chunks", chunkDone.getKey(), findChunksWaitingForMeStopWatch.getElapsedTime(), unblocked.size());

        JobSchedulerSinkStatus.QueueStatus sinkQueueStatus = dependencyTrackingService.getSinkStatus(chunkDoneSinkId).getDeliveringStatus();

        for (TrackingKey chunkBlockedKey : unblocked) {
            // Attempts to unblock all chunks found waiting for "me" must happen
            // in separate transactions or else there is a risk of exhausting the
            // JMS connection pool and also of ending up stuck in DIRECT mode when
            // it should be BULK causing the sink delivery to stall because changes
            // to ready state will be seen to late by the bulk submitter.
            if(JobsBean.isAborted(chunk.getJobId())) throw new JobAborted(chunk.getJobId());
            jobSchedulerTransactionsBean.attemptToUnblockChunk(chunkBlockedKey, sinkQueueStatus);

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
            int ready = dependencyTrackingService.getCount(sinkId, READY_FOR_PROCESSING);
            int spaceLeftInQueue = dependencyTrackingService.capacity(sinkId, QUEUED_FOR_PROCESSING);
            if (spaceLeftInQueue > 0) {
                List<DependencyTracking> chunks = dependencyTrackingService.findStream(READY_FOR_PROCESSING, sinkId)
                        .limit(spaceLeftInQueue)
                        .collect(Collectors.toList());

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
            } else LOGGER.info("bulk scheduling for processing - sink {} capacity={} ready={}", sinkId, spaceLeftInQueue, ready);
        } catch (Exception ex) {
            LOGGER.error("Error in bulk scheduling for processing for sink {}", sinkId, ex);
        }
        return new AsyncResult<>(chunksPushedToQueue);
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<Integer> bulkScheduleToDeliveringForSink(int sinkId, JobSchedulerSinkStatus.QueueStatus queueStatus) {
        int chunksPushedToQueue = 0;
        try {
            int ready = dependencyTrackingService.getCount(sinkId, READY_FOR_DELIVERY);
            int spaceLeftInQueue = dependencyTrackingService.capacity(sinkId, QUEUED_FOR_DELIVERY);
            if (spaceLeftInQueue > 0) {
                LOGGER.debug("bulk scheduling for delivery - sink {} has space left in queue for {} chunks", sinkId, spaceLeftInQueue);

                List<TrackingKey> chunks = dependencyTrackingService.find(READY_FOR_DELIVERY, sinkId, spaceLeftInQueue);

                if(!chunks.isEmpty()) LOGGER.info("bulk scheduling for delivery - found {} chunks ready for processing for sink {}", chunks.size(), sinkId);
                for (TrackingKey toSchedule : chunks) {
                    if(!JobsBean.isAborted(toSchedule.getJobId())) {
                        LOGGER.info("bulk scheduling for delivery - chunk {} to be scheduled for delivery for sink {}", toSchedule, sinkId);
                        Chunk chunk = jobSchedulerTransactionsBean.getProcessedChunkFrom(toSchedule);
                        if(chunk != null) {
                            jobSchedulerTransactionsBean.submitToDeliveringNewTransaction(chunk, queueStatus);
                            chunksPushedToQueue++;
                        } else dependencyTrackingService.remove(toSchedule);
                    }
                }
            } else LOGGER.info("bulk scheduling for delivery - sink {} capacity={} ready={}", sinkId, spaceLeftInQueue, ready);
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

    private MetricID getBlockedMetricID(String sinkName) {
        return new MetricID("chunks.blocked", new Tag("sink_name", sinkName));
    }
}
