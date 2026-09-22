package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.ejb.JobGateBean;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.jms.artemis.AdminClient;
import dk.dbc.jms.artemis.AdminClientFactory;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.glassfish.jersey.internal.guava.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;

@Stateless
@Path("/")
public class AdminBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBean.class);
    /** Counts the sink and status pairs the hourly recount found the counters had wrong. */
    private static final String SINK_STATUS_COUNTER_DRIFT = "dataio_sink_status_counter_drift";
    @EJB
    JobSchedulerBean jobSchedulerBean;
    @EJB
    PgJobStoreRepository jobStoreRepository;
    @EJB
    JobGateBean jobGateBean;

    private Instant nextJobCheckFrom = null;

    @EJB
    FlowStoreServiceConnectorBean flowstore;
    @Inject
    DependencyTrackingService dependencyTrackingService;

    @Inject
    @ConfigProperty(name = "PROCESSOR_TIMEOUT", defaultValue = "PT1H")
    private Duration processorTimeout;

    /**
     * How many times one chunk may be sent again before it is reported as beyond repair.
     * <p>
     * Each retry sets {@code lastmodified}, and a chunk becomes stale again only once that is
     * older than the phase's timeout, so the retries are already a timeout apart and need no
     * backoff of their own.
     */
    @Inject
    @ConfigProperty(name = "CHUNK_RESEND_LIMIT", defaultValue = "3")
    int chunkResendLimit;

    JSONBContext jsonbContext = new JSONBContext();

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Inject
    private MetricRegistry metricRegistry;

    AdminClient adminClient = AdminClientFactory.getAdminClient();
    private static final Map<String, AtomicInteger> staleChunks = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> exhaustedRetries = new ConcurrentHashMap<>();
    private static final Set<TrackingKey> exhaustedRetriesReported = ConcurrentHashMap.newKeySet();
    private final org.glassfish.jersey.internal.guava.Cache<Integer, Sink> sinkMap = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

    @SuppressWarnings("unused")
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void updateStaleChunks() {
        if(Hazelcast.isSlave()) return;
        try {
            rescueChunksLeftReady();
            Stream<DependencyTrackingRO> delStream = dependencyTrackingService.getStaleDependencies(QUEUED_FOR_DELIVERY, Duration.ofHours(1)).stream().filter(this::isTimeout);
            Stream<DependencyTrackingRO> procStream = dependencyTrackingService.getStaleDependencies(QUEUED_FOR_PROCESSING, processorTimeout).stream();
            List<DependencyTrackingRO> stale = Stream.concat(delStream, procStream).collect(Collectors.toList());
            // Advancing costs nothing and always succeeds, so it goes first and the resend below
            // sees only the chunks whose work really is outstanding.
            List<DependencyTrackingRO> list = advanceChunksWhosePhaseFinished(stale);
            resendIfNeeded(list);
            reportExhaustedRetries(list);
            list.stream().map(s -> getSinkName(s.getSinkId())).distinct().filter(s -> staleChunks.putIfAbsent(s, new AtomicInteger(0)) == null).forEach(this::registerChunkMetric);
            Map<Integer, List<DependencyTrackingRO>> map = list.stream().collect(Collectors.groupingBy(DependencyTrackingRO::getSinkId));
            Map<String, Integer> counters = map.entrySet().stream().collect(Collectors.toMap(e -> getSinkName(e.getKey()), e -> e.getValue().size()));
            staleChunks.forEach((k, v) -> v.set(counters.getOrDefault(k, 0)));
            if(!list.isEmpty()) LOGGER.info("Stale chunks alert set for jobs: " + list.stream().map(e -> e.getKey().getJobId()).distinct().map(i -> Integer.toString(i)).collect(Collectors.joining(", ")));
        } catch (RuntimeException e) {
            LOGGER.error("Caught runtime exception un update stale chunks", e);
            throw e;
        }
    }

    /**
     * Re-drives the chunks whose dispatch attempt was fired and never arrived.
     * <p>
     * Both {@code READY_*} statuses mean the same thing: whoever put the chunk here went straight on
     * to dispatch it, so the status is held for the length of one attempt and no longer. An attempt
     * that dies leaves the chunk with nothing else watching it, since the bulk sweeps read only the
     * {@code SCHEDULED_*} statuses, and moving it to its own {@code SCHEDULED_*} status is what hands
     * it to the sweep that does.
     * <p>
     * Validated in both cases, because each target has exactly one legal predecessor, which is the
     * status the query selected on. That makes the write the precise guard against a chunk that
     * moved on between the query and the write: unvalidated, the delivery side would push a chunk
     * the sink already holds back to {@code SCHEDULED_FOR_DELIVERY} and the next sweep would deliver
     * its items a second time.
     * <p>
     * The two timeouts answer different questions and are deliberately not the same number. Five
     * minutes on the delivery side covers a real round trip to a sink. The processing side's attempt
     * is an EJB asynchronous invocation made as the chunk's row commits, so it is milliseconds in
     * health, and its timeout is set by the other risk instead: a large partitioning burst queues
     * those invocations, and a sweep firing while they are still draining hands the same chunks to
     * the bulk submitter, leaving every queued invocation to find its chunk already claimed. Ten
     * minutes sits far above any backlog that queue plausibly holds, and still bounds a stranded
     * chunk to minutes rather than to the hourly sweeps.
     */
    void rescueChunksLeftReady() {
        dependencyTrackingService.getStaleDependencies(READY_FOR_DELIVERY, Duration.ofMinutes(5))
                .forEach(dt -> dependencyTrackingService.setValidatedStatus(dt.getKey(), SCHEDULED_FOR_DELIVERY));
        dependencyTrackingService.getStaleDependencies(READY_FOR_PROCESSING, Duration.ofMinutes(10))
                .forEach(dt -> dependencyTrackingService.setValidatedStatus(dt.getKey(), SCHEDULED_FOR_PROCESSING));
    }

    /**
     * Runs {@link #recheckBlocks} on demand, which the hourly timer otherwise only does at minute 10.
     * <p>
     * Exists for the same reason {@link #gateSweep} does, a recovery mechanism has to be reachable
     * when something is actually stranded. It reaches more than {@link #gateSweep}: the row drop for
     * jobs that are gone or already completed, the barrier lift for each, and the sink status
     * recount, all of which nest transactions inside this one and so are the part with a hang for a
     * failure mode.
     *
     * @return the number of barriers lifted and gates opened by the sweep the recheck ends with
     */
    @POST
    @Path(JobStoreServiceConstants.DEPENDENCY_RECHECK_BLOCKS)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestRecheckBlocks() throws JSONBException {
        recheckBlocks();
        return Response.ok(jsonbContext.marshall(Map.of("recheckCompleted", true))).build();
    }

    @Schedule(minute = "10", hour = "*", persistent = false)
    public void recheckBlocks() {
        if(Hazelcast.isSlave()) return;
        Set<Integer> trackedJobIds = dependencyTrackingService.getAllJobIs();
        for (Integer jobId : trackedJobIds) {
            JobEntity entity = jobStoreRepository.getJobEntityById(jobId);
            if(entity == null || entity.getTimeOfCompletion() != null) {
                dependencyTrackingService.removeJobId(jobId);
                LOGGER.info("Dropped the scheduling rows of job {}, which is gone or already completed", jobId);
                // Dropping the rows takes away the termination row a barrier lift would have fired
                // on, so the lift has to happen here. Without it every later job on that submitter
                // is held behind a barrier nothing can lift, since the re-trigger is edge triggered
                // and the edge has already passed. A job with no barrier is a single no-op update,
                // see JobGateRepository#markTerminationBarrierLifted.
                if (entity != null) {
                    liftBarrierImposedBy(entity);
                }
            }
        }
        // Barriers first, gates second: lifting a barrier is what makes the gates queued behind it
        // openable in the same pass. A job whose entity was already gone above is picked up here,
        // since this reads the scope from the job row rather than from the caller.
        int lifted = jobGateBean.sweepUnliftedBarriers();
        int opened = jobGateBean.sweepClosedGates();
        if (lifted > 0 || opened > 0) {
            LOGGER.info("Hourly gate sweep lifted {} barriers and opened {} gates", lifted, opened);
        }
        recountAndReportDrift();
    }

    /**
     * Replaces the sink chunk counts with a census of the table, and counts what had to be
     * corrected.
     * <p>
     * The counts are maintained from the write sites rather than derived, and the mutation is not
     * transactional, so a transaction that does not commit can leave a count moved and the table
     * not. One scan of a table bounded by in-flight chunks, hourly.
     * <p>
     * What is corrected is worth a metric even though the recount repairs it. A count that says a
     * sink holds nothing parked costs that sink a minute of dispatch, until
     * {@code JobSchedulerBulkSubmitterBean.sweepSinksWithParkedChunks} asks the table instead, and
     * the repair itself says nothing, so this number is the only standing signal that deltas are
     * being lost. {@link DependencyTrackingService#recountSinkStatus} logs which sink and status
     * each was.
     */
    void recountAndReportDrift() {
        int corrected = dependencyTrackingService.recountSinkStatus(Set.of());
        if (corrected > 0) {
            countCorrectedCounters(corrected);
        }
    }

    void countCorrectedCounters(int corrected) {
        metricRegistry.counter(SINK_STATUS_COUNTER_DRIFT).inc(corrected);
    }

    /**
     * Lifts the barrier imposed by a job whose dependency tracking rows have just been removed,
     * and re-triggers the jobs queued behind it.
     * <p>
     * In its own transaction, so this method's advisory lock is released here rather than at the end
     * of the recheck. The gate sweep further down locks the same scopes from a nested transaction,
     * and would wait forever on one this transaction was still holding.
     *
     * @param job job whose rows were removed
     */
    private void liftBarrierImposedBy(JobEntity job) {
        if (job.getCachedSink() == null) {
            return;
        }
        jobGateBean.liftBarrierForRemovedJob(job.getId(), job.getCachedSink().getSink().getId(),
                (int) job.getSpecification().getSubmitterId());
    }

    @Schedule(minute = "15", hour = "*", persistent = false)
    public void completeFinishedJobs() {
        if(Hazelcast.isSlave()) return;
        Instant from = nextJobCheckFrom == null ? Instant.now().minus(Duration.ofHours(2)) : nextJobCheckFrom;
        Instant to = Instant.now().minus(Duration.ofMinutes(1));
        nextJobCheckFrom = to;
        completeFinishedJobs(from, to);
    }

    /**
     * Advances the stale chunks whose phase has already finished, and returns the rest.
     * <p>
     * A chunk sits in a {@code QUEUED_*} status until the completion its phase reports moves it
     * on. That completion writes the chunk's items in one call and moves the scheduling row in
     * another, so a lost second half leaves a chunk whose work is done and whose row still says
     * it is out. Nothing arrives to move it, and re-sending it repeats work already recorded:
     * the job processor runs the chunk again for a result the database refuses as a duplicate,
     * or a sink is handed items it has already taken.
     * <p>
     * The repair is the completion call itself, run again. Both are idempotent by construction,
     * one behind a validated status change and the other behind a delete that returns the row it
     * removed, so a chunk that has since moved on is left alone. Neither sends anything outside
     * job-store, which is what makes this worth attempting on every sweep with no limit, ahead of
     * the bounded resend in {@link #resendIfNeeded}.
     *
     * @param stale the stale chunks this sweep found
     * @return the chunks whose work really is outstanding
     */
    List<DependencyTrackingRO> advanceChunksWhosePhaseFinished(List<DependencyTrackingRO> stale) {
        List<DependencyTrackingRO> outstanding = new ArrayList<>(stale.size());
        List<String> advanced = new ArrayList<>();
        for (DependencyTrackingRO dt : stale) {
            State.Phase finished = finishedPhaseOf(dt);
            if (finished == null) {
                outstanding.add(dt);
                continue;
            }
            advanced.add(dt.getKey().toChunkIdentifier() + " (" + finished + ")");
            if (finished == State.Phase.PROCESSING) {
                jobSchedulerBean.chunkProcessingDone(
                        new Chunk(dt.getKey().getJobId(), dt.getKey().getChunkId(), Chunk.Type.PROCESSED));
            } else {
                jobSchedulerBean.chunkDeliveringDone(
                        new Chunk(dt.getKey().getJobId(), dt.getKey().getChunkId(), Chunk.Type.DELIVERED));
            }
        }
        if (!advanced.isEmpty()) {
            LOGGER.warn("Advanced stale chunks whose phase had already finished: {}",
                    String.join(", ", advanced));
        }
        return outstanding;
    }

    /**
     * Tells which phase a chunk has finished while its scheduling row still waits for it.
     *
     * @param dt stale chunk to examine
     * @return the finished phase, or null if the chunk's work is genuinely outstanding
     */
    private State.Phase finishedPhaseOf(DependencyTrackingRO dt) {
        State.Phase phase = switch (dt.getStatus()) {
            case QUEUED_FOR_PROCESSING -> State.Phase.PROCESSING;
            case QUEUED_FOR_DELIVERY -> State.Phase.DELIVERING;
            default -> null;
        };
        if (phase == null) {
            return null;
        }
        ChunkEntity chunk = entityManager.find(ChunkEntity.class,
                new ChunkEntity.Key(dt.getKey().getChunkId(), dt.getKey().getJobId()));
        if (chunk == null || !chunk.getState().phaseIsDone(phase)) {
            return null;
        }
        return phase;
    }

    public void resendIfNeeded(List<DependencyTrackingRO> list) {
        // The filter picks what to log. What bounds the retries is the retry statement's own
        // WHERE clause, which carries the same limit, so two callers reaching one chunk here
        // still produce one retry between them.
        Set<DependencyTrackingRO> retries = list.stream()
                .filter(de -> de.getRetries() < chunkResendLimit)
                .collect(Collectors.toSet());
        if(retries.isEmpty()) return;
        LOGGER.warn("Retrying stale chunks: {}", retries.stream()
                .map(e -> e.getKey().toChunkIdentifier())
                .collect(Collectors.joining(", ")));

        retries.forEach(dt -> dependencyTrackingService.resend(dt.getKey(), chunkResendLimit));
        Set<Integer> sinks = list.stream().map(DependencyTrackingRO::getSinkId).collect(Collectors.toSet());
        jobSchedulerBean.loadSinkStatusOnBootstrap(sinks);
    }

    /**
     * Reports the stale chunks that have used every retry, once each.
     * <p>
     * A chunk here is one no further sweep will touch, so its job cannot complete and its items
     * are never delivered. That is worth an error rather than another line of the per-minute
     * stale-chunk count, which says only that a sink is behind and is expected while one is down.
     * <p>
     * Reported once per chunk. The record of what has been reported is pruned against the chunks
     * still stale, so it cannot outgrow them, and a chunk that recovers and gets stuck again is
     * reported again.
     *
     * @param stale the stale chunks this sweep found
     * @return how many chunks this sweep reported for the first time
     */
    int reportExhaustedRetries(List<DependencyTrackingRO> stale) {
        Set<TrackingKey> exhausted = stale.stream()
                .filter(de -> de.getRetries() >= chunkResendLimit)
                .map(DependencyTrackingRO::getKey)
                .collect(Collectors.toSet());

        List<DependencyTrackingRO> newlyExhausted = stale.stream()
                .filter(de -> exhausted.contains(de.getKey()))
                .filter(de -> !exhaustedRetriesReported.contains(de.getKey()))
                .toList();
        if (!newlyExhausted.isEmpty()) {
            LOGGER.error("Chunks beyond repair after {} retries, their jobs cannot complete: {}",
                    chunkResendLimit, newlyExhausted.stream()
                            .map(e -> e.getKey().toChunkIdentifier())
                            .collect(Collectors.joining(", ")));
        }
        exhaustedRetriesReported.retainAll(exhausted);
        exhaustedRetriesReported.addAll(exhausted);

        Map<String, Integer> perSink = stale.stream()
                .filter(de -> exhausted.contains(de.getKey()))
                .collect(Collectors.groupingBy(de -> getSinkName(de.getSinkId()),
                        Collectors.summingInt(de -> 1)));
        perSink.keySet().stream()
                .filter(s -> exhaustedRetries.putIfAbsent(s, new AtomicInteger(0)) == null)
                .forEach(this::registerExhaustedRetriesMetric);
        exhaustedRetries.forEach((sinkName, count) -> count.set(perSink.getOrDefault(sinkName, 0)));
        return newlyExhausted.size();
    }

    @SuppressWarnings("unused")
    @Schedule(minute = "5", hour = "*", persistent = false)
    public void cleanStaleJMSConnections() {
        if(Hazelcast.isSlave()) return;
        LOGGER.info("Cleaning stale artemis connections");
        Instant i = Instant.now().minus(Duration.ofMinutes(15));
        adminClient.closeConsumerConnections(c -> i.isAfter(c.getLastAcknowledgedTime()) && c.getDeliveringCount() > 0);
    }

    @GET
    @Path(JobStoreServiceConstants.SINKS_STATUS_RECOUNT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response recountSinkStatus() throws JSONBException {
        dependencyTrackingService.recountSinkStatus(Set.of());
        return Response.ok(jsonbContext.marshall(dependencyTrackingService.getCountersForSinks())).build();
    }

    /**
     * Runs the per-job gate sweep on demand, which {@link #recheckBlocks} otherwise only runs
     * hourly.
     * <p>
     * Opens gates closed behind a barrier that is gone and lifts the barrier of a job whose
     * termination row was removed without one, which are the two ways a job can be left unable to
     * complete with nothing edge triggered left to fire on.
     * <p>
     * Ordered barriers before gates, as {@link #recheckBlocks} orders it, since lifting a barrier is
     * what makes the gates queued behind it openable in the same pass.
     *
     * @return the number of barriers lifted and gates opened
     */
    @POST
    @Path(JobStoreServiceConstants.DEPENDENCY_GATE_SWEEP)
    @Produces(MediaType.APPLICATION_JSON)
    public Response gateSweep() throws JSONBException {
        int lifted = jobGateBean.sweepUnliftedBarriers();
        int opened = jobGateBean.sweepClosedGates();
        LOGGER.info("Requested gate sweep lifted {} barriers and opened {} gates", lifted, opened);
        return Response.ok(jsonbContext.marshall(
                Map.of("barriersLifted", lifted, "gatesOpened", opened))).build();
    }

    @GET
    @Path(JobStoreServiceConstants.DEPENDENCIES)
    public Response dependencies(@PathParam("jobId") int jobId) throws JSONBException {
        List<DependencyTracking> snapshot = dependencyTrackingService.getSnapshot(jobId);
        return Response.ok(jsonbContext.marshall(snapshot)).build();
    }

    @GET
    @Path(JobStoreServiceConstants.CLEAR_HZ)
    @Produces({MediaType.TEXT_PLAIN})
    public Response clearHazelcastCache(@PathParam("name") String cacheName) {
        Map<?, ?> map = Hazelcast.Objects.valueOf(cacheName.toUpperCase()).get();
        map.clear();
        return Response.ok().build();
    }

    @GET
    @Path(JobStoreServiceConstants.CLEAR_CACHE)
    @Produces({MediaType.TEXT_PLAIN})
    public Response clearJobstoreCache() {
        Cache cache = entityManager.getEntityManagerFactory().getCache();
        cache.evictAll();
        LOGGER.info("Evicted jpa cache");
        return Response.ok("ok").build();
    }

    @SuppressWarnings("UnresolvedRestParam")
    @POST
    @Path(JobStoreServiceConstants.FORCE_DEPENDENCY_TRACKING_RETRANSMIT_ID)
    public Response retransmit(@PathParam("jobIds") String jobIds) {
        Set<Integer> ids = Arrays.stream(jobIds.split(" *, *")).map(Integer::valueOf).collect(Collectors.toSet());
        return retransmitJobs(ids);
    }

    @GET
    @Path(JobStoreServiceConstants.CHECK_INCOMPLETE)
    public Response completeFinishedJobs(@PathParam("days") int days) {
        Instant from = LocalDate.now().minusDays(days).atStartOfDay(Constants.ZONE_CPH).toInstant();
        Instant to = Instant.now().minus(Duration.ofMinutes(1)) ;
        completeFinishedJobs(from, to);
        return Response.ok().build();
    }

    private void completeFinishedJobs(Instant from, Instant to) {
        List<JobInfoSnapshot> jobs = jobStoreRepository.listJobs(new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.GREATER_THAN_OR_EQUAL_TO, new Timestamp(from.toEpochMilli())))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.LESS_THAN, new Timestamp(to.toEpochMilli())))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL)));
        for (JobInfoSnapshot job : jobs) {
            JobEntity entity = jobStoreRepository.getJobEntityById(job.getJobId());
            if(entity.getState().phaseIsDone(State.Phase.PARTITIONING)) {
                List<Timestamp> chunks = jobStoreRepository.listTimeOfChunkCompletion(job.getJobId());
                if(job.getNumberOfChunks() <= chunks.size() && chunks.stream().noneMatch(Objects::isNull)) {
                    Arrays.stream(State.Phase.values())
                            .filter(p -> entity.getState().getPhase(p).getEndDate() == null)
                            .forEach(p -> entity.getState().getPhase(p).withEndDate(new Date()));
                    entity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                    LOGGER.info("completeFinishedJobs marked {} as completed, all chunks are accounted for", job.getJobId());
                }
            }
        }
    }

    private Response retransmitJobs(Set<Integer> jobIds) {
        Set<Integer> sinkId = jobIds.stream().map(id -> entityManager.find(JobEntity.class, id))
                .map(JobEntity::getCachedSink)
                .map(SinkCacheEntity::getSink)
                .map(Sink::getId)
                .collect(Collectors.toSet());
        int rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_PROCESSING, SCHEDULED_FOR_PROCESSING);
        LOGGER.info("Reset dependency tracking states. Sets status QUEUED_FOR_PROCESSING -> SCHEDULED_FOR_PROCESSING for {} entities", rowsUpdated);
        rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_DELIVERY, SCHEDULED_FOR_DELIVERY);
        LOGGER.info("Reset dependency tracking states. Sets status QUEUED_FOR_DELIVERY -> SCHEDULED_FOR_DELIVERY for {} entities", rowsUpdated);
        jobSchedulerBean.loadSinkStatusOnBootstrap(sinkId);
        return Response.ok().build();
    }

    private void registerChunkMetric(String sinkName) {
        MetricID metricID = new MetricID("dataio_stale_chunks", new Tag("sink", sinkName));
        LOGGER.info("Registering metric: {}", metricID);
        metricRegistry.gauge(metricID, () -> staleChunks.get(sinkName));
    }

    void registerExhaustedRetriesMetric(String sinkName) {
        MetricID metricID = new MetricID("dataio_chunks_retries_exhausted", new Tag("sink", sinkName));
        LOGGER.info("Registering metric: {}", metricID);
        metricRegistry.gauge(metricID, () -> exhaustedRetries.get(sinkName));
    }

    private String getSinkName(int id) {
        return getSink(id).getContent().getName();
    }

    Sink getSink(int id) {
        Sink sink = sinkMap.getIfPresent(id);
        if(sink == null) {
            sink = getFromFlowstore(id);
            sinkMap.put(id, sink);
        }
        return sink;
    }

    private Sink getFromFlowstore(int id) {
        try {
            return flowstore.getConnector().getSink(id);
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException("Found no sink with id " + id);
        }
    }

    boolean isTimeout(DependencyTrackingRO de) {
        if(de.getLastModified() == null) return false;
        Optional<Sink> sink = Optional.ofNullable(getSink(de.getSinkId()));
        Instant lm = de.getLastModified();
        Instant now = Instant.now();
        return sink.map(Sink::getContent)
                .map(SinkContent::getTimeout)
                .map(Duration::ofHours)
                .map(now::minus)
                .map(lm::isBefore)
                .orElse(false);
    }
}
