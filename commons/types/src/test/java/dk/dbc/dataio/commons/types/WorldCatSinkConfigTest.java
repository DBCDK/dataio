package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * WorldCatSinkConfig unit tests
 */
public class WorldCatSinkConfigTest {
    private static final String USER_ID = "userId";
    private static final String PASSWORD = "password";
    private static final String PROJECT_ID = "projectId";
    private static final String ENDPOINT = "endpoint";
    private static final List<String> RETRY_DIAGNOSTICS = Arrays.asList("rt1", "rt2");

    @Test
    public void withRetryDiagnostics_retryDiagnosticsArgIsEmpty_returnsNewInstance() {
        WorldCatSinkConfig worldCatSinkConfig = new WorldCatSinkConfig().withRetryDiagnostics(Collections.emptyList());
        assertThat(worldCatSinkConfig.getRetryDiagnostics(), is(Collections.emptyList()));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstanceWithValuesSet() {
        final WorldCatSinkConfig worldCatSinkConfig = new WorldCatSinkConfig()
                .withUserId(USER_ID)
                .withPassword(PASSWORD)
                .withProjectId(PROJECT_ID)
                .withEndpoint(ENDPOINT)
                .withRetryDiagnostics(RETRY_DIAGNOSTICS);

        assertThat(worldCatSinkConfig.getUserId(), is(USER_ID));
        assertThat(worldCatSinkConfig.getPassword(), is(PASSWORD));
        assertThat(worldCatSinkConfig.getProjectId(), is(PROJECT_ID));
        assertThat(worldCatSinkConfig.getEndpoint(), is(ENDPOINT));
        assertThat(worldCatSinkConfig.getRetryDiagnostics(), is(RETRY_DIAGNOSTICS));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final WorldCatSinkConfig worldCatSinkConfig = new WorldCatSinkConfig();
        final WorldCatSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(worldCatSinkConfig), WorldCatSinkConfig.class);
        assertThat(unmarshalled, is(worldCatSinkConfig));
    }
}
