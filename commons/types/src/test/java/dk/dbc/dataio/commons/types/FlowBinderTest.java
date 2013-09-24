package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * FlowBinder unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowBinderTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowBinderContent CONTENT = FlowBinderContentTest.newFlowBinderContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new FlowBinder(ID, VERSION, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowBinder instance = new FlowBinder(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    static FlowBinder newFlowBinderInstance() {
        return new FlowBinder(ID, VERSION, CONTENT);
    }
}
