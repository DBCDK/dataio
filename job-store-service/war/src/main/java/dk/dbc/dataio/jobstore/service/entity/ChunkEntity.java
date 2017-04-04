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

import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "chunk")
public class ChunkEntity {
    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    @EmbeddedId
    private Key key=new Key(-1,-1);

    @Column(nullable = false)
    private String dataFileId;

    @Column(nullable = false)
    private short numberOfItems;

    private Timestamp timeOfCreation;
    private Timestamp timeOfLastModification;
    private Timestamp timeOfCompletion;

    @Column(columnDefinition = "json", nullable = false)
    @Convert(converter = SequenceAnalysisDataConverter.class)
    private SequenceAnalysisData sequenceAnalysisData;

    @Column(columnDefinition = "json", nullable = false)
    @Convert(converter = StateConverter.class)
    private State state;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public String getDataFileId() {
        return dataFileId;
    }

    public void setDataFileId(String dataFileId) {
        this.dataFileId = dataFileId;
    }

    public short getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(short numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Timestamp getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public Timestamp getTimeOfCompletion() {
        return timeOfCompletion;
    }

    public void setTimeOfCompletion(Timestamp timeOfCompletion) {
        this.timeOfCompletion = timeOfCompletion;
    }

    public SequenceAnalysisData getSequenceAnalysisData() {
        return sequenceAnalysisData;
    }

    public void setSequenceAnalysisData(SequenceAnalysisData sequenceAnalysisData) {
        this.sequenceAnalysisData = sequenceAnalysisData;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public CollisionDetectionElement toCollisionDetectionElement() {
        return new CollisionDetectionElement(key.toChunkIdentifier(), sequenceAnalysisData.getData(), numberOfItems);
    }


    // builder api
    public ChunkEntity withJobId(int jobId) {
        this.key.setJobId(jobId );
        return this;
    }

    public ChunkEntity withChunkId(int chunkId) {
        this.key.setId( chunkId );
        return this;
    }

    public ChunkEntity withSequenceAnalysisData(SequenceAnalysisData sequenceAnalysisData) {
        this.sequenceAnalysisData = sequenceAnalysisData;
        return this;
    }

    public ChunkEntity withState(State state) {
        this.state = state;
        return this;
    }

    public ChunkEntity withNumberOfItems(short numberOfItems) {
        this.numberOfItems = numberOfItems;
        return this;
    }

    @PrePersist
    void onCreate() {
        final Timestamp ts = new Timestamp(new Date().getTime());
        this.timeOfCreation = ts;
        this.timeOfLastModification = ts;
    }

    @PreUpdate
    void onUpdate() {
        this.timeOfLastModification = new Timestamp(new Date().getTime());
    }

    @Embeddable
    public static class Key {
        @Column(name = "id")
        private int id;

        @Column(name = "jobid")
        private int jobId;

        /* Private constructor in order to keep class static */
        private Key(){}

        public Key(int id, int jobId) {
            this.id = id;
            this.jobId = jobId;
        }

        public int getId() {
            return id;
        }

        public int getJobId() {
            return jobId;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setJobId(int jobId) {
            this.jobId = jobId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key key = (Key) o;

            if (id != key.id) return false;
            if (jobId != key.jobId) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + jobId;
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "chunkId=" + id +
                    ", jobId=" + jobId +
                    '}';
        }

        public ChunkIdentifier toChunkIdentifier() {
            return new ChunkIdentifier(jobId, id);
        }
    }
}

