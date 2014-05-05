package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Created by sma on 02/05/14.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class FlowStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private static final long SINK_ID = 1;

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new FlowStoreServiceConnector(null, FLOW_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new FlowStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new FlowStoreServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(FLOW_STORE_URL));
    }

    @Test
    public void getSink_sinkRetrieved_returnsSink() throws FlowStoreServiceConnectorException {

        final Sink expectedSinkResult = new SinkBuilder().build();
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK_ID), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedSinkResult));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final Sink sinkResult = instance.getSink(SINK_ID);
        assertThat(sinkResult, is(notNullValue()));
        assertThat(sinkResult.getId(), is(expectedSinkResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK_ID), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(SINK_ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK_ID), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(SINK_ID);
    }

    private static FlowStoreServiceConnector newFlowStoreServiceConnector() {
        return new FlowStoreServiceConnector(CLIENT, FLOW_STORE_URL);
    }
}
