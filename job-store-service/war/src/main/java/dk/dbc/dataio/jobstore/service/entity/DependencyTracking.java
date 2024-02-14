package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.lang.Hashcode;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerSinkStatus;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Class for tracking chunk dependencies.
 */
public class DependencyTracking implements DependencyTrackingRO, Serializable {
    private static final long serialVersionUID = 1L;
//    static final String SINKID_STATUS_COUNT_RESULT = "SinkIdStatusCountResult";
//    public static final String KEY_RESULT = "DependencyTrackingEntity.Key";
//    public static final String KEY_WAITING_ON_RESULT = "DependencyTrackingEntity.KeyAndWaitingOn";
//    public static final String SINKID_STATUS_COUNT_QUERY = "DependencyTrackingEntity.sinkIdStatusCount";
//    public static final String SINKID_STATUS_COUNT_QUERY_ALL = "DependencyTrackingEntity.sinkIdStatusCountAll";
//    public static final String JOB_COUNT_CHUNK_COUNT_QUERY = "DependencyTrackingEntity.jobCountChunkCount";
//    public static final String RELATED_CHUNKS_QUERY = "DependencyTrackingEntity.relatedChunks";
//    public static final String BY_SINKID_AND_STATE_QUERY = "DependencyTrackingEntity.bySinkIdAndState";
//    public static final String CHUNKS_TO_WAIT_FOR_QUERY = "DependencyTrackingEntity.chunksToWaitFor";
//    public static final String BLOCKED_GROUPED_BY_SINK = "DependencyTrackingEntity.blockedGroupedBySink";
//    public static final String CHUNKS_IN_STATE = "DependencyTrackingEntity.inState";
//    public static final String RESET_STATES_IN_DEPENDENCYTRACKING = "DependencyTrackingEntity.resetStates";
//    public static final String RESET_STATE_IN_DEPENDENCYTRACKING = "DependencyTrackingEntity.resetState";
//    public static final String BY_STATE_AND_LAST_MODIFIED = "DependencyTrackingEntity.bySinkIdAndLastModified";
//    public static final String DELETE_JOB = "DependencyTrackingEntity.deleteJob";

    private Key key;
    private int sinkid;

//    @Column(nullable = false)
//    @Convert(converter = ChunkSchedulingStatusConverter.class)
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

    private Timestamp lastModified = new Timestamp(new Date().getTime());
    private int retries = 0;

    public DependencyTracking(ChunkEntity chunk, int sinkId, String extraKey) {
        this.key = new Key(chunk.getKey());
        this.sinkid = sinkId;
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
        lastModified = rs.getTimestamp("lastmodified");
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
    public int getSinkid() {
        return sinkid;
    }

    public DependencyTracking setSinkid(int sinkid) {
        this.sinkid = sinkid;
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
        lastModified = new Timestamp(new Date().getTime());
    }

    @Override
    public Timestamp getLastModified() {
        return lastModified;
    }

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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DependencyTracking that = (DependencyTracking) o;

        if (sinkid != that.sinkid) {
            return false;
        }
        if (submitter != that.submitter) {
            return false;
        }
        if (priority != that.priority) {
            return false;
        }
        if (key != null ? !key.equals(that.key) : that.key != null) {
            return false;
        }
        if (status != that.status) {
            return false;
        }
        if (waitingOn != null ? !waitingOn.equals(that.waitingOn) : that.waitingOn != null) {
            return false;
        }
        return matchKeys != null ? matchKeys.equals(that.matchKeys) : that.matchKeys == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + sinkid;
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

//    @Embeddable
    public static class Key implements Serializable {
        private static final long serialVersionUID = -5575195152198835462L;
//        @Column(name = "jobid")
        private int jobId;

//        @Column(name = "chunkid")
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
            this.jobId = chunk.getJobId();
            this.chunkId = (int) chunk.getChunkId();
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

    public enum ChunkSchedulingStatus {
        READY_FOR_PROCESSING(1, s -> s.getProcessingStatus().ready.incrementAndGet()),   // chunk is ready for processing
        QUEUED_FOR_PROCESSING(2, READY_FOR_PROCESSING, s -> s.getProcessingStatus().enqueued.incrementAndGet()),  // chunk is sent to processor JMS queue
        BLOCKED(3, null),                // chunk is waiting for other chunk(s) to return from sink
        READY_FOR_DELIVERY(4, s -> s.getDeliveringStatus().ready.incrementAndGet()),     // chunk is ready for delivery
        QUEUED_FOR_DELIVERY(5, READY_FOR_DELIVERY, s -> s.getDeliveringStatus().enqueued.incrementAndGet());     // chunk is sent to sink JMS queue

        public final Integer value;
        public final ChunkSchedulingStatus resend;
        private static final Map<Integer, ChunkSchedulingStatus> VALUE_MAP = Arrays.stream(values()).collect(Collectors.toMap(c -> c.value, c -> c));
        private final Consumer<JobSchedulerSinkStatus> counter;

        ChunkSchedulingStatus(Integer value, Consumer<JobSchedulerSinkStatus> counter) {
            this.value = value;
            this.counter = counter;
            resend = null;
        }

        ChunkSchedulingStatus(Integer value, ChunkSchedulingStatus resend, Consumer<JobSchedulerSinkStatus> counter) {
            this.value = value;
            this.resend = resend;
            this.counter = counter;
        }

        public void countSinkStatus(JobSchedulerSinkStatus status) {
            if(counter != null) counter.accept(status);
        }

        public static ChunkSchedulingStatus from(int value) {
            return VALUE_MAP.get(value);
        }
    }
}

