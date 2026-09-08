package dk.dbc.dataio.jobstore.distributed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Class for tracking chunk dependencies.
 */
public class DependencyTracking implements DependencyTrackingRO, Serializable, Comparable<DependencyTracking> {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final ZoneId ZONE_ID_DK = ZoneId.of("Europe/Copenhagen");

    private final TrackingKey key;
    private final int sinkId;
    private ChunkSchedulingStatus status = ChunkSchedulingStatus.READY_FOR_PROCESSING;
    private int priority;
    private final int submitter;
    private Instant lastModified = Instant.now();
    private int retries = 0;
    private boolean termination = false;

    public DependencyTracking(TrackingKey key, int sinkId, int submitter) {
        this.key = key;
        this.sinkId = sinkId;
        this.submitter = submitter;
    }

    public DependencyTracking(ResultSet rs) throws SQLException {
        key = new TrackingKey(rs.getInt("jobid"), rs.getInt("chunkid"));
        sinkId = rs.getInt("sinkid");
        status = ChunkSchedulingStatus.from(rs.getInt("status"));
        priority = rs.getInt("priority");
        submitter = rs.getInt("submitter");
        lastModified = rs.getTimestamp("lastmodified").toInstant();
        retries = rs.getInt("retries");
        termination = rs.getBoolean("is_termination");
    }

    @Override
    public TrackingKey getKey() {
        return key;
    }

    @Override
    public int getSinkId() {
        return sinkId;
    }

    @Override
    public ChunkSchedulingStatus getStatus() {
        return status;
    }

    public DependencyTracking setStatus(ChunkSchedulingStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public int getSubmitter() {
        return submitter;
    }

    /**
     * Says whether this chunk is its job's termination chunk.
     * <p>
     * The {@code is_termination} column is the authority and this is a copy of it, which is sound
     * because the value is decided when the row is created and never changes afterwards. It is set
     * on the entry the termination chunk is scheduled with, and read back from the column whenever
     * an entry is loaded from the table.
     * <p>
     * {@code gate_open} deliberately has no counterpart here. It is written by four sites over a
     * chunk's life, so a copy on this object could be stale, and a stale open gate dispatches a
     * job's end-of-job work ahead of the data it summarises.
     */
    @Override
    public boolean isTermination() {
        return termination;
    }

    public DependencyTracking setTermination(boolean termination) {
        this.termination = termination;
        return this;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public DependencyTracking setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public void updateLastModified() {
        lastModified = Instant.now();
    }

    @Override
    @JsonIgnore
    public Instant getLastModified() {
        return lastModified;
    }

    @JsonProperty("lastModified")
    public String getLastModifiedFormatted() {
        return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(getLastModified().atZone(ZONE_ID_DK));
    }

    @Override
    public int getRetries() {
        return retries;
    }

    public int resend() {
        ChunkSchedulingStatus resend = status.resend;
        if(resend != null) {
            setStatus(resend);
            ++retries;
        }
        return retries;
    }

    public DependencyTracking withRetries(int retries) {
        this.retries = retries;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DependencyTracking that = (DependencyTracking) o;
        return priority == that.priority && submitter == that.submitter && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, priority, submitter);
    }

    @Override
    public String toString() {
        return "DependencyTracking{" +
                "key=" + key +
                ", sinkId=" + sinkId +
                ", status=" + status +
                ", submitter=" + submitter +
                '}';
    }

    @Override
    public int compareTo(DependencyTracking o) {
        int result = Integer.compare(o.priority, priority);
        if(result != 0) return result;
        return key.compareTo(o.getKey());
    }
}
