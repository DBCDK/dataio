package dk.dbc.dataio.logstore.service.connector;

import org.junit.Test;

import javax.ws.rs.client.Client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class LogStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String LOG_STORE_URL = "http://dataio/log-store";
    private static final String JOB_ID = "jobId";
    private static final long CHUNK_ID = 42;
    private static final long ITEM_ID = 1;

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new LogStoreServiceConnector(null, LOG_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new LogStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new LogStoreServiceConnector(CLIENT, "");
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
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getItemLog_jobIdArgIsEmpty_throws() throws LogStoreServiceConnectorException {
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        try {
            logStoreServiceConnector.getItemLog("", CHUNK_ID, ITEM_ID);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    private LogStoreServiceConnector newLogStoreServiceConnector() {
        return new LogStoreServiceConnector(CLIENT, LOG_STORE_URL);
    }
}
