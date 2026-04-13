package dk.dbc.dataio.logstore.service.connector;

import dk.dbc.commons.useragent.UserAgent;
import jakarta.ws.rs.client.Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class LogStoreServiceConnectorTest {
    private static final UserAgent USER_AGENT = new UserAgent(LogStoreServiceConnectorTest.class.getSimpleName());
    private static final Client CLIENT = mock(Client.class);
    private static final String LOG_STORE_URL = "http://dataio/log-store";
    private static final String JOB_ID = "jobId";
    private static final long CHUNK_ID = 42;
    private static final long ITEM_ID = 1;

    @Test
    public void constructor_httpClientArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new LogStoreServiceConnector(null, USER_AGENT, LOG_STORE_URL));
    }

    @Test
    public void constructor_baseUrlArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new LogStoreServiceConnector(CLIENT, USER_AGENT, null));
    }

    @Test
    public void constructor_baseUrlArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new LogStoreServiceConnector(CLIENT, USER_AGENT, ""));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        assertThat(logStoreServiceConnector, is(notNullValue()));
        assertThat(logStoreServiceConnector.getHttpClient(), is(CLIENT));
        assertThat(logStoreServiceConnector.getBaseUrl(), is(LOG_STORE_URL));
    }

    @Test
    public void getItemLog_jobIdArgIsNull_throws() throws LogStoreServiceConnectorException {
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        try {
            logStoreServiceConnector.getItemLog(null, CHUNK_ID, ITEM_ID);
            Assertions.fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getItemLog_jobIdArgIsEmpty_throws() throws LogStoreServiceConnectorException {
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        try {
            logStoreServiceConnector.getItemLog("", CHUNK_ID, ITEM_ID);
            Assertions.fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    private LogStoreServiceConnector newLogStoreServiceConnector() {
        return new LogStoreServiceConnector(CLIENT, USER_AGENT, LOG_STORE_URL);
    }
}
