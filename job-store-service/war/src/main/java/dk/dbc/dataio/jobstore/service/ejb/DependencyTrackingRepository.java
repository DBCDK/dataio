package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Every statement against {@code dependencytracking} that the scheduler runs.
 * <p>
 * The table is the sole store of a chunk's scheduling state. A row is inserted when the chunk is
 * scheduled, its {@code status} is advanced by the statements here, and it is deleted when the
 * chunk's delivery is acknowledged. Nothing caches it, so a read here is the current state and a
 * write here takes effect when the caller's transaction commits.
 * <p>
 * Three statements carry the concurrency this class is responsible for, and each does so in one
 * statement rather than in a read followed by a write:
 * <ul>
 * <li>{@link #updateStatusValidated} moves a chunk only from a status it may legally move from, so
 *     of two callers racing to advance one chunk exactly one succeeds.</li>
 * <li>{@link #delete} removes a chunk only from {@code QUEUED_FOR_DELIVERY} and hands the removed
 *     row back, which makes the returned row the once-only token for that chunk's delivery.</li>
 * <li>{@link #resend} couples the status change and the retry increment, so a chunk cannot be sent
 *     again twice.</li>
 * </ul>
 * Everything here is a native query on the caller's {@link EntityManager}, so it runs in the
 * caller's transaction and on its connection, matching {@link JobGateRepository} and
 * {@link DeliveryDispatchRepository}. Native rather than JPA because {@code dependencytracking} has
 * no entity: the row's fields are read into a detached {@link DependencyTracking} and the
 * statements are single writes over columns rather than object graph changes.
 * <p>
 * See job-store-service/dependency-tracking.md.
 */
@Stateless
public class DependencyTrackingRepository extends RepositoryBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTrackingRepository.class);

    /** Read in the order {@link #fromRow} expects them. */
    private static final String COLUMNS =
            "jobid, chunkid, sinkid, status, priority, submitter, lastmodified, retries, is_termination, gate_open";

    public DependencyTrackingRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * Creates a chunk's row, unless the chunk already has one.
     * <p>
     * {@code ON CONFLICT DO NOTHING} rather than a plain insert, because
     * {@code JobSchedulerBean.ensureLastChunkIsScheduled} re-enters {@code scheduleChunk} for a
     * chunk that may already be scheduled. Leaving the statement idempotent makes the existence
     * check in front of it advisory rather than necessary.
     * <p>
     * {@code gate_open} is passed rather than defaulted, so a chunk held back by an earlier job's
     * barrier is closed by the same statement that creates its row. There is then no instant in
     * which the row exists with a gate that should be shut, see
     * {@link JobGateBean#insertDataChunkRow}.
     *
     * @param key       chunk to create the row for
     * @param sinkId    sink the chunk is destined for
     * @param submitter submitter the chunk's job belongs to
     * @param status    status the chunk starts in
     * @param priority  the chunk's dispatch priority
     * @param gateOpen  false only if the chunk may not be dispatched yet
     * @return 1 if the row was created, 0 if the chunk already had one
     */
    @Timed
    public int insert(TrackingKey key, int sinkId, int submitter, ChunkSchedulingStatus status,
                      int priority, boolean gateOpen) {
        return entityManager.createNativeQuery(
                        "INSERT INTO dependencytracking " +
                                "       (jobid, chunkid, sinkid, status, priority, submitter, gate_open) " +
                                "VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7) " +
                                "ON CONFLICT ON CONSTRAINT dependencytracking_pkey DO NOTHING")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .setParameter(3, sinkId)
                .setParameter(4, status.value)
                .setParameter(5, priority)
                .setParameter(6, submitter)
                .setParameter(7, gateOpen)
                .executeUpdate();
    }

    /**
     * Overwrites a chunk's status without checking if {@link ChunkSchedulingStatus} allows the change.
     * <p>
     * For a move the caller means to make anyway, which is how a failed send parks a chunk back in
     * {@code SCHEDULED_FOR_DELIVERY} from {@code QUEUED_FOR_DELIVERY}, a step backwards
     * {@link ChunkSchedulingStatus} deliberately does not allow. Use
     * {@link #updateStatusValidated} for anything that is a decision rather than a correction.
     *
     * @param key    chunk to move
     * @param status status to move it to
     * @return what changed, or empty if the chunk has no row
     */
    @Timed
    public Optional<StatusChangeEvent> updateStatus(TrackingKey key, ChunkSchedulingStatus status) {
        return updateStatus(key, status, "");
    }

    /**
     * Sets a chunk's status only if {@link ChunkSchedulingStatus} allows the change from where the
     * chunk stands.
     * <p>
     * <b>Of two callers racing to make the same change, exactly one is told it made it.</b> That is
     * the guarantee callers are entitled to rely on, and several do: {@code chunkProcessingDone}
     * rejects a chunk that has already moved on, and the two dispatch paths use it to claim a chunk
     * so that only one of them sends it. They run concurrently and on separate instances.
     * <p>
     * A target no status may legally reach is declined rather than attempted.
     * {@code READY_FOR_PROCESSING} is the case: a chunk is inserted in it and never returns to it.
     *
     * @param key    chunk to move
     * @param status status to move it to
     * @return what changed, or empty if the chunk was not somewhere it may move to this from, so
     * another caller had already moved it
     */
    @Timed
    public Optional<StatusChangeEvent> updateStatusValidated(TrackingKey key, ChunkSchedulingStatus status) {
        Set<ChunkSchedulingStatus> predecessors = status.getValidPredecessors();
        if (predecessors.isEmpty()) {
            // Answered here rather than in SQL because an empty set renders as "IN ()", which is a
            // syntax error and would surface as a PSQLException on the scheduling path rather than
            // as the rejection the caller expects.
            return Optional.empty();
        }
        return updateStatus(key, status, " AND d.status IN (" + valueList(predecessors) + ")");
    }

    /**
     * Runs the status change, with the predecessor test supplied by
     * {@link #updateStatusValidated} and left out by
     * {@link #updateStatus(TrackingKey, ChunkSchedulingStatus)}.
     * <p>
     * <b>The predecessor test sits on the target row and must never move into the CTE.</b> Under
     * READ COMMITTED an {@code UPDATE} that finds its row already changed by a committed
     * transaction re-fetches the newest version and re-evaluates the statement's qualification
     * against it, so a chunk that has moved out of the set is skipped and nothing is affected. That
     * is what makes {@link #updateStatusValidated} a decision rather than an overwrite. A CTE is
     * evaluated once against the transaction's snapshot, so the same test there would be frozen at
     * the value both racers read and both would succeed, which is the check-then-act the caller is
     * promised it is not getting.
     * <p>
     * The CTE is there for one thing only, the status the chunk held before the change, which
     * {@code UPDATE ... RETURNING} cannot give because it returns post-update values. That value
     * feeds the sink counters and nothing else, so a stale read costs one counter until the next
     * recount rather than a wrong decision. {@code MATERIALIZED} keeps it from being inlined into a
     * self-join whose behaviour under the re-evaluation above is harder to reason about than it is
     * worth.
     *
     * @param predicate the {@code AND d.status IN (...)} test, or empty for an unconditional change
     */
    private Optional<StatusChangeEvent> updateStatus(TrackingKey key, ChunkSchedulingStatus status, String predicate) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "WITH prev AS MATERIALIZED (" +
                                "  SELECT status FROM dependencytracking WHERE jobid = ?1 AND chunkid = ?2) " +
                                "UPDATE dependencytracking d SET status = ?3, lastmodified = now()" +
                                retriesReset(status) +
                                "  FROM prev " +
                                " WHERE d.jobid = ?1 AND d.chunkid = ?2" + predicate +
                                " RETURNING d.sinkid, prev.status")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .setParameter(3, status.value)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new StatusChangeEvent(intOf(row[0]),
                ChunkSchedulingStatus.from(intOf(row[1])), status));
    }

    /**
     * Removes a delivered chunk's row and hands it to the caller that removed it.
     * <p>
     * The returned row is the once-only token for that chunk's delivery. Whatever number of callers
     * acknowledge one chunk, the row matches for exactly one of them, so a caller that must act
     * once per chunk asks the question here rather than by reading the row first, which two callers
     * can both pass. {@code JobSchedulerBean.chunkDeliveringDone} counts a delivered data chunk
     * against its job's gate on that basis, in this same transaction.
     *
     * @param key    chunk to remove
     * @param status status the chunk has to be in, so an acknowledgement of a chunk that is not
     *               out for delivery removes nothing
     * @return the removed row, or empty if this call did not remove it
     */
    @Timed
    public Optional<DependencyTracking> delete(TrackingKey key, ChunkSchedulingStatus status) {
        return delete(key, " AND status = " + status.value);
    }

    /**
     * Removes a chunk's row regardless of its status.
     * <p>
     * For the one path that drops a chunk rather than acknowledging it, where the bulk delivery
     * sweep finds a candidate with no items to send.
     *
     * @param key chunk to remove
     * @return the removed row, or empty if the chunk had none
     */
    @Timed
    public Optional<DependencyTracking> delete(TrackingKey key) {
        return delete(key, "");
    }

    private Optional<DependencyTracking> delete(TrackingKey key, String predicate) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "DELETE FROM dependencytracking " +
                                " WHERE jobid = ?1 AND chunkid = ?2" + predicate +
                                " RETURNING sinkid, submitter, priority, is_termination, status, gate_open")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new DependencyTracking(key, intOf(row[0]), intOf(row[1]))
                .setPriority(intOf(row[2]))
                .setTermination((Boolean) row[3])
                .setStatus(ChunkSchedulingStatus.from(intOf(row[4])))
                .setGateOpen((Boolean) row[5]));
    }

    /**
     * Removes every row of a job, whatever status or gate state each is in.
     * <p>
     * Reached when a job is aborted and when the hourly recheck finds a job that has completed, so
     * the rows removed are not the delivery path's and their gates may well be closed. That makes
     * this the one writer whose row set overlaps the gate sweep's, which is why its caller commits
     * it on its own, see {@code DependencyTrackingService.removeJobId}.
     *
     * @param jobId job whose rows are removed
     * @return the removed rows as a count per sink and status, so the caller can correct its
     * counters without a full recount
     */
    @Timed
    public Map<Integer, Map<ChunkSchedulingStatus, Integer>> deleteByJob(int jobId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "DELETE FROM dependencytracking WHERE jobid = ?1 RETURNING sinkid, status")
                .setParameter(1, jobId)
                .getResultList();
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> removed = new HashMap<>();
        for (Object[] row : rows) {
            ChunkSchedulingStatus status = ChunkSchedulingStatus.from(intOf(row[1]));
            if (status == null) {
                continue;
            }
            removed.computeIfAbsent(intOf(row[0]), s -> new EnumMap<>(ChunkSchedulingStatus.class))
                    .merge(status, 1, Integer::sum);
        }
        return removed;
    }

    /**
     * @param key chunk to look up
     * @return the chunk's row, or empty if it has none
     */
    @Timed
    public Optional<DependencyTracking> get(TrackingKey key) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT " + COLUMNS + " FROM dependencytracking WHERE jobid = ?1 AND chunkid = ?2")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .getResultList();
        return rows.stream().findFirst().map(DependencyTrackingRepository::fromRow);
    }

    /**
     * @param key chunk to ask about
     * @return true if the chunk has a row, so is currently scheduled
     */
    @Timed
    public boolean exists(TrackingKey key) {
        return !entityManager.createNativeQuery(
                        "SELECT 1 FROM dependencytracking WHERE jobid = ?1 AND chunkid = ?2")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .getResultList()
                .isEmpty();
    }

    /**
     * @param jobId job to read
     * @return every row of the job in ascending chunk id
     */
    public List<DependencyTracking> findByJob(int jobId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT " + COLUMNS + " FROM dependencytracking WHERE jobid = ?1 ORDER BY chunkid")
                .setParameter(1, jobId)
                .getResultList();
        return rows.stream().map(DependencyTrackingRepository::fromRow).toList();
    }

    /**
     * Chunks that have held a status longer than they plausibly should.
     * <p>
     * <b>Deliberately unindexed, and therefore a sequential scan.</b> No index leads with
     * {@code status}, and adding one would put a fourth entry on a write path where every status
     * change is already a non-HOT update writing to every index on the table, see
     * job-store-service/dependency-tracking.md under "Write volume". Four scans a minute of a
     * table bounded by in-flight chunks is the cheaper side of that trade. Scoping the query per
     * sink would let the ordered indexes serve it, and is the thing to reach for if the scan ever
     * shows up in {@code pg_stat_statements}.
     *
     * @param status status to scan
     * @param before rows last modified before this instant are stale
     * @return the stale rows
     */
    @Timed
    public List<DependencyTracking> findStale(ChunkSchedulingStatus status, Instant before) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT " + COLUMNS + " FROM dependencytracking " +
                                " WHERE status = ?1 AND lastmodified < ?2")
                .setParameter(1, status.value)
                .setParameter(2, Timestamp.from(before))
                .getResultList();
        return rows.stream().map(DependencyTrackingRepository::fromRow).toList();
    }

    /**
     * Chunks awaiting processing for a sink, in dispatch order.
     * <p>
     * Index-backed by {@code dependencytracking_processing_order_index},
     * {@code (sinkid, status, priority desc, jobid, chunkid)}, where the first two are equality
     * predicates and the rest is exactly this {@code ORDER BY}.
     * <p>
     * <b>{@code gate_open} takes no part, deliberately.</b> A gate holds back delivery only. Under
     * the full barrier width a queued job's data chunks sit at {@code gate_open = FALSE} while
     * still needing to be processed, so a gate predicate here would stop the processing of exactly
     * the chunks the barrier assumes get processed. That is also why this cannot share a statement
     * with {@link DeliveryDispatchRepository#findDeliveryCandidates}, whose index carries
     * {@code gate_open} as its third key column.
     *
     * @param sinkId sink to schedule for
     * @param limit  maximum number of candidates to return
     * @return candidates, highest priority first and lowest job then chunk id within a priority
     */
    @Timed
    public List<ProcessingCandidate> findProcessingCandidates(int sinkId, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT jobid, chunkid, priority FROM dependencytracking " +
                                " WHERE sinkid = ?1 AND status = ?2 " +
                                " ORDER BY priority DESC, jobid, chunkid " +
                                " LIMIT ?3")
                .setParameter(1, sinkId)
                .setParameter(2, ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING.value)
                .setParameter(3, limit)
                .getResultList();
        return rows.stream()
                .map(row -> new ProcessingCandidate(
                        new TrackingKey(intOf(row[0]), intOf(row[1])), intOf(row[2])))
                .toList();
    }

    /**
     * Moves every chunk of the given jobs out of one status, for the retransmit endpoint.
     *
     * @param from   status to move chunks out of
     * @param to     status to move them to
     * @param jobIds jobs to consider
     * @return the number of chunks moved
     */
    public int resetStatus(ChunkSchedulingStatus from, ChunkSchedulingStatus to, Collection<Integer> jobIds) {
        if (jobIds.isEmpty()) {
            return 0;
        }
        Query query = entityManager.createNativeQuery(
                        "UPDATE dependencytracking SET status = ?1, lastmodified = now() " +
                                " WHERE status = ?2 AND jobid IN (" + placeholders(jobIds.size(), 3) + ")")
                .setParameter(1, to.value)
                .setParameter(2, from.value);
        int position = 3;
        for (Integer jobId : jobIds) {
            query.setParameter(position++, jobId);
        }
        return query.executeUpdate();
    }

    /**
     * Sends a stale chunk again, up to the given number of times.
     * <p>
     * The status change and the retry increment are one statement, so two callers reaching one
     * chunk produce one retry between them: the second finds {@code retries} already counted and
     * matches nothing. The successor is per status rather than constant, and is generated from
     * {@link ChunkSchedulingStatus#resend} rather than written out, so the two cannot drift.
     * <p>
     * The limit is a parameter because it is configuration, which a repository does not read, and
     * because the caller filters on the same number to decide what to log. Both readings have to
     * be of one value.
     *
     * @param key        chunk to send again
     * @param retryLimit how many times one chunk may be sent again
     * @return what changed, or empty if the chunk had used its retries or held a status with no
     * successor
     */
    @Timed
    public Optional<StatusChangeEvent> resend(TrackingKey key, int retryLimit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "WITH prev AS MATERIALIZED (" +
                                "  SELECT status FROM dependencytracking WHERE jobid = ?1 AND chunkid = ?2) " +
                                "UPDATE dependencytracking d " +
                                "   SET status = CASE d.status " + resendCases() + " END, " +
                                "       retries = d.retries + 1, " +
                                "       lastmodified = now() " +
                                "  FROM prev " +
                                " WHERE d.jobid = ?1 AND d.chunkid = ?2 " +
                                "   AND d.retries < ?3 " +
                                "   AND d.status IN (" + valueList(resendable()) + ") " +
                                " RETURNING d.sinkid, prev.status, d.status")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .setParameter(3, retryLimit)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new StatusChangeEvent(intOf(row[0]),
                ChunkSchedulingStatus.from(intOf(row[1])), ChunkSchedulingStatus.from(intOf(row[2]))));
    }

    /**
     * The chunk counts the queue caps are read from, counted from the table itself.
     *
     * @param sinkIds sinks to count, or empty for every sink with rows
     * @return one status count map per sink, with statuses no chunk holds left out
     */
    @Timed
    public Map<Integer, Map<ChunkSchedulingStatus, Integer>> countByStatus(Set<Integer> sinkIds) {
        String filter = sinkIds.isEmpty() ? "" : " WHERE sinkid IN (" + placeholders(sinkIds.size(), 1) + ")";
        Query query = entityManager.createNativeQuery(
                "SELECT sinkid, status, count(*) FROM dependencytracking" + filter + " GROUP BY 1, 2");
        int position = 1;
        for (Integer sinkId : sinkIds) {
            query.setParameter(position++, sinkId);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> counts = new HashMap<>();
        for (Object[] row : rows) {
            // merge rather than put, because two stored values can resolve to one status:
            // ChunkSchedulingStatus.from folds the deleted BLOCKED value onto
            // SCHEDULED_FOR_DELIVERY, so a rolling restart in which an instance on the previous
            // build still writes 3 produces two group-by rows for one enum constant. Overwriting
            // there would make the recount that exists to repair counter drift a source of it.
            ChunkSchedulingStatus status = ChunkSchedulingStatus.from(intOf(row[1]));
            if (status == null) {
                LOGGER.warn("ignoring {} rows on sink {} with unknown status {}",
                        intOf(row[2]), intOf(row[0]), intOf(row[1]));
                continue;
            }
            counts.computeIfAbsent(intOf(row[0]), s -> new EnumMap<>(ChunkSchedulingStatus.class))
                    .merge(status, intOf(row[2]), Integer::sum);
        }
        return counts;
    }

    /**
     * @param sinkId sink to count for
     * @return the number of jobs and the number of chunks the sink currently has in the scheduler
     */
    public Integer[] countJobsAndChunks(int sinkId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT count(DISTINCT jobid), count(*) FROM dependencytracking WHERE sinkid = ?1")
                .setParameter(1, sinkId)
                .getResultList();
        Object[] row = rows.getFirst();
        return new Integer[]{intOf(row[0]), intOf(row[1])};
    }

    /**
     * Names the sinks holding at least one chunk in a status, from the table rather than the
     * counters.
     * <p>
     * The sink chunk counts are maintained from the write sites and can lose a delta, and a sink
     * whose count says zero is a sink the bulk submitters do not look at. This answers the same
     * question from the rows themselves, so a lost delta cannot hide a sink that has chunks
     * waiting.
     * <p>
     * No index serves it. Both ordered indexes lead on {@code sinkid}, so a predicate on status
     * alone scans, which is why {@code JobSchedulerBulkSubmitterBean} asks once a minute rather
     * than on its once-a-second dispatch tick.
     *
     * @param status status to look for
     * @return every sink with at least one chunk in that status
     */
    @Timed
    public Set<Integer> distinctSinkIdsWithStatus(ChunkSchedulingStatus status) {
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery(
                        "SELECT DISTINCT sinkid FROM dependencytracking WHERE status = ?1")
                .setParameter(1, status.value)
                .getResultList();
        return rows.stream().map(Number::intValue).collect(Collectors.toSet());
    }

    /**
     * @return every job with at least one chunk in the scheduler
     */
    public Set<Integer> distinctJobIds() {
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery(
                        "SELECT DISTINCT jobid FROM dependencytracking")
                .getResultList();
        return rows.stream().map(Number::intValue).collect(Collectors.toSet());
    }

    private static DependencyTracking fromRow(Object[] row) {
        return new DependencyTracking(new TrackingKey(intOf(row[0]), intOf(row[1])), intOf(row[2]), intOf(row[5]))
                .setStatus(ChunkSchedulingStatus.from(intOf(row[3])))
                .setPriority(intOf(row[4]))
                .withLastModified(((Timestamp) row[6]).toInstant())
                .withRetries(intOf(row[7]))
                .setTermination((Boolean) row[8])
                .setGateOpen((Boolean) row[9]);
    }

    /**
     * Clears the retry count as a chunk enters the delivery half.
     * <p>
     * The budget {@link #resend} spends is per phase. A chunk that needed every retry to get
     * through processing would otherwise arrive in delivery with the count already at the limit,
     * and the first delivery stall would be reported as beyond repair without a single delivery
     * attempt having been retried.
     * <p>
     * {@code READY_FOR_DELIVERY} is the only way into that half, so it is the only status that
     * resets. The processing half needs no equivalent, since a row is inserted with the count at
     * zero and never returns to processing.
     *
     * @param status status the chunk is moving to
     * @return the assignment to append, or empty where the count carries over
     */
    private static String retriesReset(ChunkSchedulingStatus status) {
        if (status != ChunkSchedulingStatus.READY_FOR_DELIVERY) {
            return " ";
        }
        return ", retries = 0 ";
    }

    private static int intOf(Object value) {
        return ((Number) value).intValue();
    }

    /**
     * Renders statuses as the literal list a status predicate compares against.
     * <p>
     * Literals rather than bound parameters because the values come from the enum and the set size
     * varies per target status, so a bound list would mean a parameter count that changes with the
     * statement.
     */
    private static String valueList(Collection<ChunkSchedulingStatus> statuses) {
        return statuses.stream()
                .map(s -> String.valueOf(s.value))
                .collect(Collectors.joining(", "));
    }

    private static String placeholders(int count, int from) {
        return IntStream.range(from, from + count)
                .mapToObj(i -> "?" + i)
                .collect(Collectors.joining(", "));
    }

    private static Set<ChunkSchedulingStatus> resendable() {
        return Arrays.stream(ChunkSchedulingStatus.values())
                .filter(s -> s.resend != null)
                .collect(Collectors.toSet());
    }

    private static String resendCases() {
        return resendable().stream()
                .map(s -> "WHEN " + s.value + " THEN " + s.resend.value)
                .collect(Collectors.joining(" "));
    }

    /**
     * A chunk the processing sweep may dispatch, carrying the priority the dispatch needs, so the
     * sweep does not read each chunk's row a second time.
     *
     * @param key      chunk to dispatch
     * @param priority the chunk's dispatch priority, which becomes the JMS priority
     */
    public record ProcessingCandidate(TrackingKey key, int priority) {
    }
}
