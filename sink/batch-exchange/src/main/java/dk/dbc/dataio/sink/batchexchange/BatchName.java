package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.jms.JMSHeader;

/**
 * Identity of one item staged in the batch exchange, carrying everything needed to report that
 * item's delivery once the consumer system has answered.
 * <p>
 * The values are held in {@code staged_item} and {@code held_item}. This class is the
 * in-memory form they take, and it also renders the name given to the batch:
 * <pre>
 * &lt;sinkId&gt;-&lt;recordKey&gt;-&lt;jobId&gt;-&lt;chunkId&gt;-&lt;itemId&gt;
 *
 * 15-870970:12345678-123-4-7
 * 15--123-4-7                  no record key
 * </pre>
 * That name is written for operators reading the batch exchange directly. Nothing reads it
 * back, so a record key containing hyphens needs no special treatment.
 */
public class BatchName {
    private final long sinkId;
    private final String recordKey;
    private final int jobId;
    private final long chunkId;
    private final short itemId;

    BatchName(long sinkId, String recordKey, int jobId, long chunkId, short itemId) {
        this.sinkId = sinkId;
        this.recordKey = recordKey;
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.itemId = itemId;
    }

    /**
     * Creates the identity of the item the given message carries
     *
     * @param message message carrying the item, whose headers name both the item and the
     *                watermark row its delivery may advance
     * @return BatchName object
     * @throws IllegalArgumentException if the message lacks a header the identity needs
     */
    public static BatchName fromMessage(ConsumedMessage message) throws IllegalArgumentException {
        return new BatchName(
                header(message, JMSHeader.sinkId, Long.class),
                JMSHeader.recordKey.getHeader(message, String.class),
                header(message, JMSHeader.jobId, Integer.class),
                header(message, JMSHeader.chunkId, Long.class),
                header(message, JMSHeader.itemId, Short.class));
    }

    /**
     * Whether two identities name the same item
     * <p>
     * The record key takes no part in the comparison. Two identities agreeing on job, chunk and
     * item are the same item, and a record key differing between them would mean the same item
     * was partitioned twice with different record identities, which cannot happen.
     *
     * @param other identity to compare against
     * @return true if both name the same item
     */
    public boolean isSameItemAs(BatchName other) {
        return jobId == other.jobId && chunkId == other.chunkId && itemId == other.itemId;
    }

    /**
     * Orders two identities by the version of the record they carry, as the delivery watermark
     * orders the same three ids
     *
     * @param other identity to compare against, for the same record as this one
     * @return true if the other identity carries a newer version of the record than this one
     */
    public boolean isOlderThan(BatchName other) {
        if (jobId != other.jobId) {
            return jobId < other.jobId;
        }
        if (chunkId != other.chunkId) {
            return chunkId < other.chunkId;
        }
        return itemId < other.itemId;
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
     * Gives the tracking id to fall back on for an item that arrived without one, built from
     * the item's identity alone so that neither the record key nor the sink id ends up in a log
     * context
     *
     * @return tracking id naming the item
     */
    public String asTrackingId() {
        return String.format("io:%d-%d-%d", jobId, chunkId, itemId);
    }

    /**
     * Gives the form used to name this item in an outcome an operator reads
     *
     * @return the item's job, chunk and item ids
     */
    public String asItemReference() {
        return String.format("%d/%d/%d", jobId, chunkId, itemId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BatchName)) {
            return false;
        }
        BatchName other = (BatchName) o;
        return sinkId == other.sinkId && jobId == other.jobId && chunkId == other.chunkId
                && itemId == other.itemId
                && (recordKey == null ? other.recordKey == null : recordKey.equals(other.recordKey));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%d-%s-%d-%d-%d", sinkId, recordKey == null ? "" : recordKey,
                jobId, chunkId, itemId);
    }

    private static <T> T header(ConsumedMessage message, JMSHeader header, Class<T> type) {
        T value = header.getHeader(message, type);
        if (value == null) {
            throw new IllegalArgumentException(String.format("Message<%s> has no %s property",
                    message.getMessageId(), header.name));
        }
        return value;
    }
}
