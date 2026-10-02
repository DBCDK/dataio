package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * The bulk delivery dispatch query against {@code dependencytracking}.
 * <p>
 * Delivery order is {@code (priority DESC, jobId ASC, chunkId ASC)} and a chunk whose gate is closed
 * is not dispatched at all. Both are decided here, in SQL, over the {@code gate_open} and
 * {@code is_termination} columns and the ordering keys, with an index shaped to serve exactly that.
 * <p>
 * The table is the authority on every column the query here reads: {@code status} is advanced by
 * the scheduler, the gate columns are written by job-store, and both are written in the transaction
 * that decides them. A candidate this class returns is therefore a chunk that is genuinely awaiting
 * delivery with an open gate, and the order it comes back in is the order it should be dispatched
 * in, so the caller dispatches it without asking anything further.
 * <p>
 * The direct dispatch path reads {@code gate_open} off the row it already holds, and asks this
 * class one further question: whether anything parked outranks the chunk it is about to send. That
 * is the same query with {@code limit} 1, so the ordering is stated once,
 * see {@code JobSchedulerTransactionsBean.submitToDeliveringIfPossible} and {@link DispatchOrder}.
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
     * <p>
     * <b>The query returns {@code priority} alongside the key, and the sweep never looks at it.</b>
     * The direct dispatch path is what needs it. Before sending a chunk it calls this with
     * {@code limit} 1 to get the highest ranked chunk already waiting, and then compares that chunk
     * against the one it holds to decide which of the two should go first. The comparison is on all
     * three ordering keys, so the priority has to come back with the job and chunk id or the caller
     * would have to read the row a second time to get it.
     *
     * @param sinkId sink to dispatch for
     * @param limit  maximum number of candidates to return
     * @return candidates, highest priority first and lowest job then chunk id within a priority
     */
    public List<DeliveryCandidate> findDeliveryCandidates(int sinkId, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT jobid, chunkid, priority FROM dependencytracking " +
                                " WHERE sinkid = ?1 AND status = ?2 AND gate_open " +
                                " ORDER BY priority DESC, jobid, chunkid " +
                                " LIMIT ?3")
                .setParameter(1, sinkId)
                .setParameter(2, ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY.value)
                .setParameter(3, limit)
                .getResultList();
        return rows.stream()
                .map(row -> new DeliveryCandidate(
                        new TrackingKey(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).intValue()))
                .toList();
    }

    /**
     * A chunk awaiting delivery with an open gate, so a chunk that may be dispatched now.
     *
     * @param key      chunk to dispatch
     * @param priority the chunk's dispatch priority, which becomes the JMS priority
     */
    public record DeliveryCandidate(TrackingKey key, int priority) {
    }
}
