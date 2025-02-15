package dk.dbc.dataio.jobstore.distributed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.jobstore.distributed.tools.KeySetJSONBConverter;
import dk.dbc.dataio.jobstore.distributed.tools.StringSetConverter;
import org.postgresql.util.PGobject;

import java.io.Serial;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private Set<TrackingKey> waitingOn = new HashSet<>();
    private final Set<String> matchKeys;
    private final Set<WaitFor> waitFor;
    private final int submitter;
    private Instant lastModified = Instant.now();
    private int retries = 0;

    public DependencyTracking(TrackingKey key, int sinkId, int submitter, Set<String> sequenceData) {
        this.key = key;
        this.sinkId = sinkId;
        this.submitter = submitter;
        matchKeys = makeKeys(null, sequenceData);
        waitFor = toWaitForIndexSet(sinkId, submitter, matchKeys);
    }

    public DependencyTracking(TrackingKey key, int sinkId, int submitter, String barrierKey, Set<String> sequenceData) {
        this(key, sinkId, submitter, makeKeys(barrierKey, sequenceData));
    }

    public DependencyTracking(TrackingKey key, int sinkId, int submitter) {
        this(key, sinkId, submitter, Set.of());
    }

    public DependencyTracking(ResultSet rs) throws SQLException {
        key = new TrackingKey(rs.getInt("jobid"), rs.getInt("chunkid"));
        sinkId = rs.getInt("sinkid");
        waitingOn = new HashSet<>(new KeySetJSONBConverter().convertToEntityAttribute((PGobject) rs.getObject("waitingon")));
        status = ChunkSchedulingStatus.from(rs.getInt("status"));
        matchKeys = new StringSetConverter().convertToEntityAttribute((PGobject) rs.getObject("matchkeys"));
        priority = rs.getInt("priority");
        submitter = rs.getInt("submitter");
        lastModified = rs.getTimestamp("lastmodified").toInstant();
        retries = rs.getInt("retries");
        waitFor = toWaitForIndexSet(sinkId, submitter, matchKeys);
    }

    public static Set<String> makeKeys(String barrierKey, Set<String> sequenceData) {
        Set<String> keys = sequenceData == null ? new HashSet<>() : new HashSet<>(sequenceData);
        if (barrierKey != null) keys.add(barrierKey);
        return keys;
    }

    public static Set<WaitFor> toWaitForIndexSet(int sinkId, int submitter, Set<String> matchKeys) {
        return matchKeys.stream().map(k -> new WaitFor(sinkId, submitter, k)).collect(Collectors.toSet());
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
    public Set<TrackingKey> getWaitingOn() {
        return waitingOn;
    }

    @Override
    public Set<WaitFor> getWaitFor() {
        return waitFor;
    }

    public DependencyTracking setWaitingOn(Set<TrackingKey> waitingOn) {
        this.waitingOn = waitingOn instanceof HashSet ? waitingOn : new HashSet<>(waitingOn);
        return this;
    }

    @Override
    public Set<String> getMatchKeys() {
        return matchKeys;
    }

    @Override
    public int getSubmitter() {
        return submitter;
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

