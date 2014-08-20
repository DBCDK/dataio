package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

public class FlowStoreServiceConnector_Sinks_Test extends FlowStoreServiceConnectorTestSuper {

    // **************************************** create sink tests ****************************************

    @Test(expected = NullPointerException.class)
    public void createSink_sinkContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
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

    // **************************************** get sink tests ****************************************
    @Test
    public void getSink_sinkRetrieved_returnsSink() throws FlowStoreServiceConnectorException {

        final Sink expectedSinkResult = new SinkBuilder().build();
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedSinkResult));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final Sink sinkResult = instance.getSink(ID);
        assertThat(sinkResult, is(notNullValue()));
        assertThat(sinkResult.getId(), is(expectedSinkResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getSink_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getSink(ID);
    }

    // ************************************* find all sinks tests *************************************
    @Test
    public void findAllSinks_sinksRetrieved_returnsSinks() throws FlowStoreServiceConnectorException {

        final SinkContent sinkContentA = new SinkContentBuilder().setName("a").setResource("resource").build();
        final SinkContent sinkContentB = new SinkContentBuilder().setName("b").setResource("resource").build();
        final Sink expectedSinkResultA = new SinkBuilder().setContent(sinkContentA).build();
        final Sink expectedSinkResultB = new SinkBuilder().setContent(sinkContentB).build();

        List<Sink> expectedSinkResultList = new ArrayList<>();
        expectedSinkResultList.add(expectedSinkResultA);
        expectedSinkResultList.add(expectedSinkResultB);

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
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSinks();
    }

    @Test
    public void findAllSinks_noResults() throws FlowStoreServiceConnectorException {
        List<Sink> sinkResultList = new ArrayList<>();
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), sinkResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        sinkResultList = instance.findAllSinks();
        assertThat(sinkResultList, is(notNullValue()));
        assertThat(sinkResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSinks_noListReturned() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSinks();
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllSinks_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSinks();
    }

    // **************************************** update sink tests ****************************************
    @Test
    public void updateSink_sinkIsUpdated_returnsSink() throws FlowStoreServiceConnectorException, JsonException {
        final SinkContent sinkContent = new SinkContentBuilder().build();
        final Sink sinkToUpdate = new SinkBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, sinkContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), sinkToUpdate));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        Sink updatedSink = instance.updateSink(sinkContent, sinkToUpdate.getId(), sinkToUpdate.getVersion());

        assertThat(updatedSink, is(notNullValue()));
        assertThat(updatedSink.getContent(), is(notNullValue()));
        assertThat(updatedSink.getId(), is (sinkToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final SinkContent sinkContent = new SinkContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, sinkContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateSink(sinkContent, ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSink_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        final SinkContent sinkContent = new SinkContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, sinkContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_ACCEPTABLE.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateSink(sinkContent, ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSink_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException{
        final SinkContent sinkContent = new SinkContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, sinkContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.CONFLICT.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateSink(sinkContent, ID, VERSION);
    }
}
