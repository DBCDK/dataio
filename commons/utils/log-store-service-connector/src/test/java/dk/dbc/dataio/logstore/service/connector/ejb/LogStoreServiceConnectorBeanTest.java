package dk.dbc.dataio.logstore.service.connector.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.naming.Context;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class LogStoreServiceConnectorBeanTest {
    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void clearContext() {
        InMemoryInitialContextFactory.clear();
    }

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowsNamingException_throws() {
        final LogStoreServiceConnectorBean logStoreServiceConnectorBean = newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void getConnector_connectorIsSet_connectorIsReturned() {
        LogStoreServiceConnector logStoreServiceConnector = mock(LogStoreServiceConnector.class);
        LogStoreServiceConnectorBean logStoreServiceConnectorBean = newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.logStoreServiceConnector = logStoreServiceConnector;
        assertThat(logStoreServiceConnectorBean.getConnector(), is(logStoreServiceConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() throws Exception{
        InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_LOGSTORE_RS, "someURL");
        LogStoreServiceConnectorBean logStoreServiceConnectorBean = newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.initializeConnector();
        assertThat(logStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

   /*
    * Private methods
    */
    private LogStoreServiceConnectorBean newLogStoreServiceConnectorBean() {
        return new LogStoreServiceConnectorBean();
    }
}