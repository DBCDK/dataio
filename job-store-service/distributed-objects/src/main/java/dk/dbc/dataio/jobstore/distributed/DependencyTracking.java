package dk.dbc.dataio.jobstore.distributed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
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
    private boolean gateOpen = true;

    public DependencyTracking(TrackingKey key, int sinkId, int submitter) {
        this.key = key;
        this.sinkId = sinkId;
        this.submitter = submitter;
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
     */
    @Override
    public boolean isTermination() {
        return termination;
    }

    public DependencyTracking setTermination(boolean termination) {
        this.termination = termination;
        return this;
    }

    /**
     * Says whether this chunk's gate is open, so whether it may be delivered.
     * <p>
     * Read from {@code gate_open}, which is the authority. This is a snapshot of the column as it
     * stood when the row was selected, and it is used the way {@code status} is used: within the
     * transaction that read it, by a dispatch path that goes on to claim the chunk with a validated
     * status change. The gate is written by four sites over a chunk's life, so a value carried
     * across transactions or held past the dispatch decision says nothing about the row.
     * <p>
     * Defaults to open, matching {@code NOT NULL DEFAULT TRUE} on the column, so an object built
     * for a chunk whose gate nobody has closed reads the same as its row.
     */
    @Override
    public boolean isGateOpen() {
        return gateOpen;
    }

    public DependencyTracking setGateOpen(boolean gateOpen) {
        this.gateOpen = gateOpen;
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

    public DependencyTracking withLastModified(Instant lastModified) {
        this.lastModified = lastModified;
        return this;
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
