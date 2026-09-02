package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.tools.StringSetConverter;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The per-job gate's queries against {@code job} and {@code dependencytracking}.
 * <p>
 * The gate state is split across two tables. {@code job} ({@code data_chunks_delivered},
 * {@code data_chunks_expected}, {@code termination_barrier_lifted}) carries the per-job half.
 * {@code is_termination} and {@code gate_open} sit on {@code dependencytracking}, where the
 * dispatch query needs them. Both halves are job-store's: job-store decides whether a termination
 * chunk may be dispatched and writes that into {@code gate_open}, and the scheduler reads
 * {@code gate_open} to order dispatch and never writes it.
 * <b>No other writer of a {@code dependencytracking} row may write {@code is_termination} or
 * {@code gate_open}.</b>
 * <p>
 * Two consequences run through the whole class. The cross-job barrier answers from
 * {@code job.termination_barrier_lifted} rather than from a {@code dependencytracking} row being
 * present, so it does not depend on when that row is deleted and stays answerable once it is gone.
 * And {@link #upsertTerminationRow} creates the termination chunk's row itself. {@code gate_open}
 * is a column on that row, so until the row exists there is nowhere to record that the gate is
 * closed.
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
    private static final StringSetConverter MATCH_KEYS_CONVERTER = new StringSetConverter();

    public JobGateRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * Serializes everything that decides whether a gate opens, within one barrier scope.
     * <p>
     * Without it, site B can read "an earlier job has an unlifted barrier" under READ COMMITTED
     * while the transaction lifting that barrier cannot yet see site B's uncommitted termination
     * row, so both decline and the gate is left closed with nothing to open it. The lock scope is
     * {@code (sinkId, submitter)}, the same scope job partitioning already serializes on.
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
     * Creates the termination chunk's {@code dependencytracking} row, with {@code gate_open} set to
     * whether the chunk may be dispatched right away.
     * <p>
     * An insert rather than an update, because {@code gate_open} is a column on that row and a
     * closed gate cannot be recorded before the row exists. The conflict clause writes the two gate
     * columns and nothing else, leaving the rest of an existing row alone.
     * <p>
     * Creating the row means supplying more than the gate columns. {@code status} and
     * {@code sinkid} are NOT NULL with no default, and {@code submitter} is what the cross-job
     * barrier reads. The rest take defaults or nulls, since the scheduler writes them as the chunk
     * advances. {@code matchkeys} is supplied for the {@code waitingOn} barrier, which reads it
     * after a restart.
     *
     * @param key        termination chunk's tracking key
     * @param sinkId     sink the chunk is destined for
     * @param submitter  submitter the barrier is scoped to
     * @param status     status the chunk enters dependency tracking with
     * @param matchKeys  the chunk's match keys, carrying its barrier key
     * @param gateOpen   true only if this chunk may be dispatched right away
     */
    public void upsertTerminationRow(TrackingKey key, int sinkId, int submitter,
                                     ChunkSchedulingStatus status, Set<String> matchKeys, boolean gateOpen) {
        entityManager.createNativeQuery(
                        "INSERT INTO dependencytracking " +
                                "       (jobid, chunkid, sinkid, status, matchkeys, submitter, is_termination, gate_open) " +
                                "VALUES (?1, ?2, ?3, ?4, ?5, ?6, true, ?7) " +
                                "ON CONFLICT ON CONSTRAINT dependencytracking_pkey DO UPDATE " +
                                "  SET is_termination = excluded.is_termination, gate_open = excluded.gate_open")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .setParameter(3, sinkId)
                .setParameter(4, status.value)
                .setParameter(5, MATCH_KEYS_CONVERTER.convertToDatabaseColumn(matchKeys))
                .setParameter(6, submitter)
                .setParameter(7, gateOpen)
                .executeUpdate();
    }

    /**
     * @param key tracking key to ask about
     * @return true if the chunk is its job's termination chunk
     * <p>
     * A missing row answers false, which is correct either way round: a chunk with no row is not a
     * termination chunk, and a data chunk has to be counted whether or not its row is there.
     */
    public boolean isTerminationChunk(TrackingKey key) {
        return !entityManager.createNativeQuery(
                        "SELECT 1 FROM dependencytracking WHERE jobid = ?1 AND chunkid = ?2 AND is_termination")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .getResultList()
                .isEmpty();
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
     *
     * @param key termination chunk's tracking key
     */
    public void openGate(TrackingKey key) {
        entityManager.createNativeQuery(
                        "UPDATE dependencytracking SET gate_open = true " +
                                " WHERE jobid = ?1 AND chunkid = ?2 AND is_termination")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .executeUpdate();
    }

    /**
     * Records that this job's termination chunk no longer holds later jobs back.
     *
     * @param jobId job whose barrier is lifted
     */
    public void markTerminationBarrierLifted(int jobId) {
        entityManager.createNativeQuery(
                        "UPDATE job SET termination_barrier_lifted = true WHERE id = ?1")
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
}
