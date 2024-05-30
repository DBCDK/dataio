package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.io.Serializable;
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
    private Key key = new Key(-1, -1);

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

    // builder api
    public ChunkEntity withJobId(int jobId) {
        this.key.setJobId(jobId);
        return this;
    }

    public ChunkEntity withChunkId(int chunkId) {
        this.key.setId(chunkId);
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
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        @Column(name = "id")
        private int id;

        @Column(name = "jobid")
        private int jobId;

        /* Private constructor in order to keep class static */
        private Key() {
        }

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

        public TrackingKey toTrackingKey() {
            return new TrackingKey(jobId, id);
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
    }
}

