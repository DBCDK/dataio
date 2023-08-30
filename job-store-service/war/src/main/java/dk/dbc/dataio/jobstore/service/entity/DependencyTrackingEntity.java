package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jpa.converter.IntegerArrayToPgIntArrayConverter;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.lang.Hashcode;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import org.eclipse.persistence.annotations.Mutable;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class for tracking chunk dependencies.
 */
@Entity
@Table(name = "dependencytracking")
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = DependencyTrackingEntity.KEY_RESULT,
                classes = {
                        @ConstructorResult(
                                targetClass = DependencyTrackingEntity.Key.class,
                                columns = {
                                        @ColumnResult(name = "jobId"),
                                        @ColumnResult(name = "chunkId")})}),
        @SqlResultSetMapping(
                name = DependencyTrackingEntity.KEY_WAITING_ON_RESULT,
                classes = {
                        @ConstructorResult(
                                targetClass = DependencyTrackingEntity.class,
                                columns = {
                                        @ColumnResult(name = "jobId"),
                                        @ColumnResult(name = "chunkId"),
                                        @ColumnResult(name = "waitingon")})}),
        @SqlResultSetMapping(
                name = DependencyTrackingEntity.SINKID_STATUS_COUNT_RESULT,
                classes = {
                        @ConstructorResult(
                                targetClass = SinkIdStatusCountResult.class,
                                columns = {
                                        @ColumnResult(name = "sinkId"),
                                        @ColumnResult(name = "Status"),
                                        @ColumnResult(name = "count")})})
})
@NamedNativeQueries({
        @NamedNativeQuery(name = DependencyTrackingEntity.SINKID_STATUS_COUNT_QUERY_ALL,
                query = "SELECT sinkid, status, count(*) FROM dependencytracking GROUP BY sinkid, status ORDER BY sinkid, status",
                resultSetMapping = DependencyTrackingEntity.SINKID_STATUS_COUNT_RESULT),
        @NamedNativeQuery(name = DependencyTrackingEntity.SINKID_STATUS_COUNT_QUERY,
                query = "SELECT sinkid, status, count(*) FROM dependencytracking WHERE sinkid=? GROUP BY sinkid, status ORDER BY sinkid, status",
                resultSetMapping = DependencyTrackingEntity.SINKID_STATUS_COUNT_RESULT),
        @NamedNativeQuery(name = DependencyTrackingEntity.JOB_COUNT_CHUNK_COUNT_QUERY,
                query = "SELECT count(DISTINCT jobid) AS numberOfJobs, count(jobid) AS NumberOfChunks FROM dependencytracking WHERE sinkid = ?"),
        @NamedNativeQuery(name = DependencyTrackingEntity.RELATED_CHUNKS_QUERY,
                query = "SELECT jobId, chunkId FROM dependencyTracking WHERE sinkId=? AND (jobId=? or matchKeys @> '[\"?\"]' ) ORDER BY jobId, chunkId FOR NO KEY UPDATE",
                resultSetMapping = DependencyTrackingEntity.KEY_RESULT),
        @NamedNativeQuery(name = DependencyTrackingEntity.CHUNKS_TO_WAIT_FOR_QUERY,
                // Using the array overlap (&&) operator, which returns true
                // if the two argument arrays have at least one common element, and
                // certainly is a lot faster than OR'ing together 'matchKeys @>' expressions.
                query = "SELECT jobid, chunkid, waitingon FROM dependencyTracking WHERE sinkId = ? AND submitter = ? AND hashes && ?::INTEGER[] ORDER BY jobId, chunkId FOR NO KEY UPDATE",
                resultSetMapping = DependencyTrackingEntity.KEY_WAITING_ON_RESULT),
        @NamedNativeQuery(name = "DependencyTrackingEntity.blockedGroupedBySink", query = "SELECT sinkid, 3 as status, count(*) from dependencyTracking where status = 3 group by sinkid",
                resultSetMapping = DependencyTrackingEntity.SINKID_STATUS_COUNT_RESULT)
})
@NamedQueries({
        @NamedQuery(name = DependencyTrackingEntity.BY_SINKID_AND_STATE_QUERY,
                query = "SELECT e FROM DependencyTrackingEntity e WHERE e.sinkid=:sinkId AND e.status=:state ORDER BY e.priority DESC, e.key.jobId, e.key.chunkId"),
        @NamedQuery(name = DependencyTrackingEntity.BY_STATE_AND_LAST_MODIFIED,
                query = "SELECT e FROM DependencyTrackingEntity e WHERE e.lastModified < :date AND e.status = :status"),
        @NamedQuery(name = DependencyTrackingEntity.CHUNKS_IN_STATE,
                query = "SELECT count(e) FROM DependencyTrackingEntity e WHERE e.status = :status"),
        @NamedQuery(name = DependencyTrackingEntity.RESET_STATES_IN_DEPENDENCYTRACKING,
                query = "UPDATE DependencyTrackingEntity e SET e.status = :toStatus WHERE e.status = :fromStatus"),
        @NamedQuery(name = DependencyTrackingEntity.RESET_STATE_IN_DEPENDENCYTRACKING,
                query = "UPDATE DependencyTrackingEntity e SET e.status = :toStatus WHERE e.status = :fromStatus AND e.key.jobId in :jobIds"),
        @NamedQuery(name = DependencyTrackingEntity.DELETE_JOB,
                query = "DELETE FROM DependencyTrackingEntity e WHERE e.key.jobId=:jobId")
})
public class DependencyTrackingEntity {
    static final String SINKID_STATUS_COUNT_RESULT = "SinkIdStatusCountResult";
    public static final String KEY_RESULT = "DependencyTrackingEntity.Key";
    public static final String KEY_WAITING_ON_RESULT = "DependencyTrackingEntity.KeyAndWaitingOn";
    public static final String SINKID_STATUS_COUNT_QUERY = "DependencyTrackingEntity.sinkIdStatusCount";
    public static final String SINKID_STATUS_COUNT_QUERY_ALL = "DependencyTrackingEntity.sinkIdStatusCountAll";
    public static final String JOB_COUNT_CHUNK_COUNT_QUERY = "DependencyTrackingEntity.jobCountChunkCount";
    public static final String RELATED_CHUNKS_QUERY = "DependencyTrackingEntity.relatedChunks";
    public static final String BY_SINKID_AND_STATE_QUERY = "DependencyTrackingEntity.bySinkIdAndState";
    public static final String CHUNKS_TO_WAIT_FOR_QUERY = "DependencyTrackingEntity.chunksToWaitFor";
    public static final String BLOCKED_GROUPED_BY_SINK = "DependencyTrackingEntity.blockedGroupedBySink";
    public static final String CHUNKS_IN_STATE = "DependencyTrackingEntity.inState";
    public static final String RESET_STATES_IN_DEPENDENCYTRACKING = "DependencyTrackingEntity.resetStates";
    public static final String RESET_STATE_IN_DEPENDENCYTRACKING = "DependencyTrackingEntity.resetState";
    public static final String BY_STATE_AND_LAST_MODIFIED = "DependencyTrackingEntity.bySinkIdAndLastModified";
    public static final String DELETE_JOB = "DependencyTrackingEntity.deleteJob";

