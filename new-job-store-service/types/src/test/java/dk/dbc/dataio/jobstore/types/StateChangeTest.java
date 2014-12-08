package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class StateChangeTest {

    @Test
    public void constructor_noArgs_returnsNewInstanceWithDefaultValues() {
        StateChange stateChange = new StateChange();
        assertNewStateChange(stateChange);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSucceeded_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.setSucceeded(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setFailed_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.setFailed(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIgnored_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.setIgnored(-1);
    }

    @Test
    public void setAllValues_allValuesAreSet_valuesAreCorrect() {
        final Date DATE = new Date(System.currentTimeMillis());
        StateChange stateChange = new StateChange();
        stateChange.setBeginDate(DATE);
        stateChange.setEndDate(DATE);
        stateChange.setSucceeded(10);
        stateChange.setFailed(2);
        stateChange.setIgnored(3);
        stateChange.setPending(-5);
        stateChange.setActive(-4);
        stateChange.setPhase(State.Phase.PROCESSING);

        assertThat(stateChange.getBeginDate(), is(DATE));
        assertThat(stateChange.getEndDate(), is(DATE));
        assertThat(stateChange.getSucceeded(), is(10));
        assertThat(stateChange.getFailed(), is(2));
        assertThat(stateChange.getIgnored(), is(3));
        assertThat(stateChange.getPending(), is(-5));
        assertThat(stateChange.getActive(), is(-4));
        assertThat(stateChange.getPhase(), is(State.Phase.PROCESSING));
    }

    /*
     * Private methods
     */

    private void assertNewStateChange(StateChange stateChange) {
        assertThat(stateChange.getBeginDate(), is(nullValue()));
        assertThat(stateChange.getEndDate(), is(nullValue()));
        assertThat(stateChange.getPending(), is(0));
        assertThat(stateChange.getActive(), is(0));
        assertThat(stateChange.getSucceeded(), is(0));
        assertThat(stateChange.getFailed(), is(0));
        assertThat(stateChange.getIgnored(), is(0));
        assertThat(stateChange.getPhase(), is(nullValue()));
    }
}
