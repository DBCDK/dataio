package dk.dbc.dataio.logstore.service.connector.ejb;

import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class LogStoreServiceConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void initializeConnector_environmentNotSet_throws() {
        final LogStoreServiceConnectorBean jobStoreServiceConnectorBean = newLogStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("LOGSTORE_URL", "http://test");
        final LogStoreServiceConnectorBean logStoreServiceConnectorBean =
                newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.initializeConnector();

        assertThat(logStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final LogStoreServiceConnector logStoreServiceConnector =
                mock(LogStoreServiceConnector.class);
        final LogStoreServiceConnectorBean logStoreServiceConnectorBean =
                newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.logStoreServiceConnector = logStoreServiceConnector;

        assertThat(logStoreServiceConnectorBean.getConnector(),
                is(logStoreServiceConnector));
    }

    private LogStoreServiceConnectorBean newLogStoreServiceConnectorBean() {
        return new LogStoreServiceConnectorBean();
    }
}