    public DependencyTrackingEntity(ChunkEntity chunk, int sinkId, String extraKey) {
        this.key = new Key(chunk.getKey());
        this.sinkid = sinkId;
        if (chunk.getSequenceAnalysisData() != null) {
            this.matchKeys = new HashSet<>(chunk.getSequenceAnalysisData().getData());
        } else {
            this.matchKeys = new HashSet<>();
        }
        if (extraKey != null) {
            this.matchKeys.add(extraKey);
        }
        this.hashes = computeHashes(this.matchKeys);
    }

    public DependencyTrackingEntity() {
    }

    public DependencyTrackingEntity(int jobId, int chunkId, PGobject waitingOn) {
        key = new Key(jobId, chunkId);
        this.waitingOn = new KeySetJSONBConverter().convertToEntityAttribute(waitingOn);
    }

    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    public enum ChunkSchedulingStatus {
        READY_FOR_PROCESSING(1),   // chunk is ready for processing
        QUEUED_FOR_PROCESSING(2, READY_FOR_PROCESSING),  // chunk is sent to processor JMS queue
        BLOCKED(3),                // chunk is waiting for other chunk(s) to return from sink
        READY_FOR_DELIVERY(4),     // chunk is ready for delivery
        QUEUED_FOR_DELIVERY(5, READY_FOR_DELIVERY);     // chunk is sent to sink JMS queue

        public final Integer value;
        public final ChunkSchedulingStatus resend;

        ChunkSchedulingStatus(Integer value) {
            this.value = value;
            resend = null;
        }

        ChunkSchedulingStatus(Integer value, ChunkSchedulingStatus resend) {
            this.value = value;
            this.resend = resend;
        }
    }

