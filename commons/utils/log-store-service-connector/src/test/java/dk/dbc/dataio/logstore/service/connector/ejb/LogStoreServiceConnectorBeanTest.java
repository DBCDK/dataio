package dk.dbc.dataio.logstore.service.connector.ejb;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LogStoreServiceConnectorBeanTest {
    @Test
    public void initializeConnector_environmentNotSet_throws() {
        assertThrows(NullPointerException.class, LogStoreServiceConnectorBean::new);
    }

    @Test
    public void initializeConnector() {
        final LogStoreServiceConnectorBean logStoreServiceConnectorBean = new LogStoreServiceConnectorBean("http://test");
        assertThat(logStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }
}
