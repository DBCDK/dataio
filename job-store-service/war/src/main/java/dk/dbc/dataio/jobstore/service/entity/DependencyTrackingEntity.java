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

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

/**
 * Class for Tracking Chunk Dependencys.
 *
 *
 */
@Entity
@Table(name = "dependencytracking")
@SqlResultSetMapping(
    name="JobIdChunkIdResult",
    classes={
       @ConstructorResult(
            targetClass=dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key.class,
            columns= {
                    @ColumnResult(name = "jobId"),
                    @ColumnResult(name = "chunkId"),
            }
       )
    }
)
public class DependencyTrackingEntity {
    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    public enum ChunkProcessStatus {
        READY_TO_PROCESS,  // Chunk is Ready for Processing
        QUEUED_TO_PROCESS, // Chunk is Send to JobProcessor JMS queue
        BLOCKED, // Chunk waits for Other Chunk to return from the Sink
        READY_DELEVERING, // Ready for Sending to Sink JMS queue
        QUEUED_TO_DELEVERING // Chunk is send to to the Sink JMS queue
    }



    @EmbeddedId
    private Key key;

    @Column(nullable = false)
    private int sinkid;

    @Column(nullable = false)
    @Convert(converter = ChunkProcessStatusConverter.class)
    private ChunkProcessStatus status = ChunkProcessStatus.READY_TO_PROCESS;

    @Column(columnDefinition = "jsonb" )
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
            return "DependenT..Key{" +
                    "jobId=" + jobId +
                    ", chunkId=" + chunkId +
                    '}';
        }
    }
}

