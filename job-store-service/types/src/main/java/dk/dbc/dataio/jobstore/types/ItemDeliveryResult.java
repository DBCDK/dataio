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
     * Drives job-store's own phase-counter/watermark logic for a single item's delivery
     * result (see docs/chunk-scheduling-redesign.md). Deliberately kept separate from
     * the sink's own {@link dk.dbc.dataio.commons.types.ChunkItem.Status}. The two
     * happen to line up value-for-value today (SUCCESS/FAILURE/IGNORE), but ChunkItem.Status
     * is a general-purpose outcome reused across partitioning/processing/delivering,
     * whereas SKIPPED here has one specific meaning (see below). Coupling the two would
     * silently tie job-store's counting/watermark behaviour to whatever a sink's
     * ChunkItem.Status happens to be for unrelated reasons.
     */
    public enum Status {
        DELIVERED,
        /**
         * The sink chose not to deliver this item because the delivery watermark showed
         * an equal-or-newer version of the same record had already been delivered, not
         * a general "ignored" outcome. Does not advance the watermark.
         */
        SKIPPED,
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
