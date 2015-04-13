package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;

import java.util.Date;

public class ItemInfoSnapshotBuilder {

    private int itemNumber = 23;
    private short itemId = 3;
    private int chunkId = 2;
    private int jobId = 1;
    private Date timeOfCreation = new Date();
    private Date timeOfLastModification = new Date() ;
    private Date timeOfCompletion = new Date();
    private State state = new State();

    public ItemInfoSnapshotBuilder setItemId(short itemId) {
        this.itemId = itemId;
        this.itemNumber = calculateItemNumber();
        return this;
    }

    public ItemInfoSnapshotBuilder setChunkId(int chunkId) {
        this.chunkId = chunkId;
        this.itemNumber = calculateItemNumber();
        return this;
    }

    public ItemInfoSnapshotBuilder setJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public ItemInfoSnapshotBuilder setIimeOfCreation(Date timeOfCreation) {
        this.timeOfCreation = timeOfCreation;
        return this;
    }

    public ItemInfoSnapshotBuilder setTimeOfLastModification(Date timeOfLastModification) {
        this.timeOfLastModification = timeOfLastModification;
        return this;
    }

    public ItemInfoSnapshotBuilder setTimeOfCompletion(Date timeOfCompletion) {
        this.timeOfCompletion = timeOfCompletion;
        return this;
    }

    public ItemInfoSnapshotBuilder setState(State state) {
        this.state = state;
        return this;
    }

    public ItemInfoSnapshot build() {
        return new ItemInfoSnapshot(itemNumber, itemId, chunkId, jobId, timeOfCreation, timeOfLastModification, timeOfCompletion, state);
    }

    private int calculateItemNumber() {
        return chunkId * 10 + itemId + 1;
    }

}
