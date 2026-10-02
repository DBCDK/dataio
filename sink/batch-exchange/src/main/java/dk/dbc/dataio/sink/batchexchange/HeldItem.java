package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * An item held back because another version of the same record is already staged.
 * <p>
 * The item's records are kept here rather than in the batch exchange, because any entry the
 * sink writes there is claimable by the consumer system and would be applied against no
 * ordering. {@link BatchFinalizer} stages the row once the blocking batch completes.
 * <p>
 * A record has at most one held version. A newer one replaces the parked row through
 * {@link #displacing(HeldItem)} and the displaced version is reported as superseded, so
 * versions collapse rather than queue.
 * <p>
 * Held is not deferred. Every item this sink stages has its delivery result deferred, in the
 * sense {@code SinkMessageConsumerAdapter.defersDeliveryResult} gives the word: its outcome is
 * reported once the consumer system has answered. A held item has not reached the batch
 * exchange, so there is nothing yet for the consumer system to answer for.
 */
@Entity
@Table(name = "held_item")
public class HeldItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "held_item_id_seq")
    @SequenceGenerator(name = "held_item_id_seq", sequenceName = "held_item_id_seq", allocationSize = 1)
    @Column(name = "id")
    private int id;

    @Column(name = "sink_id")
    private long sinkId;

    @Column(name = "record_key")
    private String recordKey;

    @Column(name = "job_id")
    private int jobId;

    @Column(name = "chunk_id")
    private long chunkId;

    @Column(name = "item_id")
    private short itemId;

    @Column(name = "tracking_id")
    private String trackingId;

    @Column(name = "priority")
    private int priority;

    @Column(name = "payload")
    private byte[] payload;

    @Column(name = "collapsed_count")
    private int collapsedCount;

    @Column(name = "collapsed_from")
    private String collapsedFrom;

    /**
     * Creates the parked row for an item that cannot be staged yet
     *
     * @param message   message carrying the item, for the priority its entries are given when
     *                  the row is eventually staged
     * @param item      processing outcome whose records are parked
     * @param batchName identity of the item
     * @return HeldItem object
     */
    public static HeldItem of(ConsumedMessage message, ChunkItem item, BatchName batchName) {
        HeldItem heldItem = new HeldItem();
        heldItem.sinkId = batchName.getSinkId();
        heldItem.recordKey = batchName.getRecordKey();
        heldItem.jobId = batchName.getJobId();
        heldItem.chunkId = batchName.getChunkId();
        heldItem.itemId = batchName.getItemId();
        heldItem.trackingId = item.getTrackingId();
        heldItem.priority = message.getPriority().getValue();
        heldItem.payload = item.getData();
        return heldItem;
    }

    /**
     * Records that this item takes the place of an already parked version
     * <p>
     * The count accumulates across every replacement and the origin keeps the oldest displaced
     * version, so both stay bounded however many versions a record collapses through.
     *
     * @param displaced the version this one replaces
     * @return this item, carrying what was skipped to arrive at it
     */
    public HeldItem displacing(HeldItem displaced) {
        collapsedCount = displaced.collapsedCount + 1;
        if (displaced.collapsedFrom != null) {
            collapsedFrom = displaced.collapsedFrom;
        } else {
            collapsedFrom = displaced.batchName().asItemReference();
        }
        return this;
    }

    /**
     * Gives the identity of the item parked in this row
     *
     * @return BatchName object
     */
    public BatchName batchName() {
        return new BatchName(sinkId, recordKey, jobId, chunkId, itemId);
    }

    public int getId() {
        return id;
    }

    public long getSinkId() {
        return sinkId;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public int getJobId() {
        return jobId;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public int getPriority() {
        return priority;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Gives the number of versions of this record that were skipped to arrive at this one
     *
     * @return number of collapsed versions, zero when this version displaced none
     */
    public int getCollapsedCount() {
        return collapsedCount;
    }

    /**
     * Gives the oldest version skipped to arrive at this one
     *
     * @return that version's ids, or null when this version displaced none
     */
    public String getCollapsedFrom() {
        return collapsedFrom;
    }
}
