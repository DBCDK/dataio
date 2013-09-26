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
    public void constructor_idArgIsBelowThreshold_throws() {
        new Flow(Flow.ID_VERSION_LOWER_THRESHOLD, VERSION, CONTENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsBelowThreshold_throws() {
        new Flow(ID, Flow.ID_VERSION_LOWER_THRESHOLD, CONTENT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Flow instance = new Flow(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    public static Flow newFlowInstance() {
        return new Flow(ID, VERSION, CONTENT);
    }
}
