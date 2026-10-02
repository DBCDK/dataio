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
import jakarta.annotation.Resource;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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

    /**
     * Used to undo a counter move whose transaction rolls back, see {@link #applyDeltas}. Null
     * outside a container, where there is no transaction to roll back.
     */
    @Resource
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    /**
     * Bumped by every recount, so a pending rollback reversal can tell that the counters it meant
     * to correct have been replaced by a census of the table.
     */
    private final AtomicLong recountGeneration = new AtomicLong();

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

    public DependencyTrackingService withTransactionSynchronizationRegistry(TransactionSynchronizationRegistry registry) {
        this.transactionSynchronizationRegistry = registry;
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
     * <p>
     * <b>No rollback reversal here, unlike every other counter move.</b> Both callers count a row
     * written by a {@code REQUIRES_NEW} insert that has already committed, so the caller's
     * transaction rolling back leaves the row in the table. Reversing the count against that
     * transaction would take the chunk out of the counters while its row sits in the table, which
     * is the drift {@link #applyDeltas} exists to prevent, in the direction that makes a sink look
     * idle. The caller for a data chunk is {@code JobSchedulerBean.scheduleChunk}, inside the one
     * transaction partitioning holds for a whole job.
     *
     * @param sinkId sink the chunk is destined for
     * @param status status the chunk was inserted with
     */
    public void countInsertedChunk(int sinkId, ChunkSchedulingStatus status) {
        moveCounters(sinkId, Map.of(status, 1));
    }

    /**
     * Moves a sink's counters, and undoes the move if the transaction that asked for it rolls back.
     * <p>
     * The move is applied at once rather than on commit. {@code capacity} is read by concurrent
     * dispatchers in the window between a chunk being counted and its transaction committing, and a
     * delta they cannot see is a queue slot handed out twice.
     * <p>
     * The counters are a Hazelcast map and take no part in the JTA transaction, so a rollback
     * leaves the counter moved and the table not unless something puts it back. That is what the
     * synchronisation does, and it is why {@link #recountSinkStatus} finds less to correct.
     * Outside a transaction there is nothing to roll back and the move simply stands.
     * <p>
     * Only for a move whose row is written in the caller's own transaction. A row already
     * committed by a nested transaction is counted with {@link #moveCounters}, see
     * {@link #countInsertedChunk}.
     *
     * @param sinkId sink whose counters move
     * @param deltas change per status
     */
    private void applyDeltas(int sinkId, Map<ChunkSchedulingStatus, Integer> deltas) {
        moveCounters(sinkId, deltas);
        reverseOnRollback(sinkId, deltas);
    }

    /**
     * Moves a sink's counters, with no reversal registered.
     * <p>
     * The entry is created first because {@link UpdateCounter} leaves a key it does not find alone,
     * so a delta for a sink absent from the map would otherwise be dropped. A sink is absent
     * whenever it has no rows, which is every sink immediately after a full recount.
     *
     * @param sinkId sink whose counters move
     * @param deltas change per status
     */
    private void moveCounters(int sinkId, Map<ChunkSchedulingStatus, Integer> deltas) {
        countersMap.putIfAbsent(sinkId, new EnumMap<>(ChunkSchedulingStatus.class));
        countersMap.executeOnKey(sinkId, new UpdateCounter(deltas));
    }

    /**
     * Registers the reversal of a counter move against the current transaction.
     * <p>
     * The reversal stands down if the counters have been recounted in the meantime, since a census
     * of the table already accounts for whatever the transaction did or did not commit, and
     * subtracting from it would corrupt a number that is correct. {@code AdminBean.resendIfNeeded}
     * reaches both in one transaction: it moves counters through {@code resend} and then recounts
     * through {@code loadSinkStatusOnBootstrap}.
     *
     * @param sinkId sink whose counters moved
     * @param deltas change to undo if the transaction does not commit
     */
    private void reverseOnRollback(int sinkId, Map<ChunkSchedulingStatus, Integer> deltas) {
        if (transactionSynchronizationRegistry == null
                || transactionSynchronizationRegistry.getTransactionKey() == null) {
            return;
        }
        EnumMap<ChunkSchedulingStatus, Integer> reversed = new EnumMap<>(ChunkSchedulingStatus.class);
        deltas.forEach((status, delta) -> reversed.put(status, -delta));
        long countedAt = recountGeneration.get();
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {
                if (status == Status.STATUS_COMMITTED) {
                    return;
                }
                if (recountGeneration.get() != countedAt) {
                    LOGGER.info("Left the counters of sink {} alone, recounted since the move", sinkId);
                    return;
                }
                moveCounters(sinkId, reversed);
                LOGGER.warn("Undid the counter move of a transaction that did not commit, sink {}: {}",
                        sinkId, reversed);
            }
        });
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
            applyDeltas(sinkId, deltas);
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

    /**
     * Replaces the sink chunk counts with a census of the table, and says how far they had drifted.
     *
     * @param sinkIds sinks to recount, empty for every sink
     * @return how many sink and status pairs the counters had wrong
     */
    public int recountSinkStatus(Set<Integer> sinkIds) {
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> counts = repository.countByStatus(sinkIds);
        int disagreements = reportDisagreements(sinkIds, counts);
        if(sinkIds.isEmpty()) countersMap.clear();
        else sinkIds.forEach(countersMap::remove);
        countersMap.putAll(counts);
        recountGeneration.incrementAndGet();
        LOGGER.info("Completed status map recount for {}", sinkIds);
        return disagreements;
    }

    /**
     * Names where the counters and the table disagree, before the recount overwrites the evidence.
     * <p>
     * A counter is maintained from the write sites, so a disagreement means a delta was lost or
     * counted twice. The recount corrects the number either way and says nothing further about it,
     * so this is the only place a lost delta leaves a trace.
     * <p>
     * A transaction that has moved a chunk and not yet committed disagrees legitimately and is
     * reported all the same, since the two are not distinguishable from here. A recount is hourly
     * or operator driven, so the noise that costs is bounded.
     *
     * @param sinkIds sinks being recounted, empty for every sink
     * @param counted the table's census, per sink and status
     * @return how many sink and status pairs disagreed
     */
    private int reportDisagreements(Set<Integer> sinkIds, Map<Integer, Map<ChunkSchedulingStatus, Integer>> counted) {
        Set<Integer> sinks = new HashSet<>(counted.keySet());
        sinks.addAll(sinkIds.isEmpty() ? countersMap.keySet() : sinkIds);
        List<String> differences = new ArrayList<>();
        for (Integer sinkId : sinks) {
            Map<ChunkSchedulingStatus, Integer> held =
                    Optional.ofNullable(countersMap.get(sinkId)).orElseGet(Map::of);
            Map<ChunkSchedulingStatus, Integer> found = counted.getOrDefault(sinkId, Map.of());
            for (ChunkSchedulingStatus status : ChunkSchedulingStatus.values()) {
                int wasHeld = held.getOrDefault(status, 0);
                int wasFound = found.getOrDefault(status, 0);
                if (wasHeld != wasFound) {
                    differences.add(sinkId + " " + status + " held " + wasHeld + " found " + wasFound);
                }
            }
        }
        if (!differences.isEmpty()) {
            LOGGER.warn("Sink chunk counts disagreed with the table and are being replaced: {}",
                    String.join(", ", differences));
        }
        return differences.size();
    }

    /**
     * Names the sinks holding chunks in a status, from the table rather than the counters.
     * <p>
     * What {@link #getActiveSinks} answers from a cache, this answers from the rows. See
     * {@link DependencyTrackingRepository#distinctSinkIdsWithStatus} for why the caller asks
     * sparingly.
     *
     * @param status status to look for
     * @return every sink with at least one chunk in that status
     */
    public Set<Integer> findSinksWithChunksIn(ChunkSchedulingStatus status) {
        return repository.distinctSinkIdsWithStatus(status);
    }

    /**
     * Names the sinks the counters say hold chunks in a status.
     * <p>
     * Read on the dispatch tick, so it reads the counters rather than the table. The counters can
     * lose a delta, and a sink missing from the answer is a sink nothing dispatches for, so the
     * bulk submitter also asks {@link #findSinksWithChunksIn} on a slower schedule.
     *
     * @param status status to look for
     * @return every sink the counters say has at least one chunk in that status
     */
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
        applyDeltas(removed.getSinkId(), Map.of(removed.getStatus(), -1));
    }

    /**
     * Applies a status change's counter delta, always after the statement that caused it and never
     * before, so a statement that affected nothing moves no counter.
     */
    private Optional<StatusChangeEvent> applyCounters(Optional<StatusChangeEvent> change) {
        change.ifPresent(event -> {
            EnumMap<ChunkSchedulingStatus, Integer> deltas = new EnumMap<>(ChunkSchedulingStatus.class);
            event.apply(deltas);
            applyDeltas(event.getSinkId(), deltas);
        });
        return change;
    }
}
