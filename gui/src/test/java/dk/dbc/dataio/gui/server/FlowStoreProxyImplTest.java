package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
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

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn(flowStoreServiceUrl);
        when(HttpClient.newClient()).thenReturn(client);
    }

    @Test(expected = FlowStoreProxyException.class)
    public void findAllSinks_flowStoreServiceEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllSinks();
        } catch (FlowStoreProxyException e) {
            assertThat(e.getErrorCode(), is(FlowStoreProxyError.SERVICE_NOT_FOUND));
            throw e;
        }
    }

    @Test(expected = FlowStoreProxyException.class)
    public void findAllSinks_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceEntryPoint.SINKS)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        try {
            flowStoreProxy.findAllSinks();
        } catch (FlowStoreProxyException e) {
            assertThat(e.getErrorCode(), is(FlowStoreProxyError.INTERNAL_SERVER_ERROR));
            throw e;
        }
    }

    @Test
    public void findAllSinks_remoteServiceReturnsHttpStatusOk_returnsListOfSinkEntity() throws Exception {
        final Sink sink = new SinkBuilder().setId(666).build();
        when(HttpClient.doGet(any(Client.class), eq(flowStoreServiceUrl), eq(FlowStoreServiceEntryPoint.SINKS)))
                .thenReturn(new MockedHttpClientResponse<List<Sink>>(Response.Status.OK.getStatusCode(), Arrays.asList(sink)));

        final FlowStoreProxyImpl flowStoreProxy = new FlowStoreProxyImpl();
        final List<Sink> allSinks = flowStoreProxy.findAllSinks();
        assertThat(allSinks.size(), is(1));
        assertThat(allSinks.get(0).getId(), is(sink.getId()));
    }
}
