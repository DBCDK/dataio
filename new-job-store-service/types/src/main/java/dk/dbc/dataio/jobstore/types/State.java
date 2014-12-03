package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.JobState;

import java.util.Date;

public class State {
    private final StateElement partitioning;
    private final StateElement processing;
    private final StateElement delivering;

    public State() {

        this.partitioning = new StateElement();
        this.processing = new StateElement();
        this.delivering = new StateElement();
    }

    /**
     * @return state element partitioning
     */
    public StateElement getPartitioning() {
        return partitioning;
    }

    /**
     * @return state element processing
     */
    public StateElement getProcessing() {
        return processing;
    }

    /**
     * @return state element delivering
     */
    public StateElement getDelivering() {
        return delivering;
    }

    /**
     * Method used to update the state object through the state elements: partitioning, processing, delivering
     * @param stateChange holding the values used for update
     */
    public void updateState(StateChange stateChange) {
        if(stateChange == null) {
            throw new NullPointerException("State Change input cannot be null");
        }
        if(stateChange.getOperationalState() == null) {
            throw new IllegalStateException("Operational State (Partitioning, Processing, Delivering) must be provided as input");
        }

        switch(stateChange.getOperationalState()) {
            case CHUNKIFYING :
                updateStateElement(partitioning, stateChange);
                break;
            case PROCESSING:
                updateStateElement(processing, stateChange);
                break;
            case DELIVERING:
                updateStateElement(delivering, stateChange);
                break;
        }
    }

    /*
     * Private methods
     */

    /**
     * Method updating all counters for chunk and lifecycle as well as the 2 timestamps (begin and end)
     *
     * @param stateElement to update
     * @param stateChange holding the values used for update
     */
    private void updateStateElement(StateElement stateElement, StateChange stateChange) {
        setBeginDate(stateElement, stateChange);
        updateStateElementStatusCounters(stateElement, stateChange);
        updateStateElementLifeCycleCounters(stateElement, stateChange);
        setEndDate(stateElement, stateChange);
    }

    /**
     * Method setting the begin date.
     * The beginDate from state element is used if set. If not set the current time is used as beginDate
     *
     * @param stateElement to update
     * @param stateChange holding the values used for update
     */
    private void setBeginDate(StateElement stateElement, StateChange stateChange) {
        if (stateElement.getBeginDate() == null && stateChange.getBeginDate() != null) {
            stateElement.setBeginDate(stateChange.getBeginDate());
        } else if (stateElement.getBeginDate() == null) {
            stateElement.setBeginDate(getDateWithCurrentTime());
        }
    }

    /**
     * Method updates the chunk counter for the the state element.
     * The incrementation provided through the state change object cannot be a negative number.
     *
     * @param stateElement to update
     * @param stateChange holding the values used for update
     */
    private void updateStateElementStatusCounters(StateElement stateElement, StateChange stateChange) throws IllegalArgumentException {
        stateElement.setSucceeded(stateElement.getSucceeded() + stateChange.getSucceeded());
        stateElement.setFailed(stateElement.getFailed() + stateChange.getFailed());
        stateElement.setIgnored(stateElement.getIgnored() + stateChange.getIgnored());
    }

    /**
     * Method updating the lifecycle counters for the state element.
     * The incrementation provided through the state change object can be a negative number
     *
     * @param stateElement to update
     * @param stateChange holding the values used for update
     */
    private void updateStateElementLifeCycleCounters(StateElement stateElement, StateChange stateChange) {
        stateElement.setPending(stateElement.getPending() + stateChange.getPending());
        stateElement.setActive(stateElement.getActive() + stateChange.getActive());
        stateElement.setDone(stateElement.getFailed() + stateElement.getSucceeded() + stateElement.getIgnored());
    }

    /**
     * Method used to determine if an end date should be set on the stateElement object
     * partitioning must be complete before either processing or delivering can complete.
     *
     * Note: regarding asynchronous returns:
     * We are assuming its possible for delivering to "finish" before processing
     * due to asynchronous returns.
     * -> Our current requirements might need polishing later on.
     *
     * @param stateElement partitioning, processing or delivering
     * @param stateChange containing the desired changes
     *
     * @throws IllegalStateException if attempting to set an end date on either processing or delivering before
     *         partitioning is done
     */
    private void setEndDate(StateElement stateElement, StateChange stateChange) throws IllegalStateException{
        if(stateChange.getOperationalState() == JobState.OperationalState.PROCESSING
                || stateChange.getOperationalState() == JobState.OperationalState.DELIVERING) {

            if (partitioning.getEndDate() == null && stateChange.getEndDate() != null) {
                throw new IllegalStateException("Partitioning must be completed before "
                                + stateChange.getOperationalState().toString()
                                + " can complete.");
            }
            else if (stateElement.getEndDate() == null && stateChange.getEndDate() != null) {
                stateElement.setEndDate(stateChange.getEndDate());
            }
            else if (stateElement.getDone() == partitioning.getDone() && stateChange.getEndDate() == null) {
                 stateElement.setEndDate(getDateWithCurrentTime());
            }
        } else if(stateElement.getEndDate() == null && stateChange.getEndDate() != null) {
            stateElement.setEndDate(stateChange.getEndDate());
        }
    }

    /**
     * Retrieves the current time in milliseconds
     * @return new Date
     */
    private Date getDateWithCurrentTime() {
        return new Date(System.currentTimeMillis());
    }
}
