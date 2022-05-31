package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateElement;
import dk.dbc.dataio.jobstore.types.WorkflowNote;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

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

    // TODO: 4/4/17 Drop timeOfLastModification db trigger and use @PrePersist and @PreUpdate callbacks instead (to avoid unnecessary flush() and refresh() calls)

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;

    private Timestamp timeOfCompletion;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfLastModification;

    @Convert(converter = StateConverter.class)
    private State state;

    @Convert(converter = ChunkItemConverter.class)
    private ChunkItem partitioningOutcome;

    @Convert(converter = ChunkItemConverter.class)
    private ChunkItem processingOutcome;

    @Convert(converter = ChunkItemConverter.class)
    private ChunkItem nextProcessingOutcome;

    @Convert(converter = ChunkItemConverter.class)
    private ChunkItem deliveringOutcome;

    @Convert(converter = WorkflowNoteConverter.class)
    private WorkflowNote workflowNote;

    @Convert(converter = RecordInfoConverter.class)
    private RecordInfo recordInfo;

    private Integer positionInDatafile;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public ItemEntity withKey(Key key) {
        this.key = key;
        return this;
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

    public ItemEntity withTimeOfCompletion(Timestamp timeOfCompletion) {
        this.timeOfCompletion = timeOfCompletion;
        return this;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ItemEntity withState(State state) {
        this.state = state;
        return this;
    }

    public ChunkItem getPartitioningOutcome() {
        return partitioningOutcome;
    }

    public void setPartitioningOutcome(ChunkItem partitioningOutcome) {
        this.partitioningOutcome = partitioningOutcome;
    }

    public ItemEntity withPartitioningOutcome(ChunkItem partitioningOutcome) {
        this.partitioningOutcome = partitioningOutcome;
        return this;
    }

    public ChunkItem getProcessingOutcome() {
        return processingOutcome;
    }

    public void setProcessingOutcome(ChunkItem processingOutcome) {
        this.processingOutcome = processingOutcome;
    }

    public ItemEntity withProcessingOutcome(ChunkItem processingOutcome) {
        this.processingOutcome = processingOutcome;
        return this;
    }

    public ChunkItem getNextProcessingOutcome() {
        return nextProcessingOutcome;
    }

    public void setNextProcessingOutcome(ChunkItem nextProcessingOutcome) {
        this.nextProcessingOutcome = nextProcessingOutcome;
    }

    public ItemEntity withNextProcessingOutcome(ChunkItem nextProcessingOutcome) {
        this.nextProcessingOutcome = nextProcessingOutcome;
        return this;
    }

    public ChunkItem getDeliveringOutcome() {
        return deliveringOutcome;
    }

    public void setDeliveringOutcome(ChunkItem deliveringOutcome) {
        this.deliveringOutcome = deliveringOutcome;
    }

    public ItemEntity withDeliveringOutcome(ChunkItem deliveringOutcome) {
        this.deliveringOutcome = deliveringOutcome;
        return this;
    }

    public WorkflowNote getWorkflowNote() {
        return workflowNote;
    }

    public void setWorkflowNote(WorkflowNote workflowNote) {
        this.workflowNote = workflowNote;
    }

    public ItemEntity withWorkflowNote(WorkflowNote workflowNote) {
        this.workflowNote = workflowNote;
        return this;
    }

    public RecordInfo getRecordInfo() {
        return recordInfo;
    }

    public void setRecordInfo(RecordInfo recordInfo) {
        this.recordInfo = recordInfo;
    }

    public ItemEntity withRecordInfo(RecordInfo recordInfo) {
        this.recordInfo = recordInfo;
        return this;
    }

    public Integer getPositionInDatafile() {
        return positionInDatafile;
    }

    public ItemEntity withPositionInDatafile(Integer positionInDatafile) {
        this.positionInDatafile = positionInDatafile;
        return this;
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
        private Key() {
        }

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

        public int getZeroBasedIndex() {
            return chunkId * Constants.CHUNK_MAX_SIZE + id;
        }

        public int getOneBasedIndex() {
            return chunkId * Constants.CHUNK_MAX_SIZE + id + 1;
        }
    }

    public Optional<State.Phase> getFailedPhase() {
        for (State.Phase phase : State.Phase.values()) {
            if (getChunkItemStatusForPhase(phase) == ChunkItem.Status.FAILURE) {
                return Optional.of(phase);
            }
        }
        return Optional.empty();
    }

    public ChunkItem getChunkItemForPhase(State.Phase phase) {
        switch (phase) {
            case PARTITIONING:
                return getPartitioningOutcome();
            case PROCESSING:
                return getProcessingOutcome();
            case DELIVERING:
                return getDeliveringOutcome();
            default:
                throw new IllegalStateException(String.format("Unknown phase: '%s'", phase));
        }
    }

    public ItemInfoSnapshot toItemInfoSnapshot() {
        return new ItemInfoSnapshot(
                key.getOneBasedIndex(),
                key.getId(),
                key.getChunkId(),
                key.getJobId(),
                toDate(timeOfCreation),
                toDate(timeOfLastModification),
                toDate(timeOfCompletion),
                state,
                workflowNote,
                recordInfo,
                partitioningOutcome == null ? null : partitioningOutcome.getTrackingId());
    }

    private ChunkItem.Status getChunkItemStatusForPhase(State.Phase phase) {
        final StateElement stateElement = state.getPhase(phase);
        if (stateElement.getSucceeded() == 1) {
            return ChunkItem.Status.SUCCESS;
        } else if (stateElement.getFailed() == 1) {
            return ChunkItem.Status.FAILURE;
        } else {
            return ChunkItem.Status.IGNORE;
        }
    }

    /**
     * Converts a java.sql.Timestamp to a java.util.Date
     *
     * @param timestamp to convert
     * @return new Date representation of the timestamp, null if the timestamp is null
     */
    private static Date toDate(Timestamp timestamp) {
        if (timestamp != null) {
            return new Date(timestamp.getTime());
        }
        return null;
    }
}
