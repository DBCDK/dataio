package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.Diagnostic;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class representing the current state of a job.
 */
public class State {

    @JsonProperty
    private final Map<Phase, StateElement> states;

    @JsonProperty
    private final List<Diagnostic> diagnostics;

    public enum Phase {PARTITIONING, PROCESSING, DELIVERING}

    public State() {
        diagnostics = new ArrayList<>();
        states = new EnumMap<>(Phase.class);
        for (Phase phase : Phase.values()) {
            states.put(phase, new StateElement());
        }
    }

    public State(State state) {
        states = new HashMap<>(state.states);
        for (Map.Entry<Phase, StateElement> entry : state.states.entrySet()) {
            states.put(entry.getKey(), new StateElement(entry.getValue()));
        }
        this.diagnostics = new ArrayList<>(state.getDiagnostics().size());
        this.diagnostics.addAll(state.getDiagnostics());
    }

    /**
     * Retrieves the specified stateElement from states
     *
     * @param phase (partitioning, processing, delivering)
     * @return the state element for the specified phase
     */
    public StateElement getPhase(Phase phase) {
        return states.get(phase);
    }


    /**
     * Method used to update the state object through the state elements: partitioning, processing, delivering
     *
     * @param stateChange holding the values used for update
     */
    public void updateState(StateChange stateChange) {
        if (stateChange == null) {
            throw new NullPointerException("State Change input cannot be null");
        } else if (stateChange.getPhase() == null) {
            throw new IllegalStateException("Phase: (Partitioning, Processing, Delivering) must be provided as input");
        } else {
            updateStateElement(states.get(stateChange.getPhase()), stateChange);
        }
    }

    /**
     * Checks if a given phase is done (end date is set)
     *
     * @param phase, the phase to check
     * @return true if the given phase is done, otherwise false
     */
    public boolean phaseIsDone(Phase phase) {
        return getPhase(phase).getEndDate() != null;
    }

    /**
     * Checks if all phases are done (end dates are set on all phases)
     *
     * @return true if all phases have completed, otherwise false
     */
    public boolean allPhasesAreDone() {
        return states.values().stream()
                .map(StateElement::getEndDate)
                .noneMatch(Objects::isNull);
    }

    /**
     * Retrieves the list of diagnostics
     *
     * @return list of diagnostics, empty list if no diagnostic has been added.
     */
    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    /**
     * Checks if a diagnostic with level: FATAL has been set on state
     *
     * @return true if the list of diagnostics contains any diagnostic with level: FATAL,
     * otherwise false.
     */
    public boolean fatalDiagnosticExists() {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getLevel() == Diagnostic.Level.FATAL) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean isAborted() {
        return diagnostics.stream().map(Diagnostic::getLevel).anyMatch(Diagnostic.Level.ABORTED::equals);
    }

    /*
     * Private methods
     */

    /**
     * Method updating used to update a state element
     *
     * @param stateElement to update
     * @param stateChange  holding the values used for update
     */
    private void updateStateElement(StateElement stateElement, StateChange stateChange) {
        if (stateElement.getEndDate() == null) {
            setBeginDate(stateElement, stateChange);
            updateStateElementStatusCounters(stateElement, stateChange);
            setEndDate(stateElement, stateChange);
        }
    }

    /**
     * Method setting the begin date.
     * The begin date from state element is used if set. If not set the current time is used as beginDate
     *
     * @param stateElement to update
     * @param stateChange  holding the values used for update
     */
    private void setBeginDate(StateElement stateElement, StateChange stateChange) {
        if (stateElement.getBeginDate() == null && stateChange.getBeginDate() != null) {
            stateElement.withBeginDate(stateChange.getBeginDate());
        } else if (stateElement.getBeginDate() == null) {
            stateElement.withBeginDate(getDateWithCurrentTime());
        }
    }

    /**
     * Method updates the status counter for the the state element (succeeded, failed, ignored).
     * The incrementation number, provided through the state change object, CANNOT be a negative number.
     *
     * @param stateElement to update
     * @param stateChange  holding the values used for update
     */
    private void updateStateElementStatusCounters(StateElement stateElement, StateChange stateChange) throws IllegalArgumentException {
        stateElement.withSucceeded(stateElement.getSucceeded() + stateChange.getSucceeded());
        stateElement.withFailed(stateElement.getFailed() + stateChange.getFailed());
        stateElement.withIgnored(stateElement.getIgnored() + stateChange.getIgnored());
    }

    /**
     * Method used to determine if an end date should be set on the state element object
     * partitioning must be complete before either processing or delivering can complete.
     * <p>
     * Note: regarding asynchronous returns:
     * We are assuming its possible for delivering to be marked as "finished" before processing
     * is marked as "finished" due to asynchronous returns.
     * -> Our current requirements might need polishing later on.
     *
     * @param stateElement partitioning, processing or delivering
     * @param stateChange  containing the desired changes
     * @throws IllegalStateException if attempting to set an end date on either processing or delivering before
     *                               partitioning is done
     */
    private void setEndDate(StateElement stateElement, StateChange stateChange) throws IllegalStateException {
        if (stateChange.getPhase() == Phase.PARTITIONING) {
            stateElement.withEndDate(stateChange.getEndDate());
        } else {
            StateElement partitioning = getPhase(Phase.PARTITIONING);
            if (partitioning.getEndDate() != null && stateChange.getEndDate() != null) {
                stateElement.withEndDate(stateChange.getEndDate());
            } else if (stateChange.getEndDate() == null && phaseDone(partitioning, stateElement)) {
                stateElement.withEndDate(getDateWithCurrentTime());
            } else if (partitioning.getEndDate() == null && stateChange.getEndDate() != null) {
                throw new IllegalStateException("Partitioning must be completed before "
                        + stateChange.getPhase().toString()
                        + " can complete.");
            }
        }
    }

    /**
     * Method used to determine if an end date should be added to the stateElement.
     * <p>
     * Returns true if:
     * The end date is set on partitioning and:
     * The sum of (succeeded, failed, ignored) on partitioning equals the sum of
     * (succeeded, failed, ignored) on the state element.
     *
     * @param partitioning stateElement
     * @param stateElement on which to determined if end date should be set.
     * @return true if end date is set on partitioning and if the sum of partitioning
     * counters equals the sum of all stateElement counters, otherwise false
     */
    private boolean phaseDone(StateElement partitioning, StateElement stateElement) {
        return stateElement.getSucceeded() + stateElement.getIgnored() + stateElement.getFailed()
                == partitioning.getSucceeded() + partitioning.getIgnored() + partitioning.getFailed()
                && partitioning.getEndDate() != null;
    }

    /**
     * Retrieves the current time in milliseconds
     *
     * @return new Date
     */
    private Date getDateWithCurrentTime() {
        return new Date(System.currentTimeMillis());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;

        State state = (State) o;

        return states.equals(state.states) && diagnostics.equals(state.diagnostics);

    }

    @Override
    public int hashCode() {
        int result = states.hashCode();
        result = 31 * result + diagnostics.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "State{" +
                "states=" + states +
                ", diagnostics=" + diagnostics +
                '}';
    }
}
