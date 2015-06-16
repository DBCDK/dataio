package dk.dbc.dataio.common.utils.flowstore.ejb;//package dk.dbc.dataio.common.utils.flowstore.ejb;
//
//import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
//import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
//import dk.dbc.dataio.commons.utils.service.ServiceUtil;
//import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
//import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
//import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
//import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
//import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import javax.ejb.EJBException;
//import javax.naming.NamingException;
//import javax.ws.rs.client.Client;
//
//import static org.hamcrest.CoreMatchers.is;
//import static org.junit.Assert.assertThat;
//import static org.junit.Assert.fail;
//import static org.powermock.api.mockito.PowerMockito.mock;
//import static org.powermock.api.mockito.PowerMockito.mockStatic;
//import static org.powermock.api.mockito.PowerMockito.when;
//
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({
//        HttpClient.class,
//        ServiceUtil.class
//})
//public class FlowStoreServiceConnectorBeanTest {
//
//    private final Client client = mock(Client.class);
//
//    @Before
//    public void setup() throws Exception {
//        mockStatic(ServiceUtil.class);
//        mockStatic(HttpClient.class);
//        String flowStoreUrl = "http://dataio/flow-store";
//        when(ServiceUtil.getFlowStoreServiceEndpoint())
//                .thenReturn(flowStoreUrl);
//        when(HttpClient.newClient()).thenReturn(client);
//    }
//
//    @Test
//    public void createSink_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.createSink(new SinkContentBuilder().build());
//            fail("No exception thrown by createSink()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void getSink_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            long sinkId = 1;
//            flowStoreServiceConnectorBean.getSink(sinkId);
//            fail("No exception thrown by getSink()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void findAllSinks_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.findAllSinks();
//            fail("No exception thrown by findAllSinks()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void updateSink_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.updateSink(new SinkContentBuilder().build(),1, 1);
//            fail("No exception thrown by updateSink()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void createSubmitter_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.createSubmitter(new SubmitterContentBuilder().build());
//            fail("No exception thrown by createSubmitter()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void getSubmitter_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.getSubmitter(1L);
//            fail("No exception thrown by getSubmitter()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void getSubmitterBySubmitterNumber_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.getSubmitterBySubmitterNumber(635242L);
//            fail("No exception thrown by getSubmitterBySubmitterNumber()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void updateSubmitter_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.updateSubmitter(new SubmitterContentBuilder().build(),1, 1);
//            fail("No exception thrown by updateSubmitter()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void findAllSubmitters_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.findAllSubmitters();
//            fail("No exception thrown by findAllSubmitters()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void createFlow_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.createFlow(new FlowContentBuilder().build());
//            fail("No exception thrown by createFlow()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void getFlow_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            long flowId = 1;
//            flowStoreServiceConnectorBean.getFlow(flowId);
//            fail("No exception thrown by getFlow()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void findAllFlows_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.findAllFlows();
//            fail("No exception thrown by findAllFlows()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void refreshComponents_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.refreshFlowComponents(1, 1);
//            fail("No exception thrown by updateFlowComponentsInFlowToLatestVersion()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void updateFlow_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.updateFlow(new FlowContentBuilder().build(),1, 1);
//            fail("No exception thrown by updateFlow()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void getFlowComponent_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            long flowComponentId = 1;
//            flowStoreServiceConnectorBean.getFlowComponent(flowComponentId);
//            fail("No exception thrown by getFlowComponent()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void createFlowComponent_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.createFlowComponent(new FlowComponentContentBuilder().build());
//            fail("No exception thrown by createFlowComponent()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void findAllFlowComponents_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.findAllFlowComponents();
//            fail("No exception thrown by findAllFlowComponents()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void updateFlowComponent_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.updateFlowComponent(new FlowComponentContentBuilder().build(),1, 1);
//            fail("No exception thrown by updateFlowComponent()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void createFlowBinder_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.createFlowBinder(new FlowBinderContentBuilder().build());
//            fail("No exception thrown by createFlowBinder()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void updateFlowBinder_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.updateFlowBinder(new FlowBinderContentBuilder().build(),1, 1);
//            fail("No exception thrown by updateFlowBinder()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    @Test
//    public void getFlowBinder_endpointLookupThrowsNamingException_throws() throws NamingException, FlowStoreServiceConnectorException {
//        final NamingException namingException = new NamingException();
//        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(namingException);
//        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = getInitializedBean();
//        try {
//            flowStoreServiceConnectorBean.getFlowBinder("packaging", "format", "charset", 22L, "destination");
//            fail("No exception thrown by getFlowBinder()");
//        } catch (EJBException e) {
//            assertThat((NamingException) e.getCause(), is(namingException));
//        }
//    }
//
//    private FlowStoreServiceConnectorBean getInitializedBean() {
//        return new FlowStoreServiceConnectorBean();
//    }
//}

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
