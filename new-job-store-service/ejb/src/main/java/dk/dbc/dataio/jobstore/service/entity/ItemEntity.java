package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.State;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "item")
public class ItemEntity {

    @EmbeddedId
    private Key key;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;

    private Timestamp timeOfCompletion;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfLastModification;

    @Column(columnDefinition = "json", nullable = false)
    @Convert(converter = StateConverter.class)
    private State state;

    @Column(columnDefinition = "json")
    @Convert(converter = ItemDataConverter.class)
    private ItemData partitioningOutcome;

    @Column(columnDefinition = "json")
    @Convert(converter = ItemDataConverter.class)
    private ItemData processingOutcome;

    @Column(columnDefinition = "json")
    @Convert(converter = ItemDataConverter.class)
    private ItemData deliveringOutcome;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ItemData getPartitioningOutcome() {
        return partitioningOutcome;
    }

    public void setPartitioningOutcome(ItemData partitioningOutcome) {
        this.partitioningOutcome = partitioningOutcome;
    }

    public ItemData getProcessingOutcome() {
        return processingOutcome;
    }

    public void setProcessingOutcome(ItemData processingOutcome) {
        this.processingOutcome = processingOutcome;
    }

    public ItemData getDeliveringOutcome() {
        return deliveringOutcome;
    }

    public void setDeliveringOutcome(ItemData deliveringOutcome) {
        this.deliveringOutcome = deliveringOutcome;
    }

    @Embeddable
    public static class Key {
        @Column(name = "id")
        private short id;

        @Column(name = "chunkid")
        private int chunkId;

        @Column(name = "jobid")
        private int jobId;



        public Key(){}

        public Key(int jobId, int chunkId, short id) {
            this.id = id;
            this.jobId = jobId;
            this.chunkId = chunkId;
        }

        public short getId() {
            return id;
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
            if (!(o instanceof Key)) return false;

            Key key = (Key) o;

            if (chunkId != key.chunkId) return false;
            if (id != key.id) return false;
            if (jobId != key.jobId) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) id;
            result = 31 * result + chunkId;
            result = 31 * result + jobId;
            return result;
        }
    }
}
