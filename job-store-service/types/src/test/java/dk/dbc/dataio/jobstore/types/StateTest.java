package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StateTest {

    private static final Date BEGIN_DATE = new Date(System.currentTimeMillis());
    private static final Date END_DATE = new Date(System.currentTimeMillis() + 1000000);
    private static final State.Phase PARTITIONING = State.Phase.PARTITIONING;
    private static final State.Phase PROCESSING = State.Phase.PROCESSING;
    private static final State.Phase DELIVERING = State.Phase.DELIVERING;
    private static final Random random = new Random();

    @Test
    public void constructor_noArgs_returnsNewInstanceWithInitializedElements() {
        State state = new State();
        assertState(state);
        assertNewStateElement(state.getPhase(State.Phase.PARTITIONING));
        assertNewStateElement(state.getPhase(State.Phase.PROCESSING));
        assertNewStateElement(state.getPhase(State.Phase.DELIVERING));
        assertThat(state.getDiagnostics(), not(nullValue()));
        assertThat(state.getDiagnostics().size(), is(0));
    }

    @Test
    public void deepCopyConstructor_stateArg_returnsNewInstanceWithCopiedValues() {
        StateChange stateChange = getStateChangeWithStartAndEndDate(PARTITIONING);
        State state = new State();
        state.getDiagnostics().add(new DiagnosticBuilder().build());
        state.updateState(stateChange);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PARTITIONING), Collections.singletonList(stateChange));

        // Assert that the begin date has been set
        assertThat(state.getPhase(PARTITIONING).getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getPhase(PARTITIONING).getEndDate(), not(nullValue()));

        // Call deep copy constructor
        State deepCopyState = new State(state);

        // Assert that the values have been copied correctly by the deep copy constructor
        assertThat(deepCopyState, is(state));

        // Assert that partitioning is done, that processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, false, false);
    }

    @Test
    public void updateState_changeStateIsNull_throws() {
        State state = new State();
        assertThrows(NullPointerException.class, () -> state.updateState(null));
    }

    @Test
    public void updateState_PhaseIsNull_throws() {
        State state = new State();
        StateChange stateChange = new StateChange();
        assertThrows(IllegalStateException.class, () -> state.updateState(stateChange));
    }

    //******************************************* DIAGNOSTICS ******************************************

    @Test
    public void fatalDiagnosticExists_fatalDiagnosticFound_returns() {
        State state = new State();
        Diagnostic warningDiagnostic = new DiagnosticBuilder().setLevel(Diagnostic.Level.WARNING).build();
        Diagnostic fatalDiagnostic = new DiagnosticBuilder().build();

        state.getDiagnostics().add(warningDiagnostic);
        assertThat(state.fatalDiagnosticExists(), is(false));

        state.getDiagnostics().add(fatalDiagnostic);
        assertThat(state.fatalDiagnosticExists(), is(true));
    }

    //******************************************* PARTITIONING ******************************************

    @Test
    public void updatePartitioning_noStartDate_starDateIsSet() {
        State state = new State();
        StateChange stateChange = getStateChangeWithEndDate(PARTITIONING);

        state.updateState(stateChange);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PARTITIONING), Collections.singletonList(stateChange));

        // Assert that the begin date has been set
        assertThat(state.getPhase(PARTITIONING).getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getPhase(PARTITIONING).getEndDate(), not(nullValue()));

        // Assert that partitioning is done, that processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, false, false);
    }

    @Test
    public void updatePartitioning_noEndDate_endDateIsNotSet() {
        State state = new State();
        StateChange stateChange = getStateChangeWithStartDate(PARTITIONING);

        state.updateState(stateChange);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PARTITIONING), Collections.singletonList(stateChange));

        // Assert that the begin date has been set
        assertThat(state.getPhase(PARTITIONING).getBeginDate(), not(nullValue()));

        // Assert that the end date has not been set
        assertThat(state.getPhase(PARTITIONING).getEndDate(), is(nullValue()));

        // Assert that partitioning processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, false, false, false, false);
    }

    @Test
    public void updatePartitioning_twoDifferentBeginDates_firstBeginDateNotOverwritten() {
        State state = new State();
        StateChange stateChangeA = getStateChangeWithStartDate(PARTITIONING);
        StateChange stateChangeB = getStateChangeWithStartDate(PARTITIONING);

        state.updateState(stateChangeA);
        state.updateState(stateChangeB);

        // Assert that the begin date has been set
        assertThat(state.getPhase(PARTITIONING).getBeginDate(), not(nullValue()));

        // Assert that the two beginDates are different
        assertThat(stateChangeA.getBeginDate(), not(stateChangeB.getBeginDate()));

        // Assert that the beginDate on the state element object is the first beginDate
        assertThat(state.getPhase(PARTITIONING).getBeginDate(), is(stateChangeA.getBeginDate()));

        // Assert that partitioning processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, false, false, false, false);
    }

    @Test
    public void updatePartitioning_twoDifferentEndDates_firstEndDateNotOverwritten() {
        State state = new State();
        StateChange stateChangeA = getStateChangeWithEndDate(PARTITIONING);
        StateChange stateChangeB = getStateChangeWithEndDate(PARTITIONING);

        state.updateState(stateChangeA);
        state.updateState(stateChangeB);

        // Assert that the begin date has been set
        assertThat(state.getPhase(PARTITIONING).getEndDate(), not(nullValue()));

        // Assert that the two endDates are different
        assertThat(stateChangeA.getEndDate(), not(stateChangeB.getEndDate()));

        // Assert that the endDate on the state element object is the first endDate
        assertThat(state.getPhase(PARTITIONING).getEndDate(), is(stateChangeA.getEndDate()));

        // Assert that partitioning is done, that processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, false, false);
    }

    @Test
    public void updatePartitioning_multipleStateChanges_partitioningIsUpdatedCorrectly() {
        State state = new State();
        List<StateChange> stateChangeList = getStateChangeList(PARTITIONING);

        updateState(state, stateChangeList);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PARTITIONING), stateChangeList);

        // Assert that partitioning is done, that processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, false, false);
    }

    //******************************************* PROCESSING ******************************************

    @Test
    public void updateProcessing_processingEndDateGiven_partitioningNotCompletedThrows() {
        State state = new State();
        StateChange stateChange = getStateChangeWithStartAndEndDate(PROCESSING);
        assertThrows(IllegalStateException.class, () -> state.updateState(stateChange));
    }

    @Test
    public void updateProcessing_noStartDateGiven_processingStarDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeProcessing = getStateChangeWithoutDates(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeProcessing);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PROCESSING), Collections.singletonList(stateChangeProcessing));

        // Assert that the begin date has been set
        assertThat(state.getPhase(PROCESSING).getBeginDate(), not(nullValue()));

        // Assert that the end date has not been set
        assertThat(state.getPhase(PROCESSING).getEndDate(), is(nullValue()));

        // Assert that partitioning is done, that processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, false, false);
    }

    @Test
    public void updateProcessing_processingEndDateGiven_processingEndDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeProcessing = getStateChangeWithStartAndEndDate(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeProcessing);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PROCESSING), Collections.singletonList(stateChangeProcessing));

        // Assert that the begin date has been set
        assertThat(state.getPhase(PROCESSING).getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getPhase(PROCESSING).getEndDate(), not(nullValue()));

        // Assert that partitioning and processing are done, that delivering is not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, true, false, false);
    }

    @Test
    public void updateProcessing_processingEndDateNotGiven_processingEndDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeProcessing = getStateChangeWithStartDate(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeProcessing);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PROCESSING), Collections.singletonList(stateChangeProcessing));

        // Assert that the begin date has been set
        assertThat(state.getPhase(PROCESSING).getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getPhase(PROCESSING).getEndDate(), not(nullValue()));

        // Assert that partitioning and processing are done, that delivering is not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, true, false, false);
    }

    @Test
    public void updateProcessing_twoDifferentBeginDates_firstBeginDateNotOverwritten() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeA = getStateChangeWithStartDate(PROCESSING);
        StateChange stateChangeB = getStateChangeWithStartDate(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeA);
        state.updateState(stateChangeB);

        // Assert that the begin date has been set
        assertThat(state.getPhase(PROCESSING).getBeginDate(), not(nullValue()));

        // Assert that the two beginDates are different
        assertThat(stateChangeA.getBeginDate(), not(stateChangeB.getBeginDate()));

        // Assert that the beginDate on the state element object is the first beginDate
        assertThat(state.getPhase(PROCESSING).getBeginDate(), is(stateChangeA.getBeginDate()));

        // Assert that partitioning and processing are done, that delivering is not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, true, false, false);
    }

    @Test
    public void updateProcessing_twoDifferentEndDates_firstEndDateNotOverwritten() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeA = getStateChangeWithEndDate(PROCESSING);
        StateChange stateChangeB = getStateChangeWithEndDate(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeA);
        state.updateState(stateChangeB);

        // Assert that the begin date has been set
        assertThat(state.getPhase(PROCESSING).getEndDate(), not(nullValue()));

        // Assert that the two endDates are different
        assertThat(stateChangeA.getEndDate(), not(stateChangeB.getEndDate()));

        // Assert that the endDate on the state element object is the first endDate
        assertThat(state.getPhase(PROCESSING).getEndDate(), is(stateChangeA.getEndDate()));

        // Assert that partitioning and processing are done, that delivering is not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, true, false, false);
    }

    @Test
    public void updateProcessing_multipleStateChanges_processingIsUpdatedCorrectly() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        List<StateChange> stateChangeList = getStateChangeList(PROCESSING);

        state.updateState(stateChangePartitioning);
        updateState(state, stateChangeList);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PROCESSING), stateChangeList);

        // Assert that partitioning and processing are done, that delivering is not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, true, false, false);
    }

    //******************************************* DELIVERING ******************************************

    @Test
    public void updateDelivering_deliveringEndDateGiven_partitioningNotCompletedThrows() {
        State state = new State();
        StateChange stateChange = getStateChangeWithStartAndEndDate(DELIVERING);
        assertThrows(IllegalStateException.class, () -> state.updateState(stateChange));
    }

    @Test
    public void updateDelivering_noStartDateGiven_deliveringStartDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeDelivering = getStateChangeWithoutDates(DELIVERING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeDelivering);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(DELIVERING), Collections.singletonList(stateChangeDelivering));

        // Assert that the begin date has been set
        assertThat(state.getPhase(DELIVERING).getBeginDate(), not(nullValue()));

        // Assert that the end date has not been set
        assertThat(state.getPhase(DELIVERING).getEndDate(), is(nullValue()));

        // Assert that partitioning is done, that processing and delivering are not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, false, false);
    }

    @Test
    public void updateDelivering_multipleStateChanges_deliveringIsUpdatedCorrectly() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        List<StateChange> stateChangeList = getStateChangeList(DELIVERING);

        state.updateState(stateChangePartitioning);
        updateState(state, stateChangeList);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(DELIVERING), stateChangeList);

        // Assert that partitioning and delivering are done, that processing is not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, true, false);
    }

    @Test
    public void updateDelivering_deliveringEndDateNotGiven_deliveringEndDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeDelivering = getStateChangeWithStartDate(DELIVERING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeDelivering);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(DELIVERING), Collections.singletonList(stateChangeDelivering));

        // Assert that the begin date has been set
        assertThat(state.getPhase(DELIVERING).getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getPhase(DELIVERING).getEndDate(), not(nullValue()));

        // Assert that partitioning and delivering are done, that processing is not done and
        // therefore: Assert that all phases are not done
        assertPhaseCompletion(state, true, false, true, false);
    }

    @Test
    public void updateAllPhases_allPhasesAreUpdatedWithEndDate_areAllPhasesDoneReturnsTrue() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeProcessing = getStateChangeWithStartDate(PROCESSING);
        StateChange stateChangeDelivering = getStateChangeWithStartDate(DELIVERING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeProcessing);
        state.updateState(stateChangeDelivering);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPhase(PARTITIONING), Collections.singletonList(stateChangePartitioning));
        assertStateAfterChange(state.getPhase(PROCESSING), Collections.singletonList(stateChangeProcessing));
        assertStateAfterChange(state.getPhase(DELIVERING), Collections.singletonList(stateChangeDelivering));

        // Assert all begin dates have been set
        assertThat(state.getPhase(PARTITIONING).getBeginDate(), not(nullValue()));
        assertThat(state.getPhase(PROCESSING).getBeginDate(), not(nullValue()));
        assertThat(state.getPhase(DELIVERING).getBeginDate(), not(nullValue()));

        // Assert all end dates have been set
        assertThat(state.getPhase(PARTITIONING).getEndDate(), not(nullValue()));
        assertThat(state.getPhase(PROCESSING).getEndDate(), not(nullValue()));
        assertThat(state.getPhase(DELIVERING).getEndDate(), not(nullValue()));

        // Assert that partitioning processing and delivering are done and
        // therefore: Assert that all phases are done
        assertPhaseCompletion(state, true, true, true, true);
    }

    /*
     * Private methods
     */

    private void updateState(State state, List<StateChange> stateChangeList) {
        for (StateChange stateChange : stateChangeList) {
            state.updateState(stateChange);
        }
    }

    private void assertPhaseCompletion(State state, boolean isPartitioningDone, boolean isProcessingDone, boolean isDeliveringDone, boolean areAllPhasesDone) {
        assertThat(state.phaseIsDone(PARTITIONING), is(isPartitioningDone));
        assertThat(state.phaseIsDone(PROCESSING), is(isProcessingDone));
        assertThat(state.phaseIsDone(DELIVERING), is(isDeliveringDone));
        assertThat(state.allPhasesAreDone(), is(areAllPhasesDone));
    }

    private void assertStateAfterChange(StateElement stateElement, List<StateChange> stateChangeList) {
        int succeeded = 0;
        int failed = 0;
        int ignored = 0;

        for (StateChange stateChange : stateChangeList) {
            succeeded = succeeded + stateChange.getSucceeded();
            failed = failed + stateChange.getFailed();
            ignored = ignored + stateChange.getIgnored();
        }

        assertThat(stateElement.getSucceeded(), is(succeeded));
        assertThat(stateElement.getFailed(), is(failed));
        assertThat(stateElement.getIgnored(), is(ignored));
    }

    private void assertState(State state) {
        assertThat(state.getPhase(State.Phase.PARTITIONING), not(nullValue()));
        assertThat(state.getPhase(State.Phase.PROCESSING), not(nullValue()));
        assertThat(state.getPhase(State.Phase.DELIVERING), not(nullValue()));
    }

    private void assertNewStateElement(StateElement stateElement) {
        assertThat(stateElement.getBeginDate(), is(nullValue()));
        assertThat(stateElement.getEndDate(), is(nullValue()));
        assertThat(stateElement.getSucceeded(), is(0));
        assertThat(stateElement.getFailed(), is(0));
        assertThat(stateElement.getIgnored(), is(0));
    }

    private StateChange getStateChangeWithEndDate(State.Phase phase) {
        StateChange stateChange = new StateChange();
        stateChange.setEndDate(new Date(System.currentTimeMillis() + random.nextInt((100000 - 10000) + 1) + 10000));
        stateChange.setSucceeded(10);
        stateChange.setPhase(phase);
        return stateChange;
    }

    private StateChange getStateChangeWithStartDate(State.Phase phase) {
        StateChange stateChange = new StateChange();
        stateChange.setBeginDate(new Date(System.currentTimeMillis() + random.nextInt((100000 - 10000) + 1) + 10000));
        stateChange.setSucceeded(10);
        stateChange.setPhase(phase);
        return stateChange;
    }

    private StateChange getStateChangeWithStartAndEndDate(State.Phase phase) {
        StateChange stateChange = new StateChange();
        stateChange.setBeginDate(BEGIN_DATE);
        stateChange.setEndDate(END_DATE);
        stateChange.setSucceeded(9);
        stateChange.setIgnored(1);
        stateChange.setPhase(phase);
        return stateChange;
    }

    private StateChange getStateChangeWithoutDates(State.Phase phase) {
        StateChange stateChange = new StateChange();
        stateChange.setSucceeded(8);
        stateChange.setPhase(phase);
        return stateChange;
    }

    private List<StateChange> getStateChangeList(State.Phase phase) {
        StateChange stateChangeA = new StateChange();
        stateChangeA.setBeginDate(BEGIN_DATE);
        stateChangeA.setSucceeded(1);
        stateChangeA.setPhase(phase);

        StateChange stateChangeB = new StateChange();
        stateChangeB.setSucceeded(5);
        stateChangeB.setPhase(phase);

        StateChange stateChangeC = new StateChange();
        stateChangeC.setEndDate(END_DATE);
        stateChangeC.setSucceeded(9);
        stateChangeC.setIgnored(1);
        stateChangeC.setPhase(phase);

        List<StateChange> stateChangeList = new ArrayList<>();
        stateChangeList.add(stateChangeA);
        stateChangeList.add(stateChangeB);
        stateChangeList.add(stateChangeC);
        return stateChangeList;
    }
}
