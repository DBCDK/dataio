package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.jobstore.types.StateElement;

import java.io.Serializable;

/**
 * Created by sma on 26/06/2017.
 * Class used by the front end to gain access to the various counters stored within the StateElements
 */
public class StateModel implements Serializable {

    private StateElement partitioning = new StateElement();
    private StateElement processing = new StateElement();
    private StateElement delivering = new StateElement();
    private boolean aborted = false;

    public boolean isAborted() {
        return aborted;
    }

    public StateModel withAborted(boolean aborted) {
        this.aborted = aborted;
        return this;
    }

    public StateElement getPartitioning() {
        return partitioning;
    }

    public StateModel withPartitioning(StateElement partitioning) {
        this.partitioning = partitioning;
        return this;
    }

    public StateElement getProcessing() {
        return processing;
    }

    public StateModel withProcessing(StateElement processing) {
        this.processing = processing;
        return this;
    }

    public StateElement getDelivering() {
        return delivering;
    }

    public StateModel withDelivering(StateElement delivering) {
        this.delivering = delivering;
        return this;
    }

    public int getPartitionedCounter() {
        return getStateCount(partitioning);
    }

    public int getProcessedCounter() {
        return getStateCount(processing);
    }

    public int getDeliveredCounter() {
        return getStateCount(delivering);
    }

    public int getFailedCounter() {
        return partitioning.getFailed() + processing.getFailed() + delivering.getFailed();
    }

    public int getIgnoredCounter() {
        if (delivering.getIgnored() != 0) {
            return delivering.getIgnored();
        } else if (processing.getIgnored() != 0) {
            return processing.getIgnored();
        } else {
            return partitioning.getIgnored();
        }
    }

    /**
     * This method calculates the total number of items in the given state element
     *
     * @param element The state element for the state in question
     * @return The total number of items in the give state
     */
    private static int getStateCount(StateElement element) {
        return element.getSucceeded() + element.getFailed() + element.getIgnored();
    }
}
