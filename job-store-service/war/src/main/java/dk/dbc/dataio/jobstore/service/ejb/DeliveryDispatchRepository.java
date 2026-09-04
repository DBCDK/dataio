package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * The delivery dispatch queries against {@code dependencytracking}.
 * <p>
 * Delivery order is {@code (priority DESC, jobId ASC, chunkId ASC)} and a chunk whose gate is closed
 * is not dispatched at all. Both are decided here, in SQL, over the {@code gate_open} and
 * {@code is_termination} columns and the ordering keys, with an index shaped to serve exactly that.
 * <p>
 * The table is the authority on every column the queries here read: {@code status} is advanced by
 * the scheduler, the gate columns are written by job-store, and both are written in the transaction
 * that decides them. A candidate this class returns is therefore a chunk that is genuinely awaiting
 * delivery with an open gate, and the order it comes back in is the order it should be dispatched
 * in.
 * <p>
 * See docs/chunk-scheduling-redesign.md, "Delivery Ordering", and the comment block on
 * {@code V9__dependencytracking_delivery_indexes.sql}, which carries the candidate query verbatim
 * and the reasoning for the index that serves it.
 */
@Stateless
public class DeliveryDispatchRepository extends RepositoryBase {

    public DeliveryDispatchRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * Chunks awaiting delivery to a sink, in dispatch order.
     * <p>
     * The sorting is index-backed by {@code dependencytracking_delivery_order_index},
     * {@code (sinkid, status, gate_open, priority desc, jobid, chunkid)}, where the first three are
     * equality predicates and the rest is exactly this {@code ORDER BY}, so the rows come back in
     * order from the index scan itself and the {@code LIMIT} reads no further.
     * <p>
     * <b>The status is not a parameter, on purpose.</b> The {@code gate_open} predicate makes this
     * query delivery-only: a gate holds back delivery and nothing else, so under full barrier width a
     * queued job's data chunks sit at {@code gate_open = FALSE} while still being processed
     * normally. A caller that passed {@code SCHEDULED_FOR_PROCESSING} here would stop processing
     * exactly those chunks, which is the opposite of what the barrier asks for. The processing phase
     * needs its own query, without the gate predicate and with its own index.
     *
     * @param sinkId sink to dispatch for
     * @param limit  maximum number of candidates to return
     * @return candidate keys, highest priority first and lowest job then chunk id within a priority
     */
    public List<TrackingKey> findDeliveryCandidates(int sinkId, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT jobid, chunkid FROM dependencytracking " +
                                " WHERE sinkid = ?1 AND status = ?2 AND gate_open " +
                                " ORDER BY priority DESC, jobid, chunkid " +
                                " LIMIT ?3")
                .setParameter(1, sinkId)
                .setParameter(2, ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY.value)
                .setParameter(3, limit)
                .getResultList();
        return rows.stream()
                .map(row -> new TrackingKey(((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                .toList();
    }

    /**
     * @param key chunk to ask about
     * @return true only if a row exists saying this chunk's gate is closed
     * <p>
     * <b>An unwritten gate is an open gate.</b> {@code gate_open} is {@code NOT NULL DEFAULT TRUE}
     * and only a writer meaning to close a gate touches the column, see
     * {@link JobGateRepository#upsertTerminationRow}, so the absence of a closing write is the
     * answer and not a missing one. A chunk with no row at all answers false for the same reason:
     * nothing has closed its gate.
     */
    public boolean hasClosedGate(TrackingKey key) {
        return !entityManager.createNativeQuery(
                        "SELECT 1 FROM dependencytracking " +
                                " WHERE jobid = ?1 AND chunkid = ?2 AND NOT gate_open")
                .setParameter(1, key.getJobId())
                .setParameter(2, key.getChunkId())
                .getResultList()
                .isEmpty();
    }
}
