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
}
