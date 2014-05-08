package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    @Test(expected = NullPointerException.class)
    public void createSInk_sinkContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSink(null);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final SinkContent sinkContent = new SinkContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, sinkContent, FLOW_STORE_URL, FlowStoreServiceConstants.SINKS))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSink(sinkContent);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        final SinkContent sinkContent = new SinkContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, sinkContent, FLOW_STORE_URL, FlowStoreServiceConstants.SINKS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSink(sinkContent);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createSink_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        final SinkContent sinkContent = new SinkContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, sinkContent, FLOW_STORE_URL, FlowStoreServiceConstants.SINKS))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_ACCEPTABLE.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSink(sinkContent);
    }

    @Test
    public void createSink_sinkIsCreated_returnsSink() throws FlowStoreServiceConnectorException, JsonException {
        final SinkContent sinkContent = new SinkContentBuilder().build();
        final Sink expectedSink = new SinkBuilder().build();

        when(HttpClient.doPostWithJson(CLIENT, sinkContent, FLOW_STORE_URL, FlowStoreServiceConstants.SINKS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), expectedSink));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final Sink sink = instance.createSink(sinkContent);
        assertThat(sink, is(notNullValue()));
        assertThat(sink.getId(), is(expectedSink.getId()));
    }

    @Test
    public void getSink_sinkRetrieved_returnsSink() throws FlowStoreServiceConnectorException {

        final Sink expectedSinkResult = new SinkBuilder().build();
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
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
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(SINK_ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(SINK_ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getSink_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(SINK_ID);
    }

    @Test
    public void findAllSinks_sinksRetrieved_returnsSinks() throws FlowStoreServiceConnectorException {

        final SinkContent sinkContentA = new SinkContentBuilder().setName("a").setResource("resource").build();
        final SinkContent sinkContentB = new SinkContentBuilder().setName("b").setResource("resource").build();
        final Sink expectedSinkResultA = new SinkBuilder().setContent(sinkContentA).build();
        final Sink expectedSinkResultB = new SinkBuilder().setContent(sinkContentB).build();

        List<Sink> expectedSinkResultList = new ArrayList<>();
        expectedSinkResultList.add(expectedSinkResultA);
        expectedSinkResultList.add(expectedSinkResultB);

        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedSinkResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final List<Sink> sinkResultList = instance.findAllSinks();

        assertNotNull(sinkResultList);
        assertFalse(sinkResultList.isEmpty());
        assertThat(sinkResultList.size(), is(2));

        for (Sink sink : sinkResultList){
            assertThat(sink, is(notNullValue()));
        }
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSinks_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSinks();
    }

    @Test
    public void findAllSinks_noResults() throws FlowStoreServiceConnectorException {
        List<Sink> sinkResultList = new ArrayList<>();
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), sinkResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        sinkResultList = instance.findAllSinks();
        assertThat(sinkResultList, is(notNullValue()));
        assertThat(sinkResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllSinks_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSinks();
    }

    private static FlowStoreServiceConnector newFlowStoreServiceConnector() {
        return new FlowStoreServiceConnector(CLIENT, FLOW_STORE_URL);
    }
}
