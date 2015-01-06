package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing the current state of a job.
 */
public class State {

    @JsonProperty
    private final Map<Phase, StateElement> states;

    public enum Phase { PARTITIONING, PROCESSING, DELIVERING }

    public State() {
        states = new HashMap<>(Phase.values().length);
        for (Phase phase : Phase.values()) {
            states.put(phase, new StateElement());
        }
    }

    public State(State state) {
        states = new HashMap<>(state.states);
        for (Map.Entry<Phase, StateElement> entry : state.states.entrySet()) {
            states.put(entry.getKey(), new StateElement(entry.getValue()));
        }
    }

    /**
     * @param phase (partitioning, processing, delivering)
     * @return the state element for the specified phase
     */
    public StateElement getPhase(Phase phase) {
        return states.get(phase);
    }

    /**
     * Method used to update the state object through the state elements: partitioning, processing, delivering
     * @param stateChange holding the values used for update
     */
    public void updateState(StateChange stateChange) {
        if(stateChange == null) {
            throw new NullPointerException("State Change input cannot be null");
        }
        else if(stateChange.getPhase() == null) {
            throw new IllegalStateException("Phase: (Partitioning, Processing, Delivering) must be provided as input");
        }
        else {
            updateStateElement(states.get(stateChange.getPhase()), stateChange);
        }
    }

    /*
     * Private methods
     */

    /**
     * Method updating used to update a state element
     *
     * @param stateElement to update
     * @param stateChange holding the values used for update
     */
    private void updateStateElement(StateElement stateElement, StateChange stateChange) {
        if(stateElement.getEndDate() == null) {
            setBeginDate(stateElement, stateChange);
            updateStateElementStatusCounters(stateElement, stateChange);
            updateStateElementLifeCycleCounters(stateElement, stateChange);
            setEndDate(stateElement, stateChange);
        }
    }

    /**
     * Method setting the begin date.
     * The begin date from state element is used if set. If not set the current time is used as beginDate
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
     * Method updates the status counter for the the state element (succeeded, failed, ignored).
     * The incrementation number, provided through the state change object, CANNOT be a negative number.
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
     * Method updating the lifecycle counters for the state element (pending, active, done).
     * The incrementation number, provided through the state change object, CAN be a negative number
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
     * Method used to determine if an end date should be set on the state element object
     * partitioning must be complete before either processing or delivering can complete.
     *
     * Note: regarding asynchronous returns:
     * We are assuming its possible for delivering to be marked as "finished" before processing
     * is marked as "finished" due to asynchronous returns.
     * -> Our current requirements might need polishing later on.
     *
     * @param stateElement partitioning, processing or delivering
     * @param stateChange containing the desired changes
     *
     * @throws IllegalStateException if attempting to set an end date on either processing or delivering before
     *         partitioning is done
     */
    private void setEndDate(StateElement stateElement, StateChange stateChange) throws IllegalStateException {
        if (stateChange.getPhase() == Phase.PARTITIONING) {
                stateElement.setEndDate(stateChange.getEndDate());
        } else {
            StateElement partitioning = getPhase(Phase.PARTITIONING);
            if (partitioning.getEndDate() != null && stateChange.getEndDate() != null) {
                stateElement.setEndDate(stateChange.getEndDate());
            } else if (stateChange.getEndDate() == null && stateElement.getDone() == partitioning.getDone()) {
                stateElement.setEndDate(getDateWithCurrentTime());
            } else if (partitioning.getEndDate() == null && stateChange.getEndDate() != null) {
                throw new IllegalStateException("Partitioning must be completed before "
                        + stateChange.getPhase().toString()
                        + " can complete.");
            }
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
