package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Flow unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowContent CONTENT = FlowContentTest.newFlowContentInstance();

    @Test
    public void constructor_contentArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Flow(ID, VERSION, null));
    }

    @Test
    public void constructor_idArgIsLessThanLowerBound_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Flow(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT));
    }

    @Test
    public void constructor_versionArgIsLessThanLowerBound_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Flow(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        Flow instance = new Flow(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    public static Flow newFlowInstance() {
        return new Flow(ID, VERSION, CONTENT);
    }
}
