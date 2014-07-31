package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class FlowStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private static final long ID = 1;
    private static final long VERSION = 1;

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SINK_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");

        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.FLOW_COMPONENT_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");

        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.FLOW_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
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

    // ************************************** create submitter tests **************************************

    @Test(expected = NullPointerException.class)
    public void createSubmitter_submitterContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSubmitter(null);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, submitterContent, FLOW_STORE_URL, FlowStoreServiceConstants.SUBMITTERS))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSubmitter(submitterContent);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSubmitter_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, submitterContent, FLOW_STORE_URL, FlowStoreServiceConstants.SUBMITTERS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSubmitter(submitterContent);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createSubmitter_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, submitterContent, FLOW_STORE_URL, FlowStoreServiceConstants.SUBMITTERS))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_ACCEPTABLE.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSubmitter(submitterContent);
    }

    @Test
    public void createSubmitter_submitterIsCreated_returnsSubmitter() throws FlowStoreServiceConnectorException, JsonException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        final Submitter expectedSubmitter = new SubmitterBuilder().build();

        when(HttpClient.doPostWithJson(CLIENT, submitterContent, FLOW_STORE_URL, FlowStoreServiceConstants.SUBMITTERS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), expectedSubmitter));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final Submitter submitter = instance.createSubmitter(submitterContent);
        assertThat(submitter, is(notNullValue()));
        assertThat(submitter.getId(), is(expectedSubmitter.getId()));
    }

    // *********************************** find all submitters tests ***********************************
    @Test
    public void findAllSubmitters_submittersRetrieved_returnsSubmitters() throws FlowStoreServiceConnectorException {

        final SubmitterContent submitterContentA = new SubmitterContentBuilder().setName("a").setNumber(1L).setDescription("submitterA").build();
        final SubmitterContent submitterContentB = new SubmitterContentBuilder().setName("B").setNumber(2L).setDescription("submitterB").build();
        final Submitter expectedSubmitterResultA = new SubmitterBuilder().setContent(submitterContentA).build();
        final Submitter expectedSubmitterResultB = new SubmitterBuilder().setContent(submitterContentB).build();



        List<Submitter> expectedSubmitterResultList = new ArrayList<>();
        expectedSubmitterResultList.add(expectedSubmitterResultA);
        expectedSubmitterResultList.add(expectedSubmitterResultB);

        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedSubmitterResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final List<Submitter> submitterResultList = instance.findAllSubmitters();

        assertNotNull(submitterResultList);
        assertFalse(submitterResultList.isEmpty());
        assertThat(submitterResultList.size(), is(2));

        for (Submitter submitter : submitterResultList){
            assertThat(submitter, is(notNullValue()));
        }
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSubmitters_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSubmitters();
    }

    @Test
    public void findAllSubmitters_noResults() throws FlowStoreServiceConnectorException {
        List<Submitter> submitterResultList = new ArrayList<>();
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), submitterResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        submitterResultList = instance.findAllSubmitters();
        assertThat(submitterResultList, is(notNullValue()));
        assertThat(submitterResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSubmitters_noListReturned() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSubmitters();
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllSubmitters_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSubmitters();
    }

    // *************************************** get flow component tests **************************************
    @Test
    public void getFlowComponent_flowComponentRetrieved_returnsFlowComponent() throws FlowStoreServiceConnectorException {

        final FlowComponent expectedFlowComponentResult = new FlowComponentBuilder().build();
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedFlowComponentResult));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final FlowComponent flowComponentResult = instance.getFlowComponent(ID);
        assertThat(flowComponentResult, is(notNullValue()));
        assertThat(flowComponentResult.getId(), is(expectedFlowComponentResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlowComponent(ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlowComponent(ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getFlowComponent_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlowComponent(ID);
    }

    // ************************************* create flow component tests *************************************

    @Test(expected = NullPointerException.class)
    public void createFlowComponent_flowComponentContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowComponent(null);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowComponentContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_COMPONENTS))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowComponent(flowComponentContent);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowComponentContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_COMPONENTS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowComponent(flowComponentContent);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlowComponent_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowComponentContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_COMPONENTS))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_ACCEPTABLE.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowComponent(flowComponentContent);
    }

    @Test
    public void createFlowComponent_flowComponentIsCreated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JsonException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final FlowComponent expectedFlowComponent = new FlowComponentBuilder().build();

        when(HttpClient.doPostWithJson(CLIENT, flowComponentContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_COMPONENTS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), expectedFlowComponent));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final FlowComponent flowComponent = instance.createFlowComponent(flowComponentContent);
        assertThat(flowComponent, is(notNullValue()));
        assertThat(flowComponent.getId(), is(expectedFlowComponent.getId()));
    }

    // *********************************** find all flow components tests ***********************************
    @Test
    public void findAllFlowComponents_flowComponentsRetrieved_returnsflowComponents() throws FlowStoreServiceConnectorException {

        final FlowComponentContent flowComponentContentA = new FlowComponentContentBuilder().setName("a").build();
        final FlowComponentContent flowComponentContentB = new FlowComponentContentBuilder().setName("b").build();
        final FlowComponent expectedFlowComponentResultA = new FlowComponentBuilder().setContent(flowComponentContentA).build();
        final FlowComponent expectedFlowComponentResultB = new FlowComponentBuilder().setContent(flowComponentContentB).build();

        List<FlowComponent> expectedFlowComponentResultList = new ArrayList<>();
        expectedFlowComponentResultList.add(expectedFlowComponentResultA);
        expectedFlowComponentResultList.add(expectedFlowComponentResultB);

        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedFlowComponentResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final List<FlowComponent> flowComponentResultList = instance.findAllFlowComponents();

        assertNotNull(flowComponentResultList);
        assertFalse(flowComponentResultList.isEmpty());
        assertThat(flowComponentResultList.size(), is(2));

        for (FlowComponent flowComponent : flowComponentResultList){
            assertThat(flowComponent, is(notNullValue()));
        }
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowComponents_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllFlowComponents();
    }

    @Test
    public void findAllFlowComponents_noResults() throws FlowStoreServiceConnectorException {
        List<FlowComponent> flowComponentResultList = new ArrayList<>();
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowComponentResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        flowComponentResultList = instance.findAllFlowComponents();
        assertThat(flowComponentResultList, is(notNullValue()));
        assertThat(flowComponentResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowComponents_noListReturned() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllFlowComponents();
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlowComponents_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllFlowComponents();
    }

    // ************************************** update flow component tests **************************************
    @Test
    public void updateFlowComponent_flowComponentIsUpdated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JsonException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowComponentContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowComponentToUpdate));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        FlowComponent updatedFlowComponent = instance.updateFlowComponent(flowComponentContent, flowComponentToUpdate.getId(), flowComponentToUpdate.getVersion());

        assertThat(updatedFlowComponent, is(notNullValue()));
        assertThat(updatedFlowComponent.getContent(), is(notNullValue()));
        assertThat(updatedFlowComponent.getId(), is (flowComponentToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowComponentContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlowComponent(flowComponentContent, ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponent_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowComponentContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_ACCEPTABLE.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlowComponent(flowComponentContent, ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponent_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException{
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowComponentContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.CONFLICT.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlowComponent(flowComponentContent, ID, VERSION);
    }

    // **************************************** create flow tests ****************************************

    @Test(expected = NullPointerException.class)
    public void createFlow_flowContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlow(null);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOWS))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlow(flowContent);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOWS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlow(flowContent);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlow_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        final FlowContent flowContent = new FlowContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOWS))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_ACCEPTABLE.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlow(flowContent);
    }

    @Test
    public void createFlow_flowIsCreated_returnsFlow() throws FlowStoreServiceConnectorException, JsonException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        final Flow expectedFlow = new FlowBuilder().build();

        when(HttpClient.doPostWithJson(CLIENT, flowContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOWS))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), expectedFlow));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final Flow flow = instance.createFlow(flowContent);
        assertThat(flow, is(notNullValue()));
        assertThat(flow.getId(), is(expectedFlow.getId()));
    }

    // **************************************** get flow tests ****************************************
    @Test
    public void getFlow_flowRetrieved_returnsFlow() throws FlowStoreServiceConnectorException {

        final Flow expectedFlowResult = new FlowBuilder().build();
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedFlowResult));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final Flow flowResult = instance.getFlow(ID);
        assertThat(flowResult, is(notNullValue()));
        assertThat(flowResult.getId(), is(expectedFlowResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlow(ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlow(ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getFlow_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlow(ID);
    }

    // ************************************* find all flows tests *************************************
    @Test
    public void findAllFlows_flowsRetrieved_returnsFlows() throws FlowStoreServiceConnectorException {

        final FlowContent flowContentA = new FlowContentBuilder().setName("a").build();
        final FlowContent flowContentB = new FlowContentBuilder().setName("b").build();

        final Flow expectedFlowResultA = new FlowBuilder().setContent(flowContentA).build();
        final Flow expectedFlowResultB = new FlowBuilder().setContent(flowContentB).build();

        List<Flow> expectedFlowResultList = new ArrayList<>();
        expectedFlowResultList.add(expectedFlowResultA);
        expectedFlowResultList.add(expectedFlowResultB);

        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedFlowResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final List<Flow> flowResultList = instance.findAllFlows();

        assertNotNull(flowResultList);
        assertFalse(flowResultList.isEmpty());
        assertThat(flowResultList.size(), is(2));

        for (Flow flow : flowResultList){
            assertThat(flow, is(notNullValue()));
        }
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlows_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllFlows();
    }

    @Test
    public void findAllFlows_noResults() throws FlowStoreServiceConnectorException {
        List<Flow> flowResultList = new ArrayList<>();
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowResultList));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        flowResultList = instance.findAllFlows();
        assertThat(flowResultList, is(notNullValue()));
        assertThat(flowResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlows_noListReturned() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllFlows();
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlows_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllFlows();
    }

    // ***************************************** update flow tests *****************************************
    @Test
    public void updateFlow_flowIsUpdated_returnsFlow() throws FlowStoreServiceConnectorException, JsonException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        final Flow flowToUpdate = new FlowBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowToUpdate));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        Flow updatedFlow = instance.updateFlow(flowContent, flowToUpdate.getId(), flowToUpdate.getVersion());

        assertThat(updatedFlow, is(notNullValue()));
        assertThat(updatedFlow.getContent(), is(notNullValue()));
        assertThat(updatedFlow.getId(), is (flowToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlow(flowContent, ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlow_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        final FlowContent flowContent = new FlowContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_ACCEPTABLE.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlow(flowContent, ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlow_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException{
        final FlowContent flowContent = new FlowContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, flowContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.CONFLICT.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlow(flowContent, ID, VERSION);
    }

    private static FlowStoreServiceConnector newFlowStoreServiceConnector() {
        return new FlowStoreServiceConnector(CLIENT, FLOW_STORE_URL);
    }
}
