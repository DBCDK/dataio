package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * SinkContent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkContentTest {
    private static final String NAME = "name";
    private static final String RESOURCE = "resource";

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SinkContent(null, RESOURCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SinkContent("", RESOURCE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resourceArgIsNull_throws() {
        new SinkContent(NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_resourceArgIsEmpty_throws() {
        new SinkContent(NAME, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final SinkContent instance = new SinkContent(NAME, RESOURCE);
        assertThat(instance, is(notNullValue()));
    }

    public static SinkContent newSinkContentInstance() {
        return new SinkContent(NAME, RESOURCE);
    }
}
