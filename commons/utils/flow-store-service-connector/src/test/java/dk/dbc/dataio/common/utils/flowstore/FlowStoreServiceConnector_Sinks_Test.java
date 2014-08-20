package dk.dbc.dataio.common.utils.flowstore;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class FlowStoreServiceConnector_Sinks_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // **************************************** create sink tests ****************************************
    @Test(expected = NullPointerException.class)
    public void createSink_sinkContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSink(null);
    }

    @Test
    public void createSink_sinkIsCreated_returnsSink() throws FlowStoreServiceConnectorException, JsonException {
        final Sink expectedSink = new SinkBuilder().build();
        final Sink sink = createSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedSink);

        assertThat(sink, is(notNullValue()));
        assertThat(sink.getId(), is(expectedSink.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createSink_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    // Helper method
    private Sink createSink_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final SinkContent sinkContent = new SinkContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, sinkContent, FLOW_STORE_URL, FlowStoreServiceConstants.SINKS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createSink(sinkContent);
    }

    // **************************************** get sink tests ****************************************
    @Test
    public void getSink_sinkRetrieved_returnsSink() throws FlowStoreServiceConnectorException {
        final Sink expectedSinkResult = new SinkBuilder().build();
        final Sink sinkResult = getSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedSinkResult);
        assertThat(sinkResult, is(notNullValue()));
        assertThat(sinkResult.getId(), is(expectedSinkResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getSink_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    private Sink getSink_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getSink(ID);
    }

    // **************************************** update sink tests ****************************************
    @Test
    public void updateSink_sinkIsUpdated_returnsSink() throws FlowStoreServiceConnectorException, JsonException {
        final Sink sinkToUpdate = new SinkBuilder().build();

        Sink updatedSink = updateSink_mockedHttpWithSpecifiedReturnErrorCode(
                        Response.Status.OK.getStatusCode(),
                        sinkToUpdate,
                        sinkToUpdate.getId(),
                        sinkToUpdate.getVersion());

        assertThat(updatedSink, is(notNullValue()));
        assertThat(updatedSink.getContent(), is(notNullValue()));
        assertThat(updatedSink.getId(), is (sinkToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSink_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        updateSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSink_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException{
        updateSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    // Helper method
    private Sink updateSink_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final SinkContent sinkContent = new SinkContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doPostWithJson(CLIENT, headers, sinkContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateSink(sinkContent, id, version);
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

        final List<Sink> sinkResultList = findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedSinkResultList);

        assertNotNull(sinkResultList);
        assertFalse(sinkResultList.isEmpty());
        assertThat(sinkResultList.size(), is(2));

        for (Sink sink : sinkResultList){
            assertThat(sink, is(notNullValue()));
        }
    }

    @Test
    public void findAllSinks_noResults() throws FlowStoreServiceConnectorException {
        List<Sink> sinkResultList = findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<Sink>());
        assertThat(sinkResultList, is(notNullValue()));
        assertThat(sinkResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSinks_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSinks_noListReturned() throws FlowStoreServiceConnectorException {
        findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllSinks_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<Sink> findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllSinks();
    }
}
