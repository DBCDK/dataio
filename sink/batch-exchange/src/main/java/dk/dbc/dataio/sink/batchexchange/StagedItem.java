package dk.dbc.dataio.sink.batchexchange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bookkeeping row for one item staged in the batch exchange, holding everything needed to
 * report that item's delivery once the consumer system has answered.
 * <p>
 * One row exists per batch this sink has created, and the unique constraint over
 * {@code (sink_id, record_key)} limits a record to one staged version at a time. An item that
 * arrived without a record key has a null one here, and null record keys compare as distinct,
 * so keyless items never block each other.
 */
@Entity
@Table(name = "staged_item")
public class StagedItem {
    @Id
    @Column(name = "batch")
    private int batch;

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

    @Column(name = "collapsed_count")
    private int collapsedCount;

    @Column(name = "collapsed_from")
    private String collapsedFrom;

    /**
     * Creates the bookkeeping row for an item being staged as the given batch
     *
     * @param batch     id of the batch holding the item's entries
     * @param batchName identity of the item the batch was created for
     * @return StagedItem object
     */
    public static StagedItem of(int batch, BatchName batchName) {
        StagedItem stagedItem = new StagedItem();
        stagedItem.batch = batch;
        stagedItem.sinkId = batchName.getSinkId();
        stagedItem.recordKey = batchName.getRecordKey();
        stagedItem.jobId = batchName.getJobId();
        stagedItem.chunkId = batchName.getChunkId();
        stagedItem.itemId = batchName.getItemId();
        return stagedItem;
    }

    /**
     * Gives the identity of the item this row was created for
     *
     * @return BatchName object
     */
    public BatchName batchName() {
        return new BatchName(sinkId, recordKey, jobId, chunkId, itemId);
    }

    public int getBatch() {
        return batch;
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

    public long getChunkId() {
        return chunkId;
    }

    public short getItemId() {
        return itemId;
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

    public StagedItem withCollapsed(int collapsedCount, String collapsedFrom) {
        this.collapsedCount = collapsedCount;
        this.collapsedFrom = collapsedFrom;
        return this;
    }
}
