package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * OpenUpdateSinkConfig unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class OpenUpdateSinkConfigTest {
    private static final String USER_ID = "userId";
    private static final String PASSWORD = "password";
    private static final String ENDPOINT = "endpoint";

    @Test(expected = NullPointerException.class)
    public void constructor_userIdArgIsNull_throws() {
        new OpenUpdateSinkConfig(null, PASSWORD, ENDPOINT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_userIdArgIsEmpty_throws() {
        new OpenUpdateSinkConfig("", PASSWORD, ENDPOINT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_passwordArgIsNull_throws() {
        new OpenUpdateSinkConfig(USER_ID, null, ENDPOINT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_passwordArgIsEmpty_throws() {
        new OpenUpdateSinkConfig(USER_ID, "", ENDPOINT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_webUrlArgIsNull_throws() {
        new OpenUpdateSinkConfig(USER_ID, PASSWORD, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_wenUrlArgIsEmpty_throws() {
        new OpenUpdateSinkConfig(USER_ID, PASSWORD, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final OpenUpdateSinkConfig instance = new OpenUpdateSinkConfig(USER_ID, PASSWORD, ENDPOINT);
        assertThat(instance, is(notNullValue()));
    }

    public static OpenUpdateSinkConfig newOpenUpdateSinkConfigInstance() {
        return new OpenUpdateSinkConfig(USER_ID, PASSWORD, ENDPOINT);
    }

}
