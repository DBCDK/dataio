package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowComponent unit tests
 * The test methods of this class uses the following naming convention:
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowComponentContent CONTENT = FlowComponentContentTest.newFlowComponentContentInstance();
    private static final FlowComponentContent NEXT = FlowComponentContentTest.newFlowComponentContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new FlowComponent(ID, VERSION, null, NEXT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsBelowBound_throws() {
        new FlowComponent(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT, NEXT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsBelowBound_throws() {
        new FlowComponent(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT, NEXT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowComponent instance = new FlowComponent(ID, VERSION, CONTENT, NEXT);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_nextArgCanBeNull_returnsNewInstance() {
        final FlowComponent instance = new FlowComponent(ID, VERSION, CONTENT, FlowComponent.UNDEFINED_NEXT);
        assertThat(instance, is(notNullValue()));
    }

    public static FlowComponent newFlowComponentInstance() {
        return new FlowComponent(ID, VERSION, CONTENT, FlowComponent.UNDEFINED_NEXT);
    }
}