    @EmbeddedId
    private Key key;

    @Column(nullable = false)
    private int sinkid;

    @Column(nullable = false)
    @Convert(converter = ChunkSchedulingStatusConverter.class)
    private ChunkSchedulingStatus status = ChunkSchedulingStatus.READY_FOR_PROCESSING;

    private int priority;

    @Column(columnDefinition = "jsonb")
    @Mutable
    @Convert(converter = KeySetJSONBConverter.class)
    private Set<Key> waitingOn;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = StringSetConverter.class)
    private Set<String> matchKeys;

    @Convert(converter = IntegerArrayToPgIntArrayConverter.class)
    private Integer[] hashes;

    private int submitter;

    @Column(nullable = false)
    private Timestamp lastModified = new Timestamp(new Date().getTime());
    @Column
    private int retries = 0;

    public Key getKey() {
        return key;
    }

    public DependencyTrackingEntity setKey(Key key) {
        this.key = key;
        return this;
    }

    public int getSinkid() {
        return sinkid;
    }

    public DependencyTrackingEntity setSinkid(int sinkid) {
        this.sinkid = sinkid;
        return this;
    }

    public ChunkSchedulingStatus getStatus() {
        return status;
    }

    public DependencyTrackingEntity setStatus(ChunkSchedulingStatus status) {
        this.status = status;
        return this;
    }

    public Set<Key> getWaitingOn() {
        return waitingOn;
    }

    public DependencyTrackingEntity setWaitingOn(Set<Key> waitingOn) {
        this.waitingOn = waitingOn;
        return this;
    }

    public DependencyTrackingEntity setWaitingOn(List<Key> chunksToWaitFor) {
        this.waitingOn = new HashSet<>(chunksToWaitFor);
        return this;
    }

    public Set<String> getMatchKeys() {
        return matchKeys;
    }

    public DependencyTrackingEntity setMatchKeys(Set<String> matchKeys) {
        this.matchKeys = matchKeys;
        if (this.matchKeys != null) {
            this.hashes = computeHashes(this.matchKeys);
        }
        return this;
    }

    public Integer[] getHashes() {
        return hashes;
    }

    public int getSubmitterNumber() {
        return submitter;
    }

    public DependencyTrackingEntity setSubmitterNumber(int submitterNumber) {
        this.submitter = submitterNumber;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public DependencyTrackingEntity setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    @PreUpdate
    public void updateLastModified() {
        lastModified = new Timestamp(new Date().getTime());
    }

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

    public DependencyTrackingEntity withRetries(int retries) {
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

        DependencyTrackingEntity that = (DependencyTrackingEntity) o;

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

    @Embeddable
    public static class Key implements Serializable {
        private static final long serialVersionUID = -5575195152198835462L;
        @Column(name = "jobid")
        private int jobId;

        @Column(name = "chunkid")
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
            this.jobId = (int) chunk.getJobId();
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
}

