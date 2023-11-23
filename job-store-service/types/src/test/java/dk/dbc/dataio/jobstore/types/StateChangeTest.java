package dk.dbc.dataio.jobstore.types;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StateChangeTest {

    @Test
    public void constructor_noArgs_returnsNewInstanceWithDefaultValues() {
        StateChange stateChange = new StateChange();
        assertNewStateChange(stateChange);
    }

    @Test
    public void setSucceeded_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        assertThrows(IllegalArgumentException.class, () -> stateChange.setSucceeded(-1));
    }

    @Test
    public void setFailed_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        Assertions.assertThrows(IllegalArgumentException.class, () -> stateChange.setFailed(-1));
    }

    @Test
    public void setIgnored_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        assertThrows(IllegalArgumentException.class, () -> stateChange.setIgnored(-1));
    }

    @Test
    public void incSucceeded_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        assertThrows(IllegalArgumentException.class, () -> stateChange.incSucceeded(-1));
    }

    @Test
    public void incSucceeded_increments() {
        StateChange stateChange = new StateChange();
        stateChange.incSucceeded(1);
        assertThat(stateChange.getSucceeded(), is(1));
        stateChange.incSucceeded(9);
        assertThat(stateChange.getSucceeded(), is(10));
    }

    @Test
    public void incFailed_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        assertThrows(IllegalArgumentException.class, () -> stateChange.incFailed(-1));
    }

    @Test
    public void incFailed_increments() {
        StateChange stateChange = new StateChange();
        stateChange.incFailed(1);
        assertThat(stateChange.getFailed(), is(1));
        stateChange.incFailed(9);
        assertThat(stateChange.getFailed(), is(10));
    }

    @Test
    public void incIgnored_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        assertThrows(IllegalArgumentException.class, () -> stateChange.incIgnored(-1));
    }

    @Test
    public void incIgnored_increments() {
        StateChange stateChange = new StateChange();
        stateChange.incIgnored(1);
        assertThat(stateChange.getIgnored(), is(1));
        stateChange.incIgnored(9);
        assertThat(stateChange.getIgnored(), is(10));
    }

    @Test
    public void setAllValues_allValuesAreSet_valuesAreCorrect() {
        Date DATE = new Date(System.currentTimeMillis());
        StateChange stateChange = new StateChange();
        stateChange.setBeginDate(DATE);
        stateChange.setEndDate(DATE);
        stateChange.setSucceeded(10);
        stateChange.setFailed(2);
        stateChange.setIgnored(3);
        stateChange.setPhase(State.Phase.PROCESSING);

        assertThat(stateChange.getBeginDate(), is(DATE));
        assertThat(stateChange.getEndDate(), is(DATE));
        assertThat(stateChange.getSucceeded(), is(10));
        assertThat(stateChange.getFailed(), is(2));
        assertThat(stateChange.getIgnored(), is(3));
        assertThat(stateChange.getPhase(), is(State.Phase.PROCESSING));
    }

    /*
     * Private methods
     */

    private void assertNewStateChange(StateChange stateChange) {
        assertThat(stateChange.getBeginDate(), is(nullValue()));
        assertThat(stateChange.getEndDate(), is(nullValue()));
        assertThat(stateChange.getSucceeded(), is(0));
        assertThat(stateChange.getFailed(), is(0));
        assertThat(stateChange.getIgnored(), is(0));
        assertThat(stateChange.getPhase(), is(nullValue()));
    }
}
