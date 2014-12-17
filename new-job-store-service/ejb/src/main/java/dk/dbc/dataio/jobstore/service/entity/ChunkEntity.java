package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.State;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "chunk")
public class ChunkEntity {

    @EmbeddedId
    private Key key;

    @Column(nullable = false)
    private String dataFileId;

    @Column(nullable = false)
    private int numberOfItems;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfLastModification;

    private Timestamp timeOfCompletion;

    @Column(columnDefinition = "json")
    @Convert(converter = SequenceAnalysisDataConverter.class)
    private List<String> sequenceAnalysisData;

    @Column(columnDefinition = "json")
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

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(int numberOfItems) {
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

    public List<String> getSequenceAnalysisData() {
        return sequenceAnalysisData;
    }

    public void setSequenceAnalysisData(List<String> sequenceAnalysisData) {
        this.sequenceAnalysisData = sequenceAnalysisData;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Embeddable
    public static class Key {
        @Column(name = "id")
        private int id;

        @Column(name = "jobid")
        private int jobId;

        public Key(){}

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

    }
}

