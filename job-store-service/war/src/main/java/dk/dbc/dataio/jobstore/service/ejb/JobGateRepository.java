package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * The per-job gate's queries against {@code job} and {@code dependencytracking}.
 * <p>
 * The gate state is split across two tables. {@code job} ({@code data_chunks_delivered},
 * {@code data_chunks_expected}, {@code termination_barrier_lifted}) carries the per-job half.
 * {@code is_termination} and {@code gate_open} sit on {@code dependencytracking}, where the
 * dispatch query needs them. This class decides whether a chunk may be dispatched and writes that
 * into {@code gate_open}; the dispatch path reads the column to order dispatch and never writes it.
 * Always a termination chunk, and on a full-width sink type a data chunk too.
 * <p>
 * {@code gate_open} is {@code NOT NULL DEFAULT TRUE} and only a write that means to close a gate
 * touches the column. That convention is what lets every other writer of a
 * {@code dependencytracking} row leave the column out of its statement, and it is why the two
 * inserts that create a row, {@link #insertTerminationRow} and
 * {@link DependencyTrackingRepository#insert}, both take the verdict as a value: a closed gate has
 * nowhere to be recorded until the row exists, so a gate closed in a second statement is a gate
 * closed too late.
 * <p>
 * One consequence runs through the whole class. The cross-job barrier answers from
 * {@code job.termination_barrier_lifted} rather than from a {@code dependencytracking} row being
 * present, so it does not depend on when that row is deleted and stays answerable once it is gone.
 * <p>
 * Everything here is a native query on the caller's {@link EntityManager}, so it runs in the
 * caller's transaction and on its connection. Native rather than JPQL or entity access because
 * these are single statements against columns no entity owns, and because the counters have to be
 * read back after an increment that leaves a managed {@link
 * dk.dbc.dataio.jobstore.service.entity.JobEntity} stale.
 * <p>
 * See docs/chunk-scheduling-redesign.md, "Barrier Chunks - Per-Job Gate", and
 * job-store-service/dependency-tracking.md, "Terminology".
 */
@Stateless
public class JobGateRepository extends RepositoryBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobGateRepository.class);

    /**
     * "No job earlier than this row's still holds a barrier on this row's scope."
     * <p>
     * Correlated against the outer {@code d}, so it reads the barrier once per candidate row rather
     * than once per statement. Served by {@code dependencytracking_barrier_index},
     * {@code (sinkid, submitter, jobid) where is_termination}, plus one primary key probe into
     * {@code job} per candidate.
     */
    private static final String NO_EARLIER_UNLIFTED_BARRIER =
            "   AND NOT EXISTS (SELECT 1 FROM dependencytracking e JOIN job j ON j.id = e.jobid " +
                    "                    WHERE e.sinkid = d.sinkid AND e.submitter = d.submitter " +
                    "                      AND e.is_termination AND e.jobid < d.jobid " +
                    "                      AND j.termination_barrier_lifted IS FALSE)";

    /** Shared head of the two data-chunk reopening statements, which differ only in their bound. */
    private static final String OPEN_DATA_CHUNK_GATES =
            "UPDATE dependencytracking d SET gate_open = TRUE " +
                    " WHERE d.sinkid = ?1 AND d.submitter = ?2 " +
                    "   AND NOT d.gate_open AND NOT d.is_termination ";

    public JobGateRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * Serializes everything that decides whether a gate opens, within one barrier scope.
     * <p>
     * Without it, the termination insert can read "an earlier job has an unlifted barrier" under
     * READ COMMITTED while the transaction lifting that barrier cannot yet see that insert's
     * uncommitted row, so both decline and the gate is left closed with nothing to open it. The
     * lock scope is {@code (sinkId, submitter)}, the same scope job partitioning already
     * serializes on.
     * <p>
     * The transaction-scoped variant releases at commit, so it is safe behind a connection pool.
     *
     * @param sinkId    sink the barrier applies to
     * @param submitter submitter the barrier applies to
     */
    public void advisoryLock(int sinkId, int submitter) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?1)")
                .setParameter(1, barrierLockKey(sinkId, submitter))
                .getSingleResult();
    }

    /**
     * Packs the barrier scope into the single {@code bigint} the lock function takes, {@code sinkId}
     * high and {@code submitter} low. Distinct scopes give distinct keys, so unrelated scopes never
     * contend, and one scope gives one key in every JVM, which is what lets transactions in
     * separate processes coordinate on it.
     */
    private static long barrierLockKey(int sinkId, int submitter) {
        return ((long) sinkId << 32) | (submitter & 0xFFFFFFFFL);
    }

    /**
     * @param sinkId    sink to look for
     * @param submitter submitter to look for
     * @param jobId     job whose gate is being evaluated
     * @return true if an earlier job with the same submitter on the same sink still holds an
     * unlifted termination barrier, in which case this job's gate must stay closed
     */
    public boolean hasEarlierUndeliveredTermination(int sinkId, int submitter, int jobId) {
        // Served by dependencytracking_barrier_index, (sinkid, submitter, jobid) where
        // is_termination, plus one primary key probe into job per candidate row.
        return !entityManager.createNativeQuery(
                        "SELECT 1 FROM dependencytracking d JOIN job j ON j.id = d.jobid " +
                                " WHERE d.sinkid = ?1 AND d.submitter = ?2 AND d.jobid < ?3 " +
                                "   AND d.is_termination AND j.termination_barrier_lifted IS FALSE " +
                                " LIMIT 1")
                .setParameter(1, sinkId)
                .setParameter(2, submitter)
                .setParameter(3, jobId)
                .getResultList()
                .isEmpty();
    }

    /**
     * Creates the job's termination chunk row, with {@code gate_open} set to whether the chunk may
     * be dispatched right away.
     * <p>
     * The only insert of that row, and the only writer of {@code is_termination}. Every column is
     * supplied here, since nothing else writes this row into existence.
     * <p>
     * <b>The conflict clause is defensive, and writes only the two gate columns.</b> No caller
     * reaches it: {@link PgJobStoreRepository#createJobTerminationChunkEntity} persists the
     * termination chunk's own {@code chunk} row first, which fails on its primary key before this
     * statement runs, so a second call for one job cannot get here. It writes the gate columns
     * anyway because a row that somehow existed with an open gate has to be shut, and it must
     * <b>not</b> be widened to {@code status}: reaching it against a chunk already in
     * {@code QUEUED_FOR_DELIVERY} would reset it to {@code READY_FOR_DELIVERY} and send the job's
     * end-of-job item to the sink a second time.
     * <p>
     * Data chunks are inserted by {@link DependencyTrackingRepository#insert} instead. They carry
     * their gate verdict the same way, but they are the scheduler's ordinary rows and there is no
     * reason for the gate to own their statement.
     *
     * @param key       termination chunk's tracking key
     * @param sinkId    sink the chunk is destined for
     * @param submitter submitter the barrier is scoped to
     * @param status    status the chunk enters dependency tracking with
     * @param priority  the chunk's dispatch priority
     * @param gateOpen  true only if this chunk may be dispatched right away
     */
    public void insertTerminationRow(TrackingKey key, int sinkId, int submitter, ChunkSchedulingStatus status,
                                     int priority, boolean gateOpen) {
        entityManager.createNativeQuery(
                        "INSERT INTO dependencytracking " +
                                "       (jobid, chunkid, sinkid, status, priority, submitter, lastmodified, is_termination, gate_open) " +
                                "VALUES (?1, ?2, ?3, ?4, ?5, ?6, now(), TRUE, ?7) " +
                                "ON CONFLICT ON CONSTRAINT dependencytracking_pkey DO UPDATE " +
                                "  SET is_termination = excluded.is_termination, gate_open = excluded.gate_open")
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
     * @param sinkId    sink the job delivers to
     * @param submitter the job's submitter
     * @param jobId     job to ask about
     * @return the chunk id of the job's termination chunk if it exists and its gate is still
     * closed, empty otherwise
     */
    public OptionalInt closedTerminationChunkId(int sinkId, int submitter, int jobId) {
        // Predicate ordered to match dependencytracking_barrier_index.
        List<?> result = entityManager.createNativeQuery(
                        "SELECT chunkid FROM dependencytracking " +
                                " WHERE sinkid = ?1 AND submitter = ?2 AND jobid = ?3 " +
                                "   AND is_termination AND NOT gate_open")
                .setParameter(1, sinkId)
                .setParameter(2, submitter)
                .setParameter(3, jobId)
                .getResultList();
        if (result.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(((Number) result.getFirst()).intValue());
    }

    /**
     * Opens the gate of an already inserted termination chunk.
     * <p>
     * <b>{@code NOT gate_open} is what keeps this off open rows</b>, and with it every gate write
     * taken under the barrier scope's advisory lock matches closed gates only. That disjointness is
     * what makes the delivery acknowledgement's lock order safe, see the note on {@link JobGateBean}.
     * It also makes the statement a no-op against a gate that is already open, which is what a
     * second caller finds.
     *
     * @param key termination chunk's tracking key
     */
    public void openGate(TrackingKey key) {
        entityManager.createNativeQuery(
                        "UPDATE dependencytracking SET gate_open = true " +
                                " WHERE jobid = ?1 AND chunkid = ?2 AND is_termination AND NOT gate_open")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .executeUpdate();
    }

    /**
     * Records that this job's termination chunk no longer holds later jobs back.
     * <p>
     * <b>Guarded on {@code IS FALSE}, and it must stay that way.</b> The column is nullable with
     * three meanings, {@code NULL} for a job that never had a termination chunk at all, which is
     * the majority. Callers reach this for any job whose rows are being removed, not only for
     * jobs that hold a barrier: {@link JobsBean#abortJob} and
     * {@link dk.dbc.dataio.jobstore.service.rs.AdminBean#recheckBlocks} both do. Unguarded, those
     * would rewrite {@code NULL} to {@code TRUE} and collapse "never had a barrier" into "had one,
     * now lifted", which is the conflation the nullable column exists to avoid.
     * <p>
     * The return value is what tells a caller whether this job was holding a barrier at all, so the
     * re-trigger can be skipped entirely when it was not. The statement takes the job row lock, so
     * of two concurrent callers exactly one sees a row affected.
     *
     * @param jobId job whose barrier is lifted
     * @return the number of rows updated, so 1 if this call lifted a standing barrier and 0 if
     * there was none to lift
     */
    public int markTerminationBarrierLifted(int jobId) {
        return entityManager.createNativeQuery(
                        "UPDATE job SET termination_barrier_lifted = true " +
                                " WHERE id = ?1 AND termination_barrier_lifted IS FALSE")
                .setParameter(1, jobId)
                .executeUpdate();
    }

    /**
     * Candidates for the cross-job re-trigger.
     * <p>
     * The {@code NOT gate_open} and {@code is_termination} conditions are what keep this off jobs
     * that are still partitioning: such a job is a candidate by submitter, sink and job id, and its
     * counters satisfy the comparison for the whole partitioning window, but it has no termination
     * row yet.
     *
     * @param sinkId    sink to scan
     * @param submitter submitter to scan
     * @param jobId     job whose barrier was just lifted
     * @return termination chunks of later jobs whose gate is still closed, in ascending job id
     */
    public List<TrackingKey> laterClosedGates(int sinkId, int submitter, int jobId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT d.jobid, d.chunkid FROM dependencytracking d JOIN job j ON j.id = d.jobid " +
                                " WHERE d.sinkid = ?1 AND d.submitter = ?2 AND d.jobid > ?3 " +
                                "   AND d.is_termination AND NOT d.gate_open " +
                                "   AND j.termination_barrier_lifted IS FALSE " +
                                " ORDER BY d.jobid")
                .setParameter(1, sinkId)
                .setParameter(2, submitter)
                .setParameter(3, jobId)
                .getResultList();
        return rows.stream()
                .map(row -> new TrackingKey(((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                .toList();
    }

    /**
     * Opens the gates of data chunks in later jobs on this submitter and sink, for the jobs that no
     * longer have an earlier unlifted termination barrier ahead of them.
     * <p>
     * The counterpart to {@link #laterClosedGates} for the full barrier width. A data chunk has no
     * counter of its own, so the earlier barrier is the whole condition and one statement settles
     * it, where a termination chunk needs its own job's data chunks accounted for as well.
     * <p>
     * <b>Does not consult {@code REQUIRES_FULL_WIDTH_BARRIER}.</b> It does not need to: a data chunk's
     * gate is only ever closed by {@link JobGateBean#insertDataChunkRow}, which does read
     * that set, so on a sink type outside it there is no closed data chunk to find and this matches
     * nothing. Reached only from {@link JobGateBean#liftBarrierAndRetrigger}, which returns before
     * this on a job that held no barrier, so in practice the sink types that pay the no-op are the
     * ones with a termination chunk.
     * <p>
     * Leaving the width out buys two things. The set is read in one place, the scheduler, where the
     * job and its cached sink are already in hand, rather than at every call site that lifts a
     * barrier, one of which holds only the removed dependency tracking entry and would need an extra
     * job load per delivered termination chunk. And a row stays reopenable after the width that closed
     * it has gone: one closed by an earlier deployment, or by a sink whose type has since changed,
     * would otherwise be stranded closed with only the sweep to rescue it.
     * <p>
     * The {@code NOT EXISTS} is what keeps three or more queued jobs correct: lifting job A's
     * barrier releases job B's data chunks while job C's stay closed behind job B's still unlifted
     * one.
     *
     * @param sinkId    sink to scan
     * @param submitter submitter to scan
     * @param jobId     job whose barrier was just lifted, so only later jobs are considered
     * @return the number of data chunks opened
     */
    public int openLaterDataChunkGates(int sinkId, int submitter, int jobId) {
        return entityManager.createNativeQuery(OPEN_DATA_CHUNK_GATES
                        + "   AND d.jobid > ?3 " + NO_EARLIER_UNLIFTED_BARRIER)
                .setParameter(1, sinkId)
                .setParameter(2, submitter)
                .setParameter(3, jobId)
                .executeUpdate();
    }

    /**
     * Opens the gate of every data chunk in the scope that no longer has an earlier unlifted
     * termination barrier ahead of it, whatever job it belongs to.
     * <p>
     * The sweep's form of {@link #openLaterDataChunkGates}, with no job to be later than. The
     * re-trigger is edge-triggered on one job's barrier being lifted, so it can only ever look
     * forward from that job. The sweep has no such starting point: it exists precisely for the
     * gates whose edge was missed.
     *
     * @param sinkId    sink to scan
     * @param submitter submitter to scan
     * @return the number of data chunks opened
     */
    public int openDataChunkGates(int sinkId, int submitter) {
        return entityManager.createNativeQuery(OPEN_DATA_CHUNK_GATES + NO_EARLIER_UNLIFTED_BARRIER)
                .setParameter(1, sinkId)
                .setParameter(2, submitter)
                .executeUpdate();
    }

    /**
     * Counts one delivered data chunk against the job's gate.
     * <p>
     * One atomic statement, never a read followed by a write: a lost update would leave the counter
     * permanently below {@code data_chunks_expected} with no further chunk to deliver, so the gate
     * would never open. The row lock it takes is also the serialization point the gate evaluation
     * that follows hangs off, so two concurrent last-chunk deliveries cannot both read a short
     * count.
     *
     * @param jobId job the delivered chunk belongs to
     */
    public void incrementDataChunksDelivered(int jobId) {
        entityManager.createNativeQuery(
                        "UPDATE job SET data_chunks_delivered = data_chunks_delivered + 1 WHERE id = ?1")
                .setParameter(1, jobId)
                .executeUpdate();
    }

    /**
     * @param jobId job to ask about
     * @return true if every data chunk of the job has been acknowledged as delivered
     */
    public boolean dataChunksAccountedFor(int jobId) {
        List<?> result = entityManager.createNativeQuery(
                        "SELECT data_chunks_delivered >= data_chunks_expected FROM job WHERE id = ?1")
                .setParameter(1, jobId)
                .getResultList();
        if (result.isEmpty()) {
            LOGGER.warn("no job row for {}, so its data chunks count as unaccounted for", jobId);
            return false;
        }
        return (Boolean) result.getFirst();
    }

    /**
     * Whether any of the job's data chunks are still in dependency tracking.
     * <p>
     * <b>Deliberately not read from {@code data_chunks_delivered}.</b> The counter and the rows are
     * written in one transaction now, so they cannot disagree by a delivery being counted without
     * its row going or the other way round. What is left for the sweep to repair is a job whose
     * rows were dropped wholesale, by an abort or by the recheck, and for those the counter says
     * nothing while the absence of rows says everything.
     * <p>
     * Absence therefore means every data chunk has been acknowledged or dropped.
     *
     * @param jobId job to ask about
     * @return true if the job still has at least one data chunk in dependency tracking
     */
    public boolean hasUndeliveredDataChunks(int jobId) {
        return !entityManager.createNativeQuery(
                        "SELECT 1 FROM dependencytracking " +
                                " WHERE jobid = ?1 AND NOT is_termination LIMIT 1")
                .setParameter(1, jobId)
                .getResultList()
                .isEmpty();
    }

    /**
     * The barrier scopes the sweep has to visit, being those holding at least one closed gate.
     * <p>
     * One scan of {@code dependencytracking} per sweep, which is what an hourly job can afford and
     * what lets everything after it be an index seek. Scoping the sweep this way is also what keeps
     * the advisory lock meaningful: the lock is per scope, so the work under it has to be too.
     *
     * @return distinct sink and submitter pairs with at least one closed gate
     */
    public List<BarrierScope> closedGateScopes() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT DISTINCT sinkid, submitter FROM dependencytracking WHERE NOT gate_open")
                .getResultList();
        return rows.stream()
                .map(row -> new BarrierScope(((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                .toList();
    }

    /**
     * @param sinkId    sink to scan
     * @param submitter submitter to scan
     * @return termination chunks in the scope whose gate is still closed, in ascending job id, so
     * the sweep considers them in the order they would be delivered
     */
    public List<TrackingKey> closedTerminationGates(int sinkId, int submitter) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT jobid, chunkid FROM dependencytracking " +
                                " WHERE sinkid = ?1 AND submitter = ?2 " +
                                "   AND is_termination AND NOT gate_open " +
                                " ORDER BY jobid")
                .setParameter(1, sinkId)
                .setParameter(2, submitter)
                .getResultList();
        return rows.stream()
                .map(row -> new TrackingKey(((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                .toList();
    }

    /**
     * Jobs standing with a barrier that nothing can lift any more, because the termination row the
     * lift would have fired on is already gone.
     * <p>
     * Reached when a job's rows are dropped through {@code removeJobId} on a path that did not lift
     * the barrier, or when the lift rolled back after that removal had committed on its own. Left alone,
     * every later job on that submitter and sink is held behind a barrier whose job no longer
     * exists in dependency tracking at all.
     * <p>
     * The sink and submitter come from the {@link JobEntity} rather than from SQL, since the columns
     * that carry them on {@code job} are a foreign key into the sink cache and a JSON specification.
     * A job whose sink cache is missing is skipped: without a sink there is no scope to re-trigger
     * in, and its barrier holds nothing back that could be found.
     *
     * @return one entry per job holding an unliftable barrier
     */
    public List<JobBarrier> jobsWithUnliftedBarrierAndNoTerminationRow() {
        @SuppressWarnings("unchecked")
        List<Number> jobIds = entityManager.createNativeQuery(
                        "SELECT j.id FROM job j " +
                                " WHERE j.termination_barrier_lifted IS FALSE " +
                                "   AND NOT EXISTS (SELECT 1 FROM dependencytracking d " +
                                "                    WHERE d.jobid = j.id AND d.is_termination)")
                .getResultList();
        List<JobBarrier> barriers = new ArrayList<>(jobIds.size());
        for (Number jobId : jobIds) {
            JobEntity job = entityManager.find(JobEntity.class, jobId.intValue());
            if (job == null || job.getCachedSink() == null) {
                LOGGER.warn("job {} holds an unlifted barrier but has no cached sink, so it has no scope to re-trigger in", jobId);
                continue;
            }
            barriers.add(new JobBarrier(job.getId(), job.getCachedSink().getSink().getId(),
                    (int) job.getSpecification().getSubmitterId()));
        }
        return barriers;
    }

    /**
     * @param jobId job to ask about
     * @return the job's delivered data-chunk count, read from the database rather than from a
     * managed entity because the increment is a native statement
     */
    public int dataChunksDelivered(int jobId) {
        List<?> result = entityManager.createNativeQuery(
                        "SELECT data_chunks_delivered FROM job WHERE id = ?1")
                .setParameter(1, jobId)
                .getResultList();
        if (result.isEmpty()) {
            LOGGER.warn("no job row for {}, so it counts zero delivered data chunks", jobId);
            return 0;
        }
        return ((Number) result.getFirst()).intValue();
    }

    /**
     * The scope a barrier applies to, which is also the scope of its advisory lock and the scope the
     * jobqueue serializes partitioning on.
     *
     * @param sinkId    sink the barrier applies to
     * @param submitter submitter the barrier applies to
     */
    public record BarrierScope(int sinkId, int submitter) {
    }

    /**
     * A job holding a barrier, carrying the scope that barrier applies to.
     *
     * @param jobId     job holding the barrier
     * @param sinkId    sink the barrier applies to
     * @param submitter submitter the barrier applies to
     */
    public record JobBarrier(int jobId, int sinkId, int submitter) {
    }
}
