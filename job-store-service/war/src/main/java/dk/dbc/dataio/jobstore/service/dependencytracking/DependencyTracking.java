package dk.dbc.dataio.jobstore.service.dependencytracking;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.lang.Hashcode;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.KeySetJSONBConverter;
import dk.dbc.dataio.jobstore.service.entity.StringSetConverter;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class for tracking chunk dependencies.
 */
public class DependencyTracking implements DependencyTrackingRO, Serializable {
    private static final long serialVersionUID = 1L;

    private Key key;
    private int sinkId;
    private ChunkSchedulingStatus status = ChunkSchedulingStatus.READY_FOR_PROCESSING;

    private int priority;

//    @Column(columnDefinition = "jsonb")
//    @Mutable
//    @Convert(converter = KeySetJSONBConverter.class)
    private Set<Key> waitingOn;

//    @Column(columnDefinition = "jsonb", nullable = false)
//    @Convert(converter = StringSetConverter.class)
    private Set<String> matchKeys;

//    @Convert(converter = IntegerArrayToPgIntArrayConverter.class)
    private Integer[] hashes;

    private int submitter;

    private Instant lastModified = Instant.now();
    private int retries = 0;

    public DependencyTracking(ChunkEntity chunk, int sinkId, String extraKey) {
        this.key = new Key(chunk.getKey());
        this.sinkId = sinkId;
        if (chunk.getSequenceAnalysisData() != null) {
            matchKeys = new HashSet<>(chunk.getSequenceAnalysisData().getData());
        } else {
            matchKeys = new HashSet<>();
        }
        if (extraKey != null) {
            matchKeys.add(extraKey);
        }
        hashes = computeHashes(matchKeys);
    }

    public DependencyTracking() {
    }

    public DependencyTracking(int jobId, int chunkId, PGobject waitingOn) {
        key = new Key(jobId, chunkId);
        this.waitingOn = new KeySetJSONBConverter().convertToEntityAttribute(waitingOn);
    }

    public DependencyTracking(ResultSet rs) throws SQLException {
        key = new Key(rs.getInt("jobid"), rs.getInt("chunkid"));
        waitingOn = new KeySetJSONBConverter().convertToEntityAttribute((PGobject) rs.getObject("waitingon"));
        status = ChunkSchedulingStatus.from(rs.getInt("status"));
        matchKeys = new StringSetConverter().convertToEntityAttribute((PGobject) rs.getObject("matchkeys"));
        hashes = computeHashes(matchKeys);
        priority = rs.getInt("priority");
        submitter = rs.getInt("submitter");
        lastModified = rs.getTimestamp("lastmodified").toInstant();
        retries = rs.getInt("retries");
    }


    @Override
    public Key getKey() {
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

    public DependencyTracking setSinkId(int sinkId) {
        this.sinkId = sinkId;
        return this;
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
    public Set<Key> getWaitingOn() {
        return waitingOn;
    }

    public DependencyTracking setWaitingOn(Set<Key> waitingOn) {
        this.waitingOn = waitingOn;
        return this;
    }

    public DependencyTracking setWaitingOn(List<Key> chunksToWaitFor) {
        this.waitingOn = new HashSet<>(chunksToWaitFor);
        return this;
    }

    @Override
    public Set<String> getMatchKeys() {
        return matchKeys;
    }

    public DependencyTracking setMatchKeys(Set<String> matchKeys) {
        this.matchKeys = matchKeys;
        if (this.matchKeys != null) {
            this.hashes = computeHashes(this.matchKeys);
        }
        return this;
    }

    @Override
    public Integer[] getHashes() {
        return hashes;
    }

    @Override
    public int getSubmitterNumber() {
        return submitter;
    }

    public DependencyTracking setSubmitterNumber(int submitterNumber) {
        this.submitter = submitterNumber;
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
    public Instant getLastModified() {
        return lastModified;
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
            // There is an autoboxing penalty being paid here for int -> Integer
            hashes[i++] = Hashcode.of(str);
        }
        return hashes;
    }

    public static class Key implements Serializable {
        private static final long serialVersionUID = -5575195152198835462L;
        private int jobId;
        private int chunkId;

        /* Private constructor in order to keep class static */
        private Key() {
        }

        public Key(int jobId, int chunkId) {
            this.jobId = jobId;
            this.chunkId = chunkId;
        }

        public Key(ChunkEntity.Key chunkKey) {
            this.jobId = chunkKey.getJobId();
            this.chunkId = chunkKey.getId();
        }

        public Key(long jobId, long chunkId) {
            this.jobId = (int) jobId;
            this.chunkId = (int) chunkId;
        }

        public Key(Chunk chunk) {
            jobId = chunk.getJobId();
            chunkId = (int) chunk.getChunkId();
        }

        public int getChunkId() {
            return chunkId;
        }

        public int getJobId() {
            return jobId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return jobId == key.jobId &&
                    chunkId == key.chunkId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, chunkId);
        }

        @Override
        public String toString() {
            return "DependencyTrackingEntity.Key{" +
                    "jobId=" + jobId +
                    ", chunkId=" + chunkId +
                    '}';
        }

        public String toChunkIdentifier() {
            return jobId + "/" + chunkId;
        }
    }

}

