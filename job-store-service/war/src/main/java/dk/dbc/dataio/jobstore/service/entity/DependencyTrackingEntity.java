/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Chunk;
import org.eclipse.persistence.annotations.Mutable;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
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
                                targetClass = dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key.class,
                                columns = {
                                        @ColumnResult(name = "jobId"),
                                        @ColumnResult(name = "chunkId"),})}),
        @SqlResultSetMapping(
                name = DependencyTrackingEntity.SINKID_STATUS_COUNT_RESULT,
                classes = {
                        @ConstructorResult(
                                targetClass = dk.dbc.dataio.jobstore.service.entity.SinkIdStatusCountResult.class,
                                columns = {
                                        @ColumnResult(name = "sinkId"),
                                        @ColumnResult(name = "Status"),
                                        @ColumnResult(name = "count"),})})
})
@NamedNativeQueries({
        @NamedNativeQuery(name = DependencyTrackingEntity.SINKID_STATUS_COUNT_QUERY,
                query = "SELECT sinkid, status, count(*) FROM dependencytracking GROUP BY sinkid, status ORDER BY sinkid, status",
                resultSetMapping = DependencyTrackingEntity.SINKID_STATUS_COUNT_RESULT),
        @NamedNativeQuery(name = DependencyTrackingEntity.JOB_COUNT_CHUNK_COUNT_QUERY,
                query = "SELECT count(DISTINCT jobid) AS numberOfJobs, count(jobid) AS NumberOfChunks FROM dependencytracking WHERE sinkid = ?"),
        @NamedNativeQuery(name = DependencyTrackingEntity.RELATED_CHUNKS_QUERY,
                query = "SELECT jobId, chunkId FROM dependencyTracking WHERE sinkId=? AND (jobId=? or matchKeys @> '[\"?\"]' ) ORDER BY jobId, chunkId FOR NO KEY UPDATE",
                resultSetMapping = DependencyTrackingEntity.KEY_RESULT),
})
@NamedQueries({
        @NamedQuery(name = DependencyTrackingEntity.BY_SINKID_AND_STATE_QUERY,
                query = "SELECT e FROM DependencyTrackingEntity e WHERE e.sinkid=:sinkId AND e.status=:state ORDER BY e.priority, e.key.jobId, e.key.chunkId")
})
public class DependencyTrackingEntity {
    static final String SINKID_STATUS_COUNT_RESULT = "SinkIdStatusCountResult";
    public static final String KEY_RESULT = "DependencyTrackingEntity.Key";
    public static final String SINKID_STATUS_COUNT_QUERY = "DependencyTrackingEntity.sinkIdStatusCount";
    public static final String JOB_COUNT_CHUNK_COUNT_QUERY = "DependencyTrackingEntity.jobCountChunkCount";
    public static final String RELATED_CHUNKS_QUERY = "DependencyTrackingEntity.relatedChunks";
    public static final String BY_SINKID_AND_STATE_QUERY = "DependencyTrackingEntity.bySinkIdAndState";

    public DependencyTrackingEntity(ChunkEntity chunk, int sinkId, String extraKey) {
        this.key = new Key( chunk.getKey());
        this.sinkid= sinkId;
        if( chunk.getSequenceAnalysisData() != null) {
            this.matchKeys = new HashSet<>(chunk.getSequenceAnalysisData().getData());
        } else {
            this.matchKeys = new HashSet<>();
        }
        if (extraKey != null) {
            this.matchKeys.add( extraKey );
        }
    }

    public DependencyTrackingEntity() {}

    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    public enum ChunkSchedulingStatus {
        READY_FOR_PROCESSING,   // chunk is ready for processing
        QUEUED_FOR_PROCESSING,  // chunk is sent to processor JMS queue
        BLOCKED,                // chunk is waiting for other chunk(s) to return from sink
        READY_FOR_DELIVERY,     // chunk is ready for delivery
        QUEUED_FOR_DELIVERY     // chunk is sent to sink JMS queue
    }

    @EmbeddedId
    private Key key;

    @Column(nullable = false)
    private int sinkid;

    @Column(nullable = false)
    @Convert(converter = ChunkSchedulingStatusConverter.class)
    private ChunkSchedulingStatus status = ChunkSchedulingStatus.READY_FOR_PROCESSING;

    private int priority;

    @Column(columnDefinition = "jsonb" )
    @Mutable
    @Convert(converter = KeySetJSONBConverter.class)
    private Set<Key> waitingOn;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = KeySetJSONBConverter.class)
    private Set<Key> blocking;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = StringSetConverter.class)
    private Set<String> matchKeys;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public int getSinkid() {
        return sinkid;
    }

    public void setSinkid(int sinkid) {
        this.sinkid = sinkid;
    }

    public ChunkSchedulingStatus getStatus() {
        return status;
    }

    public void setStatus(ChunkSchedulingStatus status) {
        this.status = status;
    }

    public Set<Key> getWaitingOn() {
        return waitingOn;
    }

    public void setWaitingOn(Set<Key> waitingOn) {
        this.waitingOn = waitingOn;
    }

    public void setWaitingOn(List<Key> chunksToWaitFor) {
        this.waitingOn = new HashSet<>(chunksToWaitFor);
    }


    public Set<Key> getBlocking() {
        return blocking;
    }

    public void setBlocking(Set<Key> blockedBy) {
        this.blocking = blockedBy;
    }

    public Set<String> getMatchKeys() {
        return matchKeys;
    }

    public void setMatchKeys(Set<String> matchKeys) {
        this.matchKeys = matchKeys;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
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
        if (blocking != null ? !blocking.equals(that.blocking) : that.blocking != null) {
            return false;
        }
        return matchKeys != null ? matchKeys.equals(that.matchKeys) : that.matchKeys == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + sinkid;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + priority;
        result = 31 * result + (waitingOn != null ? waitingOn.hashCode() : 0);
        result = 31 * result + (blocking != null ? blocking.hashCode() : 0);
        result = 31 * result + (matchKeys != null ? matchKeys.hashCode() : 0);
        return result;
    }

    @Embeddable
    public static class Key {
        @Column(name = "jobid")
        private int jobId;

        @Column(name = "chunkid")
        private int chunkId;


        /* Private constructor in order to keep class static */
        private Key(){}

        public Key(int jobId, int chunkId ) {
            this.jobId = jobId;
            this.chunkId = chunkId;
        }

        public Key(ChunkEntity.Key chunkKey) {
            this.jobId=chunkKey.getJobId();
            this.chunkId=chunkKey.getId();
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
    }
}

