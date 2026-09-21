package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.map.IMap;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateCounter;
import dk.dbc.dataio.jobstore.service.ejb.DependencyTrackingRepository;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The scheduler's view of chunk scheduling state, and the sink chunk counts the queue caps are read
 * from.
 * <p>
 * State lives in {@code dependencytracking} and is read and written through
 * {@link DependencyTrackingRepository}, in the caller's own transaction. This class adds the two
 * things that sit outside a single statement: the {@code SINK_STATUS} counters, which are a
 * distributed Hazelcast map so that a cap applies across job-store instances rather than per JVM,
 * and the readiness check.
 * <p>
 * <b>{@code @Lock(READ)} is not cosmetic.</b> A singleton's default write lock serialises every
 * business method one at a time per JVM, which was tolerable while these were microsecond map
 * operations and is not now that each is a database round trip. Sound here because this class holds
 * no mutable state of its own: the counters live in Hazelcast and the rows live in PostgreSQL.
 * <p>
 * The counters are maintained from the write sites rather than derived, so a rolled back
 * transaction can leave one off by one. They are backpressure rather than correctness, so drift
 * costs throughput, and {@link #recountSinkStatus} corrects it: at startup, hourly from
 * {@code AdminBean.recheckBlocks}, and on demand from the {@code SINKS_STATUS_RECOUNT} endpoint.
 */
@Singleton
@Startup
@DependsOn("DatabaseMigrator")
@Lock(LockType.READ)
public class DependencyTrackingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTrackingService.class);
    private final IMap<Integer, Map<ChunkSchedulingStatus, Integer>> countersMap = Hazelcast.Objects.SINK_STATUS.get();

    @EJB
    DependencyTrackingRepository repository;

    @PostConstruct
    public void config() {
        init();
    }

    public DependencyTrackingService init() {
        recountSinkStatus(Set.of());
        return this;
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Commence Hazelcast node shutdown");
        Hazelcast.shutdownNode();
        LOGGER.info("Hazelcast node shutdown completed");
    }

    public DependencyTrackingService withRepository(DependencyTrackingRepository repository) {
        this.repository = repository;
        return this;
    }

    /**
     * Timed here rather than on the repository, unlike every other method on this class. It reads
     * the Hazelcast counters and never reaches the database, so no repository timer covers it, and
     * it is on the direct dispatch path once per chunk.
     */
    @Timed
    public int capacity(int sinkId, ChunkSchedulingStatus status) {
        if(status.getMax() == null) throw new IllegalArgumentException("This status does not have a capacity");
        return status.getMax() - getCount(sinkId, status);
    }

    /**
     * Counts a row that has just been created against the sink's chunk counts.
     * <p>
     * <b>Every chunk is counted, gated or not.</b> {@code JobSchedulerBean.scheduleChunk} reaches
     * this for every data chunk it creates a row for, whatever that row's {@code gate_open}, and
     * {@code createAndScheduleTerminationChunk} for every termination chunk.
     * <p>
     * The gate has no bearing on it, because these counters are a census of rows by status and the
     * gate is a column rather than a status. {@link #recountSinkStatus} recomputes that same census
     * with {@code count(*) group by sinkid, status} and no gate predicate, so a gated row left
     * uncounted here would put the incremental counters and the recount permanently at odds, and
     * the hourly recount would move a counter rather than confirm it.
     * <p>
     * No cap is involved at this point either way. A data chunk is inserted in
     * {@code READY_FOR_PROCESSING} and a termination chunk in {@code READY_FOR_DELIVERY}, and only
     * the two {@code QUEUED_*} statuses carry one. A gated chunk does go on to consume a capped
     * processing slot, since a gate holds back delivery and not processing, but it never reaches
     * {@code QUEUED_FOR_DELIVERY} at all: every dispatch path refuses a closed gate.
     * <p>
     * The insert itself is not done here, and the reason is the transaction boundary rather than the
     * gate. A row has to carry its gate verdict from the moment it exists, so the insert has to
     * happen where that verdict is decided: {@code JobGateBean.insertDataChunkRow}, in its own
     * {@code REQUIRES_NEW} transaction, and
     * {@code PgJobStoreRepository.createJobTerminationChunkEntity}, under the job row lock. What is
     * left for this class is the counter.
     *
     * @param sinkId sink the chunk is destined for
     * @param status status the chunk was inserted with
     */
    public void countInsertedChunk(int sinkId, ChunkSchedulingStatus status) {
        countersMap.putIfAbsent(sinkId, new EnumMap<>(ChunkSchedulingStatus.class));
        countersMap.executeOnKey(sinkId, new UpdateCounter(status, 1));
    }

    public List<DependencyTracking> getSnapshot(int jobId) {
        return repository.findByJob(jobId);
    }

    public List<DependencyTrackingRO> getStaleDependencies(ChunkSchedulingStatus status, Duration timeout) {
        return repository.findStale(status, Instant.now().minus(timeout)).stream()
                .map(DependencyTrackingRO.class::cast)
                .toList();
    }

    public DependencyTrackingRO get(TrackingKey key) {
        return repository.get(key).orElse(null);
    }

    public boolean contains(TrackingKey key) {
        return repository.exists(key);
    }

    public int resetStatus(ChunkSchedulingStatus from, ChunkSchedulingStatus to, Collection<Integer> jobIds) {
        return repository.resetStatus(from, to, jobIds);
    }

    /**
     * Drops every row of a job.
     * <p>
     * <b>{@code REQUIRES_NEW} is essential. Do not remove it.</b> The delete takes a row lock on
     * every row of the job, closed gates included, and holds it until the transaction it runs in
     * commits. {@code AdminBean.recheckBlocks} calls this once per finished job and then, in the
     * same transaction, nests the barrier lift and the gate sweep, both {@code REQUIRES_NEW}. Those
     * run on a different connection and ask for rows this delete has removed but not committed, so
     * they wait for a transaction that cannot commit until they return. PostgreSQL sees no cycle,
     * because one side of it is an EJB call rather than a database lock, so it is an undetectable
     * hang rather than a detected deadlock, and the hourly recheck is the one thing that must not
     * stop running.
     * <p>
     * Committing per job loses nothing. Jobs share no invariant here, so a recheck that dies
     * halfway leaves the jobs it finished correctly removed rather than rolling all of them back.
     * It also narrows the deadlock window {@code JobsBean.abortJob} accepts against a concurrent
     * gate sweep, since the abort then holds only the rows its own re-trigger writes.
     *
     * @param jobId job whose rows are dropped
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeJobId(int jobId) {
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> removed = repository.deleteByJob(jobId);
        removed.forEach((sinkId, counts) -> {
            EnumMap<ChunkSchedulingStatus, Integer> deltas = new EnumMap<>(ChunkSchedulingStatus.class);
            counts.forEach((status, count) -> deltas.put(status, -count));
            countersMap.executeOnKey(sinkId, new UpdateCounter(deltas));
        });
        LOGGER.info("Removed every tracked chunk of job {}", jobId);
    }

    public Optional<StatusChangeEvent> setStatus(TrackingKey key, ChunkSchedulingStatus status) {
        return applyCounters(repository.updateStatus(key, status));
    }

    public Optional<StatusChangeEvent> setValidatedStatus(TrackingKey key, ChunkSchedulingStatus status) {
        return applyCounters(repository.updateStatusValidated(key, status));
    }

    /**
     * Sends a stale chunk to its queue again, up to the given number of times.
     *
     * @param key        chunk to send again
     * @param retryLimit how many times one chunk may be sent again
     * @return what changed, or empty if the chunk had used its retries or held a status with no
     * successor
     */
    public Optional<StatusChangeEvent> resend(TrackingKey key, int retryLimit) {
        return applyCounters(repository.resend(key, retryLimit));
    }

    /**
     * Removes a delivered chunk's row and hands it to the caller that removed it.
     * <p>
     * The returned row is the once-only token for that chunk's delivery, so the caller that gets it
     * is the one that counts the chunk against its job's gate. See
     * {@link DependencyTrackingRepository#delete(TrackingKey, ChunkSchedulingStatus)}.
     *
     * @param key chunk whose delivery has been acknowledged
     * @return the removed row, or empty if the chunk was not out for delivery or another caller
     * acknowledged it first
     */
    public Optional<DependencyTracking> acknowledgeDelivery(TrackingKey key) {
        Optional<DependencyTracking> removed = repository.delete(key, ChunkSchedulingStatus.QUEUED_FOR_DELIVERY);
        removed.ifPresent(this::countRemovedChunk);
        return removed;
    }

    /**
     * Drops a chunk that is not going to be delivered at all.
     *
     * @param key chunk to drop
     */
    public void remove(TrackingKey key) {
        repository.delete(key).ifPresent(removed -> {
            countRemovedChunk(removed);
            LOGGER.info("Removed tracking key {} from dependency tracking", key.toChunkIdentifier());
        });
    }

    public void recountSinkStatus(Set<Integer> sinkIds) {
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> counts = repository.countByStatus(sinkIds);
        if(sinkIds.isEmpty()) countersMap.clear();
        else sinkIds.forEach(countersMap::remove);
        countersMap.putAll(counts);
        LOGGER.info("Completed status map recount for {}", sinkIds);
    }

    public Set<Integer> getActiveSinks(ChunkSchedulingStatus status) {
        return countersMap.entrySet().stream()
                .filter(e -> e.getValue().get(status) != null && e.getValue().get(status) > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Map<Integer, Map<ChunkSchedulingStatus, Integer>> getCountersForSinks() {
        return countersMap.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Collections.unmodifiableMap(e.getValue())));
    }

    public Integer getCount(int sinkId, ChunkSchedulingStatus cs) {
        return Optional.ofNullable(countersMap.get(sinkId)).map(m -> m.get(cs)).orElse(0);
    }

    public List<DependencyTrackingRepository.ProcessingCandidate> findProcessingCandidates(int sinkId, int limit) {
        return repository.findProcessingCandidates(sinkId, limit);
    }

    /**
     * Ascertains if a chunk is currently scheduled
     *
     * @param chunkEntity chunk entity representing the chunk
     * @return true if scheduled, false if not
     */
    public boolean isScheduled(ChunkEntity chunkEntity) {
        return repository.exists(new TrackingKey(chunkEntity.getKey().getJobId(), chunkEntity.getKey().getId()));
    }

    public Set<Integer> getAllJobIs() {
        return repository.distinctJobIds();
    }

    public Integer[] jobCount(int sinkId) {
        return repository.countJobsAndChunks(sinkId);
    }

    @Readiness
    public HealthCheck readyCheck() {
        return () -> HealthCheckResponse.named("hazelcast-ready").status(Hazelcast.isReady()).build();
    }

    private void countRemovedChunk(DependencyTracking removed) {
        countersMap.executeOnKey(removed.getSinkId(), new UpdateCounter(removed.getStatus(), -1));
    }

    /**
     * Applies a status change's counter delta, always after the statement that caused it and never
     * before, so a statement that affected nothing moves no counter.
     */
    private Optional<StatusChangeEvent> applyCounters(Optional<StatusChangeEvent> change) {
        change.ifPresent(event -> {
            EnumMap<ChunkSchedulingStatus, Integer> deltas = new EnumMap<>(ChunkSchedulingStatus.class);
            event.apply(deltas);
            countersMap.executeOnKey(event.getSinkId(), new UpdateCounter(deltas));
        });
        return change;
    }
}
