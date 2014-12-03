package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.JobState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StateTest {

    private static final Date BEGIN_DATE = new Date(System.currentTimeMillis());
    private static final Date END_DATE = new Date(System.currentTimeMillis() + 1000000);
    private static final JobState.OperationalState PARTITIONING = JobState.OperationalState.CHUNKIFYING;
    private static final JobState.OperationalState PROCESSING = JobState.OperationalState.PROCESSING;
    private static final JobState.OperationalState DELIVERING = JobState.OperationalState.DELIVERING;
    private static final Random random = new Random();

    @Test
    public void constructor_noArgs_returnsNewInstanceWithInitializedStateElements() {
        State state = new State();
        assertState(state);
        assertNewStateElement(state.getPartitioning());
        assertNewStateElement(state.getProcessing());
        assertNewStateElement(state.getDelivering());
    }

    @Test(expected = NullPointerException.class)
    public void updateState_changeStateIsNull_throws() {
        State state = new State();
        state.updateState(null);
    }

    @Test(expected = IllegalStateException.class)
    public void updateState_operationalStateIsNull_throws() {
        State state = new State();
        StateChange stateChange = new StateChange();
        state.updateState(stateChange);
    }

    //******************************************* PARTITIONING ******************************************
    @Test
    public void updatePartitioning_noStartDate_starDateIsSet() {
        State state = new State();
        StateChange stateChange = getStateChangeWithEndDate(PARTITIONING);

        state.updateState(stateChange);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPartitioning(), Arrays.asList(stateChange));

        // Assert that the begin date has been set
        assertThat(state.getPartitioning().getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getPartitioning().getEndDate(), not(nullValue()));
    }

    @Test
    public void updatePartitioning_noEndDate_endDateIsNotSet() {
        State state = new State();
        StateChange stateChange = getStateChangeWithStartDate(PARTITIONING);

        state.updateState(stateChange);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPartitioning(), Arrays.asList(stateChange));

        // Assert that the begin date has been set
        assertThat(state.getPartitioning().getBeginDate(), not(nullValue()));

        // Assert that the end date has not been set
        assertThat(state.getPartitioning().getEndDate(), is(nullValue()));
    }

    @Test
    public void updatePartitioning_twoDifferentBeginDates_firstBeginDateNotOverwritten() {
        State state = new State();
        StateChange stateChangeA = getStateChangeWithStartDate(PARTITIONING);
        StateChange stateChangeB = getStateChangeWithStartDate(PARTITIONING);

        state.updateState(stateChangeA);
        state.updateState(stateChangeB);

        // Assert that the begin date has been set
        assertThat(state.getPartitioning().getBeginDate(), not(nullValue()));

        // Assert that the two beginDates are different
        assertThat(stateChangeA.getBeginDate(), not(stateChangeB.getBeginDate()));

        // Assert that the beginDate on the state element object is the first beginDate
        assertThat(state.getPartitioning().getBeginDate(), is(stateChangeA.getBeginDate()));
    }

    @Test
    public void updatePartitioning_twoDifferentEndDates_firstEndDateNotOverwritten() {
        State state = new State();
        StateChange stateChangeA = getStateChangeWithEndDate(PARTITIONING);
        StateChange stateChangeB = getStateChangeWithEndDate(PARTITIONING);

        state.updateState(stateChangeA);
        state.updateState(stateChangeB);

        // Assert that the begin date has been set
        assertThat(state.getPartitioning().getEndDate(), not(nullValue()));

        // Assert that the two endDates are different
        assertThat(stateChangeA.getEndDate(), not(stateChangeB.getEndDate()));

        // Assert that the endDate on the state element object is the first endDate
        assertThat(state.getPartitioning().getEndDate(), is(stateChangeA.getEndDate()));
    }

    @Test
    public void updatePartitioning_multipleStateChanges_partitioningIsUpdatedCorrectly() {
        State state = new State();
        List<StateChange> stateChangeList = getStateChangeList(PARTITIONING);

        updateState(state, stateChangeList);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getPartitioning(), stateChangeList);
    }

    //******************************************* PROCESSING ******************************************

    @Test(expected = IllegalStateException.class)
    public void updateProcessing_processingEndDateGiven_partitioningNotCompletedThrows() {
        State state = new State();
        StateChange stateChange = getStateChangeWithStartAndEndDate(PROCESSING);

        state.updateState(stateChange);
    }

    @Test
    public void updateProcessing_noStartDateGiven_processingStarDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeProcessing = getStateChangeWithoutDates(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeProcessing);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getProcessing(), Arrays.asList(stateChangeProcessing));

        // Assert that the begin date has been set
        assertThat(state.getProcessing().getBeginDate(), not(nullValue()));

        // Assert that the end date has not been set
        assertThat(state.getProcessing().getEndDate(), is(nullValue()));
    }

    @Test
    public void updateProcessing_processingEndDateGiven_processingEndDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeProcessing = getStateChangeWithStartAndEndDate(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeProcessing);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getProcessing(), Arrays.asList(stateChangeProcessing));

        // Assert that the begin date has been set
        assertThat(state.getProcessing().getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getProcessing().getEndDate(), not(nullValue()));
    }

    @Test
    public void updateProcessing_processingEndDateNotGiven_processingEndDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeProcessing = getStateChangeWithStartDate(PROCESSING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeProcessing);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getProcessing(), Arrays.asList(stateChangeProcessing));

        // Assert that the begin date has been set
        assertThat(state.getProcessing().getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getProcessing().getEndDate(), not(nullValue()));
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
        assertThat(state.getProcessing().getBeginDate(), not(nullValue()));

        // Assert that the two beginDates are different
        assertThat(stateChangeA.getBeginDate(), not(stateChangeB.getBeginDate()));

        // Assert that the beginDate on the state element object is the first beginDate
        assertThat(state.getProcessing().getBeginDate(), is(stateChangeA.getBeginDate()));
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
        assertThat(state.getProcessing().getEndDate(), not(nullValue()));

        // Assert that the two endDates are different
        assertThat(stateChangeA.getEndDate(), not(stateChangeB.getEndDate()));

        // Assert that the endDate on the state element object is the first endDate
        assertThat(state.getProcessing().getEndDate(), is(stateChangeA.getEndDate()));
    }

    @Test
    public void updateProcessing_multipleStateChanges_processingIsUpdatedCorrectly() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        List<StateChange> stateChangeList = getStateChangeList(PROCESSING);

        state.updateState(stateChangePartitioning);
        updateState(state, stateChangeList);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getProcessing(), stateChangeList);
    }

    //******************************************* DELIVERING ******************************************

    @Test(expected = IllegalStateException.class)
    public void updateDelivering_deliveringEndDateGiven_partitioningNotCompletedThrows() {
        State state = new State();
        StateChange stateChange = getStateChangeWithStartAndEndDate(DELIVERING);

        state.updateState(stateChange);
    }

    @Test
    public void updateDelivering_noStartDateGiven_deliveringStarDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeDelivering = getStateChangeWithoutDates(DELIVERING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeDelivering);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getDelivering(), Arrays.asList(stateChangeDelivering));

        // Assert that the begin date has been set
        assertThat(state.getDelivering().getBeginDate(), not(nullValue()));

        // Assert that the end date has not been set
        assertThat(state.getDelivering().getEndDate(), is(nullValue()));
    }

    @Test
    public void updateDelivering_multipleStateChanges_deliveringIsUpdatedCorrectly() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        List<StateChange> stateChangeList = getStateChangeList(DELIVERING);

        state.updateState(stateChangePartitioning);
        updateState(state, stateChangeList);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getDelivering(), stateChangeList);
    }

    @Test
    public void updateDelivering_deliveringEndDateNotGiven_deliveringEndDateIsSet() {
        State state = new State();
        StateChange stateChangePartitioning = getStateChangeWithStartAndEndDate(PARTITIONING);
        StateChange stateChangeDelivering = getStateChangeWithStartDate(DELIVERING);

        state.updateState(stateChangePartitioning);
        state.updateState(stateChangeDelivering);

        // Assert that State has been updated correctly
        assertStateAfterChange(state.getDelivering(), Arrays.asList(stateChangeDelivering));

        // Assert that the begin date has been set
        assertThat(state.getDelivering().getBeginDate(), not(nullValue()));

        // Assert that the end date has been set
        assertThat(state.getDelivering().getEndDate(), not(nullValue()));
    }


    /*
     * Private methods
     */

    private void updateState(State state, List<StateChange> stateChangeList) {
        for(StateChange stateChange : stateChangeList) {
            state.updateState(stateChange);
        }
    }

    private void assertStateAfterChange(StateElement stateElement, List<StateChange> stateChangeList) {
        int succeeded = 0;
        int failed = 0;
        int ignored = 0;
        int pending = 0;
        int active = 0;
        int done = 0;

        for (StateChange stateChange : stateChangeList) {
            succeeded = succeeded + stateChange.getSucceeded();
            failed = failed + stateChange.getFailed();
            ignored = ignored + stateChange.getIgnored();
            pending = pending + stateChange.getPending();
            active = active + stateChange.getActive();
            done = done + stateChange.getSucceeded() + stateChange.getFailed() + stateChange.getIgnored();
        }

        assertThat(stateElement.getSucceeded(), is(succeeded));
        assertThat(stateElement.getFailed(), is(failed));
        assertThat(stateElement.getIgnored(), is(ignored));
        assertThat(stateElement.getPending(), is(pending));
        assertThat(stateElement.getActive(), is(active));
        assertThat(stateElement.getDone(), is(done));
    }

    private void assertState(State state) {
        assertThat(state.getPartitioning(), not(nullValue()));
        assertThat(state.getProcessing(), not(nullValue()));
        assertThat(state.getDelivering(), not(nullValue()));
    }

    private void assertNewStateElement(StateElement stateElement) {
        assertThat(stateElement.getBeginDate(), is(nullValue()));
        assertThat(stateElement.getEndDate(), is(nullValue()));
        assertThat(stateElement.getPending(), is(0));
        assertThat(stateElement.getActive(), is(0));
        assertThat(stateElement.getDone(), is(0));
        assertThat(stateElement.getSucceeded(), is(0));
        assertThat(stateElement.getFailed(), is(0));
        assertThat(stateElement.getIgnored(), is(0));
    }

    private StateChange getStateChangeWithEndDate(JobState.OperationalState operationalState) {
        StateChange stateChange = new StateChange();
        stateChange.setEndDate(new Date(System.currentTimeMillis() + random.nextInt((100000 - 10000) + 1) + 1000));
        stateChange.setSucceeded(10);
        stateChange.setOperationalState(operationalState);
        return stateChange;
    }

    private StateChange getStateChangeWithStartDate(JobState.OperationalState operationalState) {
        StateChange stateChange = new StateChange();
        stateChange.setBeginDate(new Date(System.currentTimeMillis() + random.nextInt((100000 - 10000) + 1) + 1000));
        stateChange.setSucceeded(10);
        stateChange.setOperationalState(operationalState);
        return stateChange;
    }

    private StateChange getStateChangeWithStartAndEndDate(JobState.OperationalState operationalState) {
        StateChange stateChange = new StateChange();
        stateChange.setBeginDate(BEGIN_DATE);
        stateChange.setEndDate(END_DATE);
        stateChange.setSucceeded(9);
        stateChange.setIgnored(1);
        stateChange.setOperationalState(operationalState);
        return stateChange;
    }

    private StateChange getStateChangeWithoutDates(JobState.OperationalState operationalState) {
        StateChange stateChange = new StateChange();
        stateChange.setSucceeded(8);
        stateChange.setOperationalState(operationalState);
        return stateChange;
    }

    private List<StateChange> getStateChangeList(JobState.OperationalState operationalState) {
        StateChange stateChangeA = new StateChange();
        stateChangeA.setBeginDate(BEGIN_DATE);
        stateChangeA.setSucceeded(10);
        stateChangeA.setPending(35);
        stateChangeA.setActive(15);
        stateChangeA.setOperationalState(operationalState);

        StateChange stateChangeB = new StateChange();
        stateChangeB.setSucceeded(10);
        stateChangeB.setPending(-10);
        stateChangeB.setActive(-10);
        stateChangeB.setOperationalState(operationalState);

        StateChange stateChangeC = new StateChange();
        stateChangeC.setEndDate(END_DATE);
        stateChangeC.setSucceeded(34);
        stateChangeC.setIgnored(1);
        stateChangeC.setPending(-25);
        stateChangeC.setActive(-5);
        stateChangeC.setOperationalState(operationalState);

        List<StateChange> stateChangeList = new ArrayList<>();
        stateChangeList.add(stateChangeA);
        stateChangeList.add(stateChangeB);
        stateChangeList.add(stateChangeC);
        return stateChangeList;
    }
}
