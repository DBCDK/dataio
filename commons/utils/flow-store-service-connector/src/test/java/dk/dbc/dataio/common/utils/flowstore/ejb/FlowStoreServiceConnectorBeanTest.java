package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.naming.Context;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

public class FlowStoreServiceConnectorBeanTest {
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
        final FlowStoreServiceConnectorBean jobStoreServiceConnectorBean = newFlowStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void getConnector_connectorIsInitialized_connectorIsReturned() {
        FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = newFlowStoreServiceConnectorBean();
        flowStoreServiceConnectorBean.flowStoreServiceConnector = flowStoreServiceConnector;
        assertThat(flowStoreServiceConnectorBean.getConnector(), is(flowStoreServiceConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() {
        InMemoryInitialContextFactory.bind(ServiceUtil.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE, "someURL");
        FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = newFlowStoreServiceConnectorBean();
        flowStoreServiceConnectorBean.initializeConnector();
        assertThat(flowStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    /*
     * Private methods
     */
    private FlowStoreServiceConnectorBean newFlowStoreServiceConnectorBean() {
        return new FlowStoreServiceConnectorBean();
    }
}
