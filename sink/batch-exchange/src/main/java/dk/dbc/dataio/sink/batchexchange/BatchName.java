package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.jms.JMSHeader;

/**
 * Name of the batch holding one item's work, carrying everything needed to report that
 * item's delivery once the consumer system has answered.
 * <p>
 * The batch exchange schema offers no other place to put it: {@code batch.id} is assigned
 * by a sequence and {@code entry} belongs to the consumer system's contract, so the name is
 * the one field this sink owns. It holds five fields:
 * <pre>
 * &lt;sinkId&gt;-&lt;recordKey&gt;-&lt;jobId&gt;-&lt;chunkId&gt;-&lt;itemId&gt;
 *
 * 15-870970:12345678-123-4-7
 * 15--123-4-7                  no record key
 * </pre>
 * The record key sits second so that {@link #prefix()} is a literal prefix of every name
 * belonging to one record, which is what lets the sink find a record's staged batches with
 * an indexed range scan rather than a scan of the whole table. It is also the only field
 * that may contain a hyphen, so it is placed where neither end of the parse has to cross
 * it: the sink id is read from the left, the three ids from the right, and whatever lies
 * between them is the record key however it is spelled.
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
     * Creates the name of the batch staging the item the given message carries
     *
     * @param message message carrying the item, whose headers name both the item and the
     *                watermark row its delivery may advance
     * @return BatchName object
     * @throws IllegalArgumentException if the message lacks a header the name needs
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
     * Creates new batch name from given string which must be formatted as
     * [SINK_ID]-[RECORD_KEY]-[JOB_ID]-[CHUNK_ID]-[ITEM_ID]
     *
     * @param name name to be parsed
     * @return BatchName object
     * @throws IllegalArgumentException if given invalid name string
     */
    public static BatchName fromString(String name) throws IllegalArgumentException {
        int sink = name.indexOf('-');
        int item = name.lastIndexOf('-');
        int chunk = item > 0 ? name.lastIndexOf('-', item - 1) : -1;
        int job = chunk > 0 ? name.lastIndexOf('-', chunk - 1) : -1;
        if (sink < 0 || job <= sink) {
            throw new IllegalArgumentException(
                    "Name does not match [SINK_ID]-[RECORD_KEY]-[JOB_ID]-[CHUNK_ID]-[ITEM_ID] pattern: " + name);
        }
        try {
            String recordKey = name.substring(sink + 1, job);
            return new BatchName(
                    Long.parseLong(name.substring(0, sink)),
                    recordKey.isEmpty() ? null : recordKey,
                    Integer.parseInt(name.substring(job + 1, chunk)),
                    Long.parseLong(name.substring(chunk + 1, item)),
                    Short.parseShort(name.substring(item + 1)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid batch name: " + name, e);
        }
    }

    /**
     * Gives the literal prefix shared by the names of every batch staged for this record
     * <p>
     * Matching on it can return more than this record's batches, since one record key can
     * be another with a hyphen and more appended. Compare {@link #getRecordKey()} of each
     * match to tell them apart.
     *
     * @return prefix to match batch names against
     */
    public String prefix() {
        return String.format("%d-%s-", sinkId, recordKey == null ? "" : recordKey);
    }

    /**
     * Gives the LIKE pattern matching every batch name staged for this record
     * <p>
     * A record key is opaque and may contain the LIKE wildcards itself, so the prefix is
     * escaped. The query using this must declare {@code ESCAPE '\\'} to match.
     *
     * @return escaped LIKE pattern for the prefix, with the trailing wildcard appended
     */
    public String likePrefixPattern() {
        return prefix()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }

    /**
     * Whether two names carry versions of the same record
     * <p>
     * Needed because {@link #prefix()} can match a record key that is this one with a hyphen
     * and more appended, so a prefix match is a candidate rather than an answer. Two names
     * without a record key compare equal here, which costs nothing: an item with no record
     * key has no record identity and is never superseded.
     *
     * @param other name to compare against
     * @return true if both names name the same record of the same sink
     */
    public boolean hasSameRecordAs(BatchName other) {
        return sinkId == other.sinkId
                && (recordKey == null ? other.recordKey == null : recordKey.equals(other.recordKey));
    }

    /**
     * Orders two names by the version of the record they carry, as the delivery watermark
     * orders the same three ids
     *
     * @param other name to compare against, for the same record as this one
     * @return true if the other name carries a newer version of the record than this one
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
     * Gives the tracking id to fall back on for an item that arrived without one, built
     * from the item's identity alone so that neither the record key nor the sink id ends up
     * in a log context
     *
     * @return tracking id naming the item
     */
    public String asTrackingId() {
        return String.format("io:%d-%d-%d", jobId, chunkId, itemId);
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
