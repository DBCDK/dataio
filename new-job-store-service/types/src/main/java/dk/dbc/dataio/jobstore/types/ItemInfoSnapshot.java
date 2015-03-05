package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;


public class ItemInfoSnapshot {

    private int itemNumber;
    private short itemId;
    private int chunkId;
    private int jobId;
    private Date timeOfCreation;
    private Date timeOfLastModification ;
    private Date timeOfCompletion;
    private State state;

    @JsonCreator
    public ItemInfoSnapshot(@JsonProperty("itemNumber") int itemNumber,
                            @JsonProperty("itemId") short itemId,
                            @JsonProperty("chunkId") int chunkId,
                            @JsonProperty("jobId") int jobId,
                            @JsonProperty("timeOfCreation") Date timeOfCreation,
                            @JsonProperty("timeOfLastModification") Date timeOfLastModification,
                            @JsonProperty("timeOfCompletion") Date timeOfCompletion,
                            @JsonProperty("state") State state) {

        this.itemNumber = itemNumber;
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.timeOfCreation = timeOfCreation;
        this.timeOfLastModification = timeOfLastModification;
        this.timeOfCompletion = timeOfCompletion;
        this.state = state;
    }

    public int getItemNumber() {
        return itemNumber;
    }

    public short getItemId() {
        return itemId;
    }

    public int getChunkId() {
        return chunkId;
    }

    public int getJobId() {
        return jobId;
    }

    public Date getTimeOfCreation() {
        return timeOfCreation;
    }

    public Date getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public Date getTimeOfCompletion() {
        return timeOfCompletion;
    }

    public State getState() {
        return state;
    }
}
