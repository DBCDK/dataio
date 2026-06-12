package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * OpenUpdateSinkConfig unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class OpenUpdateSinkConfigTest {

    @Test
    public void withUserId_userIdArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withUserId(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withUserId_userIdArgIsEmpty_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withUserId(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void withPassword_passwordArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withPassword(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withPassword_passwordArgIsEmpty_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withPassword(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void withEndpoint_webUrlArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withEndpoint(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withEndpoint_webUrlArgIsEmpty_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withEndpoint(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void withAvailableQueueProviders_queueProviderArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withAvailableQueueProviders(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withQueueProvider_queueProviderArgIsEmpty_returnsNewInstance() {
        OpenUpdateSinkConfig openUpdateSinkConfig = new OpenUpdateSinkConfig().withAvailableQueueProviders(Collections.emptyList());
        assertThat(openUpdateSinkConfig.getAvailableQueueProviders(), is(Collections.emptyList()));
    }

    @Test
    public void marshalling() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        OpenUpdateSinkConfig openUpdateSinkConfig = config();
        OpenUpdateSinkConfig unmarshalled = jsonbContext.unmarshall(
                jsonbContext.marshall(openUpdateSinkConfig),
                OpenUpdateSinkConfig.class);
        assertThat(unmarshalled, is(openUpdateSinkConfig));
    }

    @Test
    public void marshalling_withIgnoredFields() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        OpenUpdateSinkConfig openUpdateSinkConfig = config()
                .withGroupId("group")
                .withValidateOnly(false);
        OpenUpdateSinkConfig unmarshalled = jsonbContext.unmarshall(
                jsonbContext.marshall(openUpdateSinkConfig),
                OpenUpdateSinkConfig.class);
        assertThat("unmarshalled", unmarshalled, is(not(openUpdateSinkConfig)));
        assertThat("unmarshalled.getGroupId", unmarshalled.getGroupId(), is(nullValue()));
        assertThat("unmarshalled.isValidateOnly", unmarshalled.isValidateOnly(), is(nullValue()));
    }

    public static OpenUpdateSinkConfig config() {
        return new OpenUpdateSinkConfig()
                .withUserId("user")
                .withPassword("secret")
                .withEndpoint("http://update-service")
                .withAvailableQueueProviders(List.of("qp1", "qp2"));
    }
}
