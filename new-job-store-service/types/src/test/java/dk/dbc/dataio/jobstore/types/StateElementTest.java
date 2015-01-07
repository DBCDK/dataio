package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StateElementTest {

    @Test
    public void constructor_noArgs_returnsNewInstanceWithInitializedStateElements() {
        StateElement stateElement = new StateElement();
        assertStateElement(stateElement);
    }

    @Test
    public void deepCopyConstructor_stateElementArg_returnsNewInstanceWithCopiedValues() {
        StateElement stateElement = getStateElement();
        StateElement stateElementDeepCopy = new StateElement(stateElement);
        assertThat(stateElementDeepCopy, is(stateElement));
    }

    /*
     * Private methods
     */

    private void assertStateElement(StateElement stateElement) {
        assertThat(stateElement.getBeginDate(), is(nullValue()));
        assertThat(stateElement.getEndDate(), is(nullValue()));
        assertThat(stateElement.getPending(), is(0));
        assertThat(stateElement.getActive(), is(0));
        assertThat(stateElement.getDone(), is(0));
        assertThat(stateElement.getSucceeded(), is(0));
        assertThat(stateElement.getFailed(), is(0));
        assertThat(stateElement.getIgnored(), is(0));
    }

    private StateElement getStateElement() {
        StateElement stateElement = new StateElement();
        stateElement.setDone(10);
        stateElement.setActive(5);
        stateElement.setPending(5);
        stateElement.setSucceeded(9);
        stateElement.setIgnored(1);
        return stateElement;
    }
}
