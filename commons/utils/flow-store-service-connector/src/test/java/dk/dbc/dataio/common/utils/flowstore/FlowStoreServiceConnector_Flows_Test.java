package dk.dbc.dataio.common.utils.flowstore;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.newFlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

public class FlowStoreServiceConnector_Flows_Test extends FlowStoreServiceConnectorTestSuper {

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
    public void updateFlowComponentsInFlowToLatestVersion_flowIsUpdated_returnsFlow() throws FlowStoreServiceConnectorException, JsonException {
        final Flow flowToUpdate = new FlowBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, "", FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowToUpdate));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        Flow updatedFlow = instance.updateFlowComponentsInFlowToLatestVersion(flowToUpdate.getId(), flowToUpdate.getVersion());

        assertThat(updatedFlow, is(notNullValue()));
        assertThat(updatedFlow.getContent(), is(notNullValue()));
        assertThat(updatedFlow.getId(), is (flowToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlowComponentsInFlowToLatestVersion_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, "", FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlowComponentsInFlowToLatestVersion(ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponentsInFlowToLatestVersion_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException{
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.doPostWithJson(CLIENT, headers, "", FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.CONFLICT.getStatusCode(), ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateFlowComponentsInFlowToLatestVersion(ID, VERSION);
    }
}
