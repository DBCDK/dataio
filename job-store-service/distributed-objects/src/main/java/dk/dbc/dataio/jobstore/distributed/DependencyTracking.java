package dk.dbc.dataio.jobstore.distributed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.lang.Hashcode;
import dk.dbc.dataio.jobstore.distributed.tools.KeySetJSONBConverter;
import dk.dbc.dataio.jobstore.distributed.tools.StringSetConverter;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class for tracking chunk dependencies.
 */
public class DependencyTracking implements DependencyTrackingRO, Serializable {
    private static final long serialVersionUID = 1L;
    private static final ZoneId ZONE_ID_DK = ZoneId.of("Europe/Copenhagen");

    private final TrackingKey key;
    private final int sinkId;
    private ChunkSchedulingStatus status = ChunkSchedulingStatus.READY_FOR_PROCESSING;
    private int priority;
    private Set<TrackingKey> waitingOn;
    private Set<String> matchKeys;
    private Integer[] hashes;
    private int submitter;
    private Instant lastModified = Instant.now();
    private int retries = 0;

    public DependencyTracking(int jobId, int chunkId, int sinkId, String extraKey, Set<String> sequenceData) {
        this(new TrackingKey(jobId, chunkId), sinkId);
        if (sequenceData != null) {
            matchKeys = new HashSet<>(sequenceData);
        } else {
            matchKeys = new HashSet<>();
        }
        if (extraKey != null) {
            matchKeys.add(extraKey);
        }
        hashes = computeHashes(matchKeys);
    }

    public DependencyTracking(int jobId, int chunkId, int sinkId, PGobject waitingOn) {
        key = new TrackingKey(jobId, chunkId);
        this.sinkId = sinkId;
        this.waitingOn = new HashSet<>(new KeySetJSONBConverter().convertToEntityAttribute(waitingOn));
    }

    public DependencyTracking(TrackingKey key, int sinkId) {
        this.key = key;
        this.sinkId = sinkId;
        waitingOn = new HashSet<>();
    }

    public DependencyTracking(ResultSet rs) throws SQLException {
        key = new TrackingKey(rs.getInt("jobid"), rs.getInt("chunkid"));
        sinkId = rs.getInt("sinkid");
        waitingOn = new HashSet<>(new KeySetJSONBConverter().convertToEntityAttribute((PGobject) rs.getObject("waitingon")));
        status = ChunkSchedulingStatus.from(rs.getInt("status"));
        setMatchKeys(new StringSetConverter().convertToEntityAttribute((PGobject) rs.getObject("matchkeys")));
        priority = rs.getInt("priority");
        submitter = rs.getInt("submitter");
        lastModified = rs.getTimestamp("lastmodified").toInstant();
        retries = rs.getInt("retries");
    }


    @Override
    public TrackingKey getKey() {
        return key;
    }

//    public DependencyTracking setKey(Key key) {
//        this.key = key;
//        return this;
//    }

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

    public DependencyTracking setWaitingOn(Set<TrackingKey> waitingOn) {
        this.waitingOn = new HashSet<>(waitingOn);
        return this;
    }

    public DependencyTracking setWaitingOn(List<TrackingKey> chunksToWaitFor) {
        this.waitingOn = new HashSet<>(chunksToWaitFor);
        return this;
    }

    public DependencyTracking setWaitingOn(PGobject waitingOn) {
        this.waitingOn = new KeySetJSONBConverter().convertToEntityAttribute(waitingOn);
        return this;
    }

    @Override
    public Set<String> getMatchKeys() {
        return matchKeys;
    }

    public DependencyTracking setMatchKeys(Set<String> matchKeys) {
        this.matchKeys = matchKeys;
        if (this.matchKeys != null) {
            hashes = computeHashes(this.matchKeys);
        }
        return this;
    }

    @Override
    public Integer[] getHashes() {
        return hashes;
    }

    @Override
    public int getSubmitter() {
        return submitter;
    }

    public DependencyTracking setSubmitter(int submitter) {
        this.submitter = submitter;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyTracking that = (DependencyTracking) o;
        return sinkId == that.sinkId && priority == that.priority && submitter == that.submitter &&
                Objects.equals(key, that.key) && status == that.status && Objects.equals(waitingOn, that.waitingOn) && Objects.equals(matchKeys, that.matchKeys);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + sinkId;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + submitter;
        result = 31 * result + priority;
        result = 31 * result + (waitingOn != null ? waitingOn.hashCode() : 0);
        result = 31 * result + (matchKeys != null ? matchKeys.hashCode() : 0);
        return result;
    }

    private static Integer[] computeHashes(Set<String> strings) {
        final Integer[] hashes = new Integer[strings.size()];
        int i = 0;
        for (String str : strings) {
            hashes[i++] = Hashcode.of(str);
        }
        return hashes;
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
}

