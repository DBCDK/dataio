package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing the entire job life cycle defined by a list of concurrent operational
 * states, where each operational state can at a certain life cycle stage.
 *
 * This class is not thread safe.
 */
public class JobState implements Serializable {
    private static final long serialVersionUID = 2913818143190068326L;

    /** Possible life cycle stages
     */
    // Note that the order of the enum values is important since
    // we are using the ordinal value of an entry to enforce the rule
    // that it is not allowed to go back to an earlier life cycle state.
    public enum LifeCycleState { PENDING, ACTIVE, DONE }

    /** Operational states
     */
    public enum OperationalState { CHUNKIFYING, PROCESSING, DELIVERING }

    private /* final */ Map<OperationalState, LifeCycleState> states;

    /**
     * Default constructor. Initializes all possible operational states to
     * be at the LifeCycleState.PENDING stage of its life cycle
     */
    public JobState() {
        states = new HashMap<OperationalState, LifeCycleState>(OperationalState.values().length);
        for (OperationalState operationalState : OperationalState.values()) {
            states.put(operationalState, LifeCycleState.PENDING);
        }
    }

    /**
     * Sets life cycle state for given operational state
     *
     * @param operationalState operational state
     * @param lifeCycleState life cycle state
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalStateException if trying to set life cycle to a timewise earlier state
     */
    public void setLifeCycleStateFor(OperationalState operationalState, LifeCycleState lifeCycleState)
            throws NullPointerException, IllegalStateException {
        InvariantUtil.checkNotNullOrThrow(operationalState, "operationalState");
        InvariantUtil.checkNotNullOrThrow(lifeCycleState, "lifeCycleState");
        LifeCycleState currentLifeCycleState = getLifeCycleStateFor(operationalState);
        if (currentLifeCycleState.ordinal() > lifeCycleState.ordinal()) {
            throw new IllegalStateException("LifeCycleState regression not allowed " +
                    currentLifeCycleState.name() + " -> " + lifeCycleState.name());
        }
        states.put(operationalState, lifeCycleState);
    }

    /**
     * Gets life cycle state for given operational state
     *
     * @param operationalState operational state
     * @return life cycle state
     * @throws NullPointerException if given null-valued argument
     */
    public LifeCycleState getLifeCycleStateFor(OperationalState operationalState)
            throws NullPointerException {
        return states.get(InvariantUtil.checkNotNullOrThrow(operationalState, "operationalState"));
    }

    public Map<OperationalState, LifeCycleState> getStates() {
        return new HashMap<OperationalState, LifeCycleState>(states);
    }
}
