package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.TrackingKey;

/**
 * The order the scheduler dispatches chunks in, {@code (priority DESC, jobId ASC, chunkId ASC)},
 * expressed as a comparison rather than an {@code ORDER BY}.
 * <p>
 * The bulk sweeps get this order from SQL, see
 * {@link DeliveryDispatchRepository#findDeliveryCandidates} and
 * {@link DependencyTrackingRepository#findProcessingCandidates}. The direct dispatch paths have no
 * query to sort, so they compare the arriving chunk against the head of the parked queue here. The
 * two statements of the order have to agree, and changing one means changing the other.
 * <p>
 * <b>The order carries a correctness guarantee, not a fairness one.</b> Every record of a MARC
 * hierarchy shares one {@code correlationKey}, so the broker serialises them into a single group
 * and delivers them in the order they were sent. Nothing else decides which of a head and its
 * volumes reaches a sink first, and the delivery watermark does not help, since head and volume are
 * different records with different keys. See docs/chunk-scheduling-redesign.md, "Mapping" and
 * "Priority Override for Live Head/Section Records".
 */
final class DispatchOrder {

    private DispatchOrder() {
    }

    /**
     * Whether one chunk is dispatched before another.
     * <p>
     * Strict, and no tiebreak beyond the three keys is needed: the direct paths compare a chunk in
     * a {@code READY_*} status against one in a {@code SCHEDULED_*} status, so the two are never
     * the same chunk.
     *
     * @param priority      priority of the arriving chunk
     * @param key           key of the arriving chunk
     * @param otherPriority priority of the chunk to compare against
     * @param otherKey      key of the chunk to compare against
     * @return true if the arriving chunk is dispatched first
     */
    static boolean outranks(int priority, TrackingKey key, int otherPriority, TrackingKey otherKey) {
        if (priority != otherPriority) {
            return priority > otherPriority;
        }
        if (key.getJobId() != otherKey.getJobId()) {
            return key.getJobId() < otherKey.getJobId();
        }
        return key.getChunkId() < otherKey.getChunkId();
    }
}
