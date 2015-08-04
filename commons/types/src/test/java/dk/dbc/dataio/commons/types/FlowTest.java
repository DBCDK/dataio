package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Flow unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowContent CONTENT = FlowContentTest.newFlowContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new Flow(ID, VERSION, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsLessThanLowerBound_throws() {
        new Flow(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsLessThanLowerBound_throws() {
        new Flow(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Flow instance = new Flow(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void hasNextComponents_flowHasNoComponentsContainingNextEntries_returnsFalse() {
        assertThat(newFlowInstance().hasNextComponents(), is(false));
    }

    public static Flow newFlowInstance() {
        return new Flow(ID, VERSION, CONTENT);
    }
}
