package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceUtil.class
})
public class FlowStoreServiceConnectorBeanTest {

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final FlowStoreServiceConnectorBean jobStoreServiceConnectorBean = new FlowStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void setConnector_connectorIsSet_connectorIsReturned() {
        FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
        flowStoreServiceConnectorBean.flowStoreServiceConnector = flowStoreServiceConnector;

        assertThat(flowStoreServiceConnectorBean.getConnector(), is(flowStoreServiceConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() throws Exception{
        mockStatic(ServiceUtil.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn("flowStoreEndpoint");
        FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
        flowStoreServiceConnectorBean.initializeConnector();

        assertThat(flowStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    /*
     * Private methods
     */
    private FlowStoreServiceConnectorBean getInitializedBean() {
        return new FlowStoreServiceConnectorBean();
    }
}
