package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import java.util.Arrays;
import java.util.List;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,
    ServiceUtil.class
})
public class FlowStoreProxyImplTest {
    private final String flowStoreServiceUrl = "http://dataio/flow-service";
    private final Client client = mock(Client.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn(flowStoreServiceUrl);
        when(HttpClient.newClient()).thenReturn(client);
    }

    @Test(expected = ProxyException.class)
    public void findAllSinks_flowStoreServiceEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllSinks();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.SERVICE_NOT_FOUND));
            throw e;
        }
    }

    @Test(expected = ProxyException.class)
    public void findAllSinks_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.SINKS)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllSinks();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
            throw e;
        }
    }

    @Test
    public void findAllSinks_remoteServiceReturnsHttpStatusOk_returnsListOfSinkEntity() throws Exception {
        final Sink sink = new SinkBuilder().setId(666).build();
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.SINKS)))
                .thenReturn(new MockedHttpClientResponse<List<Sink>>(Response.Status.OK.getStatusCode(), Arrays.asList(sink)));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        final List<Sink> allSinks = flowStoreProxy.findAllSinks();
        assertThat(allSinks.size(), is(1));
        assertThat(allSinks.get(0).getId(), is(sink.getId()));
    }

    @Test(expected = ProxyException.class)
    public void findAllSubmitters_flowStoreServiceEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllSubmitters();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.SERVICE_NOT_FOUND));
            throw e;
        }
    }

    @Test(expected = ProxyException.class)
    public void findAllSubmitters_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceConstants.SUBMITTERS)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllSubmitters();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
            throw e;
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
}
