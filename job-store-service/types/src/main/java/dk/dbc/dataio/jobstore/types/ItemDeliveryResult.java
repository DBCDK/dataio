package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.ChunkItem;

/**
 * The outcome of one item's delivery attempt, as reported by a sink (see
 * docs/chunk-scheduling-redesign.md). sinkId and recordKey identify the delivery
 * watermark row to advance (recordKey is null for items with no watermark key, e.g.
 * barrier items), status and chunkItem are deliberately separate fields carrying
 * different information, see {@link Status} for why.
 */
public record ItemDeliveryResult(long sinkId, String recordKey, Status status, ChunkItem chunkItem) {
    /**
     * Drives job-store's own phase-counter and watermark logic for a single item's delivery
     * result (see docs/chunk-scheduling-redesign.md). Deliberately kept separate from the
     * sink's own {@link dk.dbc.dataio.commons.types.ChunkItem.Status}, which is a
     * general-purpose outcome reused across partitioning, processing and delivering,
     * whereas every value here has one specific meaning for the delivering phase alone.
     * Coupling the two would silently tie job-store's counting and watermark behaviour to
     * whatever a sink's ChunkItem.Status happens to be for unrelated reasons.
     * <p>
     * The returned chunk item is stored verbatim as the item's delivering outcome and
     * feeds no counter, so the value chosen here is the only thing deciding how the item
     * is counted. What each one decides:
     * <pre>
     *                 DELIVERING counter   watermark   returned by
     * DELIVERED       succeeded            advances    the sink
     * SUPERSEDED      ignored              untouched   the sink framework only
     * IGNORED         ignored              untouched   the sink
     * FAILED          failed               untouched   the sink
     * </pre>
     * SUPERSEDED and IGNORED are indistinguishable to job-store, which counts both as
     * ignored and advances neither watermark. They are separate values because they are
     * separate answers to "why is this record not at the target", which is the question
     * asked when investigating one, and because they have different authors.
     */
    public enum Status {
        /**
         * The sink sent this item to its target system.
         * <p>
         * The only value that advances the delivery watermark, so it must not be used for
         * an item the sink chose not to send. Counted as succeeded.
         */
        DELIVERED,
        /**
         * Not sent, because the delivery watermark showed that a newer version of the same
         * record had already been delivered.
         * <p>
         * Returned by the sink framework alone, never by a sink: a sink does not read the
         * watermark and therefore cannot detect supersession. Counted as ignored.
         */
        SUPERSEDED,
        /**
         * Not sent, because there was nothing to send.
         * <p>
         * Counted as ignored.
         */
        IGNORED,
        /**
         * The sink attempted the delivery and the target rejected it in a way retrying
         * will not fix.
         * <p>
         * Counted as failed.
         */
        FAILED
    }

    /**
     * The half of the result a sink itself decides, for use as the return value of a
     * sink's delivery method. sinkId and recordKey are left unset for the sink framework
     * to fill in with {@link #withWatermarkKey(long, String)} from the message it is
     * delivering, since those two identify the watermark row and are not the sink's to
     * choose.
     * <p>
     * A static factory rather than a second constructor on purpose: this record is
     * unmarshalled from the request body of the item delivery endpoint, and a second
     * constructor would become a competing creator candidate for the JSON binding.
     *
     * @param status    delivery outcome
     * @param chunkItem outcome item, stored verbatim as the item's delivering outcome
     * @return delivery result carrying no sink identification
     */
    public static ItemDeliveryResult of(Status status, ChunkItem chunkItem) {
        return new ItemDeliveryResult(0, null, status, chunkItem);
    }

    /**
     * Adds the identity of the watermark row this result may advance, which is the pair
     * (sinkId, recordKey) the sink framework owns rather than the sink itself: together
     * they are the primary key of sink_record_delivery_watermark, and they are what the
     * two are for - the delivery endpoint reads sinkId for the watermark upsert and
     * nothing else.
     *
     * @param sinkId    id of the sink reporting the result
     * @param recordKey watermark key of the delivered record, null when this delivery
     *                  identifies no watermark row and must therefore advance none
     * @return copy of this result with sinkId and recordKey replaced
     */
    public ItemDeliveryResult withWatermarkKey(long sinkId, String recordKey) {
        return new ItemDeliveryResult(sinkId, recordKey, status, chunkItem);
    }
}
