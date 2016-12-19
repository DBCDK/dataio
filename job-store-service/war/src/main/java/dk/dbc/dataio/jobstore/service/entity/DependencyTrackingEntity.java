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
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class for Tracking Chunk Dependencys.
 *
 *
 */
@Entity
@Table(name = "dependencytracking")
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "JobIdChunkIdResult",
                classes = {
                        @ConstructorResult(
                                targetClass = dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key.class,
                                columns = {
                                        @ColumnResult(name = "jobId"),
                                        @ColumnResult(name = "chunkId"),
                                }
                        )
                }
        ),
        @SqlResultSetMapping(
                name = "SinkIdStatusCountResult",
                classes = {
                        @ConstructorResult(
                                targetClass = dk.dbc.dataio.jobstore.service.entity.SinkIdStatusCountResult.class,
                                columns = {
                                        @ColumnResult(name = "sinkId"),
                                        @ColumnResult(name = "Status"),
                                        @ColumnResult(name = "count"),
                                }
                        )
                }
        )
})
@NamedNativeQueries({
        @NamedNativeQuery( name= "SinkIdStatusCount",
                query = "select sinkid, status, count(*) from dependencytracking group by sinkid, status order by sinkid, status",
                resultSetMapping = "SinkIdStatusCountResult"
        ),
        @NamedNativeQuery(name= DependencyTrackingEntity.QUERY_JOB_COUNT_CHUNK_COUNT,
                query = "select count (distinct jobid) as numberOfJobs, count(jobid) as NumberOfChunks from dependencytracking where sinkid = ?"
        ),
        @NamedNativeQuery( name= DependencyTrackingEntity.CHUNKS_PR_SINK_JOBID,
                query = "select jobId, chunkId from dependencyTracking where sinkId=? and (jobId=? or matchKeys @> '[\"?\"]' ) ORDER BY jobId, chunkId for update",
                resultSetMapping = "JobIdChunkIdResult"
        ),

})
public class DependencyTrackingEntity {
    public static final String QUERY_JOB_COUNT_CHUNK_COUNT = "DependencyTracking.jobCountChunkCountResult";
    public static final String CHUNKS_PR_SINK_JOBID = "DependencyTracking.jobIdSinkIdChunks";
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

    public DependencyTrackingEntity() {
    }

    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    public enum ChunkProcessStatus {
        READY_TO_PROCESS,  // Chunk is Ready for Processing
        QUEUED_TO_PROCESS, // Chunk is Send to JobProcessor JMS queue
        BLOCKED, // Chunk waits for Other Chunk to return from the Sink
        READY_TO_DELIVER, // Ready for Sending to Sink JMS queue
        QUEUED_TO_DELIVERY // Chunk is send to to the Sink JMS queue
    }



    @EmbeddedId
    private Key key;

    @Column(nullable = false)
    private int sinkid;

    @Column(nullable = false)
    @Convert(converter = ChunkProcessStatusConverter.class)
    private ChunkProcessStatus status = ChunkProcessStatus.READY_TO_PROCESS;

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

    public ChunkProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ChunkProcessStatus status) {
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyTrackingEntity that = (DependencyTrackingEntity) o;

        if (getSinkid() != that.getSinkid()) return false;
        if (!getKey().equals(that.getKey())) return false;
        if (getStatus() != that.getStatus()) return false;
        if (getWaitingOn() != null ? !getWaitingOn().equals(that.getWaitingOn()) : that.getWaitingOn() != null)
            return false;
        if (getBlocking() != null ? !getBlocking().equals(that.getBlocking()) : that.getBlocking() != null)
            return false;
        return getMatchKeys() != null ? getMatchKeys().equals(that.getMatchKeys()) : that.getMatchKeys() == null;

    }

    @Override
    public int hashCode() {
        int result = getKey().hashCode();
        result = 31 * result + getSinkid();
        result = 31 * result + getStatus().hashCode();
        result = 31 * result + (getWaitingOn() != null ? getWaitingOn().hashCode() : 0);
        result = 31 * result + (getBlocking() != null ? getBlocking().hashCode() : 0);
        result = 31 * result + (getMatchKeys() != null ? getMatchKeys().hashCode() : 0);
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

