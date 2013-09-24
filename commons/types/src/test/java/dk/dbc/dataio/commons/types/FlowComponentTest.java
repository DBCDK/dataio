package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * FlowComponent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowComponentContent CONTENT = FlowComponentContentTest.newFlowComponentContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new FlowComponent(ID, VERSION, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowComponent instance = new FlowComponent(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    static FlowComponent newFlowComponentInstance() {
        return new FlowComponent(ID, VERSION, CONTENT);
    }
}
