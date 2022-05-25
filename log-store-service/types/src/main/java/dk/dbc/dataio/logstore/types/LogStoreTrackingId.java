package dk.dbc.dataio.logstore.types;

import dk.dbc.invariant.InvariantUtil;

/**
 * Log store tracking ID with string representation
 * on the form 'JOB_ID-CHUNK_ID-ITEM_ID'
 */
public class LogStoreTrackingId {
    public static final String LOG_STORE_TRACKING_ID_MDC_KEY = "logStoreTrackingId";
    public static final String LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY = "logStoreTrackingIdCommit";

    private final String trackingId;
    private final String jobId;
    private final long chunkId;
    private final long itemId;

    /**
     * Constructs log-store tracking ID by parsing given string
     *
     * @param trackingId string to be parsed into a tracking ID
     * @throws NullPointerException     if given null-valued trackingId argument
     * @throws IllegalArgumentException if given empty-valued or otherwise invalid trackingId argument
     */
    public LogStoreTrackingId(String trackingId) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(trackingId, "trackingId");
        final String[] parts = trackingId.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid logstore tracking ID: " + trackingId);
        }
        this.trackingId = trackingId;
        jobId = InvariantUtil.checkNotNullNotEmptyOrThrow(parts[0], "Invalid logstore tracking ID jobId part");
        chunkId = Long.parseLong(parts[1]);
        itemId = Long.parseLong(parts[2]);
    }

    public String getTrackingId() {
        return trackingId;
    }

    public String getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    public long getItemId() {
        return itemId;
    }

    @Override
    public String toString() {
        return trackingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogStoreTrackingId that = (LogStoreTrackingId) o;

        if (!trackingId.equals(that.trackingId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return trackingId.hashCode();
    }

    /**
     * Factory method for tracking ID cration
     *
     * @param jobId   job ID part
     * @param chunkId chunk ID part
     * @param itemId  item ID part
     * @return LogStoreTrackingId instance
     * @throws NullPointerException     if given null-valued jobId argument
     * @throws IllegalArgumentException if given empty-valued jobId argument
     */
    public static LogStoreTrackingId create(String jobId, long chunkId, long itemId)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(jobId, "jobId");
        return new LogStoreTrackingId(String.format("%s-%s-%s", jobId, chunkId, itemId));
    }
}
