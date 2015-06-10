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
    private static final String DESCRIPTION = "description";

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SinkContent(null, RESOURCE, DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SinkContent("", RESOURCE, DESCRIPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resourceArgIsNull_throws() {
        new SinkContent(NAME, null, DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_resourceArgIsEmpty_throws() {
        new SinkContent(NAME, "", DESCRIPTION);
    }

    @Test
    public void constructor_descriptionArgIsEmpty_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, "");
    }

    @Test
    public void constructor_descriptionArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final SinkContent instance = new SinkContent(NAME, RESOURCE, DESCRIPTION);
        assertThat(instance, is(notNullValue()));
    }

    public static SinkContent newSinkContentInstance() {
        return new SinkContent(NAME, RESOURCE, DESCRIPTION);
    }
}
