package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateElement;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.nio.charset.Charset;
import java.sql.Timestamp;

@Entity
@Table(name = "item")
public class ItemEntity {
    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

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
    @Convert(converter = ChunkItemConverter.class)
    private ChunkItem nextProcessingOutcome;

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

    public ChunkItem getNextProcessingOutcome() {
        return nextProcessingOutcome;
    }

    public void setNextProcessingOutcome(ChunkItem nextProcessingOutcome) {
        this.nextProcessingOutcome = nextProcessingOutcome;
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

        /* Private constructor in order to keep class static */
        private Key(){}

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

        @Override
        public String toString() {
            return "Key{" +
                    "jobId=" + jobId +
                    ", chunkId=" + chunkId +
                    ", itemId=" + id +
                    '}';
        }
    }

    /**
     * @param phase phase
     * @return ChunkItem representation of item data for specified phase
     * @throws NullPointerException if called with null-valued phase, if
     * item contains no data for phase or if item contains no state info
     * for phase.
     */
    public ChunkItem toChunkItem(State.Phase phase) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(phase, "phase");
        final ItemData itemData = getItemDataForPhase(phase);
        return new ChunkItem(key.getId(), StringUtil.asBytes(
                StringUtil.base64decode(itemData.getData(), itemData.getEncoding()), itemData.getEncoding()),
                getChunkItemStatusForPhase(phase));
    }

    /**
     * @param phase phase
     * @return encoding of item data for specified phase or null
     * if no item data is set
     */
    public Charset getEncodingForPhase(State.Phase phase) {
        final ItemData itemData = getItemDataForPhase(phase);
        if (itemData != null) {
            return itemData.getEncoding();
        }
        return null;
    }

    private ItemData getItemDataForPhase(State.Phase phase) {
        switch (phase) {
            case PARTITIONING: return getPartitioningOutcome();
            case PROCESSING: return getProcessingOutcome();
            case DELIVERING: return getDeliveringOutcome();
            default: throw new IllegalStateException(String.format("Unknown phase: '%s'", phase));
        }
    }

    private ChunkItem.Status getChunkItemStatusForPhase(State.Phase phase) {
        final StateElement stateElement = state.getPhase(phase);
        if (stateElement.getSucceeded() == 1) {
            return ChunkItem.Status.SUCCESS;
        } else if(stateElement.getFailed() == 1) {
            return ChunkItem.Status.FAILURE;
        } else {
            return ChunkItem.Status.IGNORE;
        }
    }
}
