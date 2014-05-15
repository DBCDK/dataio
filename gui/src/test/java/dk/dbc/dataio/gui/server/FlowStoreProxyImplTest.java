package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,
    ServiceUtil.class
})
public class FlowStoreProxyImplTest {
    private final String flowStoreServiceUrl = "http://dataio/flow-service";
    private final Client client = mock(Client.class);
    private final static long SINK_ID = 1L;

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn(flowStoreServiceUrl);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }

    @Test
    public void noArgs_flowStoreProxyConstructorFlowStoreService_EndpointCanNotBeLookedUp_throws() throws Exception{
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());
        try{
            new FlowStoreProxyImpl();
            fail();
        }catch (NamingException e){
        }
    }

    @Test
    public void oneArg_flowStoreProxyConstructorFlowStoreService_EndpointCanNotBeLookedUp_throws1() throws Exception{
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());
        try{
            new FlowStoreProxyImpl(flowStoreServiceConnector);
            fail();
        }catch (NamingException e){
        }
    }

    /*
    * Test createSink
    */
    @Test
    public void createSink_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        SinkContent sinkContent = new SinkContentBuilder().build();
        when(flowStoreServiceConnector.createSink(eq(sinkContent))).
                thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.createSink(sinkContent);
            fail("No INTERNAL_SERVER_ERROR error was thrown by createSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void createSink_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        SinkContent sinkContent = new SinkContentBuilder().build();
        when(flowStoreServiceConnector.createSink(eq(sinkContent))).
                thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 406));
        try {
            flowStoreProxy.createSink(sinkContent);
            fail("No NOT_ACCEPTABLE error was thrown by createSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.NOT_ACCEPTABLE));
        }
    }

    @Test
    public void createSink_remoteServiceReturnsHttpStatusCreated_returnsSinkEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final SinkContent sinkContent = new SinkContentBuilder().build();
        final Sink sink = new SinkBuilder().build();
        when(flowStoreServiceConnector.createSink(sinkContent)).thenReturn(sink);

        try {
            final Sink createdSink  = flowStoreProxy.createSink(sinkContent);
            assertNotNull(createdSink);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createSink()");
        }
    }

    /*
    * Test getSink
    */
    @Test
    public void getSink_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getSink(eq(SINK_ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.getSink(SINK_ID);
            fail("No INTERNAL_SERVER_ERROR was thrown by getSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void getSink_remoteServiceReturnsHttpStatusOk_returnsSinkEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(SINK_ID).build();
        when(flowStoreServiceConnector.getSink(eq(SINK_ID))).thenReturn(sink);

        try {
            final Sink retrievedSink  = flowStoreProxy.getSink(sink.getId());
            assertNotNull(retrievedSink);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getSink()");
        }
    }

    @Test
    public void getSink_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getSink(eq(SINK_ID))).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 404));

        try {
            flowStoreProxy.getSink(SINK_ID);
            fail("No ENTITY_NOT_FOUND error was thrown by getSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.ENTITY_NOT_FOUND));
        }
    }

    /*
     * Test findAllSinks
     */
    @Test
    public void findAllSinks_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findAllSinks()).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.findAllSinks();
            fail("No INTERNAL_SERVER_ERROR was thrown by findAllSinks()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllSinks_remoteServiceReturnsHttpStatusOk_returnsListOfSinkEntity() throws Exception {

        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(SINK_ID).build();

        when(flowStoreServiceConnector.findAllSinks()).thenReturn(Arrays.asList(sink));
        try {
            final List<Sink> allSinks  = flowStoreProxy.findAllSinks();
            assertNotNull(allSinks);
            assertThat(allSinks.size(), is(1));
            assertThat(allSinks.get(0).getId(), is(sink.getId()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: findAllSinks()");
        }
    }

    /*
     * Test updateSink
     */
    @Test
    public void updateSink_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(SINK_ID).setVersion(1).build();
        when(flowStoreServiceConnector.updateSink(eq(sink.getContent()),(eq(sink.getId())),(eq(sink.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 500));
        try {
            flowStoreProxy.updateSink(sink.getContent(), sink.getId(), sink.getVersion());
            fail("No INTERNAL_SERVER_ERROR was thrown by updateSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusConflict_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(SINK_ID).setVersion(1).build();
        when(flowStoreServiceConnector.updateSink(eq(sink.getContent()),(eq(sink.getId())),(eq(sink.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 409));
        try {
            flowStoreProxy.updateSink(sink.getContent(), sink.getId(), sink.getVersion());
            fail("No CONFLICT_ERROR was thrown by updateSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.CONFLICT_ERROR));
        }
    }

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusNotAcceptable_throws() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(SINK_ID).setVersion(1).build();
        when(flowStoreServiceConnector.updateSink(eq(sink.getContent()),(eq(sink.getId())),(eq(sink.getVersion()))))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("DIED", 406));
        try {
            flowStoreProxy.updateSink(sink.getContent(), sink.getId(), sink.getVersion());
            fail("No NOT_ACCEPTABLE error was thrown by updateSink()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.NOT_ACCEPTABLE));
        }
    }

    @Test
    public void updateSink_remoteServiceReturnsHttpStatusOk_returnsSinkEntity() throws Exception {
        final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceConnector);
        final Sink sink = new SinkBuilder().setId(SINK_ID).setVersion(1).build();
        when(flowStoreServiceConnector.updateSink(eq(sink.getContent()),(eq(sink.getId())),(eq(sink.getVersion()))))
                .thenReturn(sink);

        try {
            final Sink updatedSink  = flowStoreProxy.updateSink(sink.getContent(), sink.getId(), sink.getVersion());
            assertNotNull(updatedSink);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: createSink()");
        }
    }

    /*
     * Test findAllSubmitters
     */
    @Test
    public void findAllSubmitters_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.SUBMITTERS)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllSubmitters();
            fail();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllSubmitters_remoteServiceReturnsHttpStatusOk_returnsListOfSubmitterEntity() throws Exception {
        final Submitter submitter = new SubmitterBuilder().setId(666).build();
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.SUBMITTERS)))
                .thenReturn(new MockedHttpClientResponse<List<Submitter>>(Response.Status.OK.getStatusCode(), Arrays.asList(submitter)));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        final List<Submitter> allSubmitters = flowStoreProxy.findAllSubmitters();
        assertThat(allSubmitters.size(), is(1));
        assertThat(allSubmitters.get(0).getId(), is(submitter.getId()));
    }

    /*
     * Test findAllFlowBinders
     */
    @Test
    public void findAllFlowBinders_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.FLOW_BINDERS)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllFlowBinders();
            fail();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllFlowBinders_remoteServiceReturnsHttpStatusOk_returnsListOfSubmitterEntity() throws Exception {
        final FlowBinder flowBinder = new FlowBinderBuilder().setId(666).build();
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.FLOW_BINDERS)))
                .thenReturn(new MockedHttpClientResponse<List<FlowBinder>>(Response.Status.OK.getStatusCode(), Arrays.asList(flowBinder)));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        final List<FlowBinder> allFlowBinders = flowStoreProxy.findAllFlowBinders();
        assertThat(allFlowBinders.size(), is(1));
        assertThat(allFlowBinders.get(0).getId(), is(flowBinder.getId()));
    }
}
