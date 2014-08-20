package dk.dbc.dataio.common.utils.flowstore;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
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
    HttpClient.class,})
public class FlowStoreServiceConnector_Flows_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // **************************************** create flow tests ****************************************
    @Test(expected = NullPointerException.class)
    public void createFlow_flowContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlow(null);
    }

    @Test
    public void createFlow_flowIsCreated_returnsFlow() throws FlowStoreServiceConnectorException, JsonException {
        final Flow expectedFlow = new FlowBuilder().build();
        final Flow flow = createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlow);
        assertThat(flow, is(notNullValue()));
        assertThat(flow.getId(), is(expectedFlow.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlow_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    // Helper method
    private Flow createFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOWS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createFlow(flowContent);
    }

    // **************************************** get flow tests ****************************************
    @Test
    public void getFlow_flowRetrieved_returnsFlow() throws FlowStoreServiceConnectorException {
        final Flow expectedFlowResult = new FlowBuilder().build();
        final Flow flowResult
                = getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowResult, ID);
        assertThat(flowResult, is(notNullValue()));
        assertThat(flowResult.getId(), is(expectedFlowResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getFlow_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null, ID);
    }

    // Helper method
    private Flow getFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getFlow(id);
    }

    // ***************************************** update flow tests *****************************************
    @Test
    public void updateFlowComponentsInFlowToLatestVersion_flowIsUpdated_returnsFlow() throws FlowStoreServiceConnectorException, JsonException {
        final Flow flowToUpdate = new FlowBuilder().build();
        Flow updatedFlow = updateFlow_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(),
                flowToUpdate,
                flowToUpdate.getId(),
                flowToUpdate.getVersion());
        assertThat(updatedFlow, is(notNullValue()));
        assertThat(updatedFlow.getContent(), is(notNullValue()));
        assertThat(updatedFlow.getId(), is(flowToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlowComponentsInFlowToLatestVersion_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponentsInFlowToLatestVersion_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    // Helper method
    private Flow updateFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");
        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.FLOW_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doPostWithJson(CLIENT, headers, "", FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateFlowComponentsInFlowToLatestVersion(id, version);
    }

    // ************************************* find all flows tests *************************************
    @Test
    public void findAllFlows_flowsRetrieved_returnsFlows() throws FlowStoreServiceConnectorException {
        final FlowContent flowContentA = new FlowContentBuilder().setName("a").build();
        final FlowContent flowContentB = new FlowContentBuilder().setName("b").build();
        final Flow expectedFlowResultA = new FlowBuilder().setContent(flowContentA).build();
        final Flow expectedFlowResultB = new FlowBuilder().setContent(flowContentB).build();
        List<Flow> expectedFlowResultList = Arrays.asList(expectedFlowResultA, expectedFlowResultB);
        final List<Flow> flowResultList = findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowResultList);

        assertNotNull(flowResultList);
        assertFalse(flowResultList.isEmpty());
        assertThat(flowResultList.size(), is(2));
        assertThat(flowResultList.get(0), is(notNullValue()));
        assertThat(flowResultList.get(1), is(notNullValue()));
    }

    @Test
    public void findAllFlows_noResults() throws FlowStoreServiceConnectorException {
        List<Flow> flowResultList = findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<Flow>());
        assertThat(flowResultList, is(notNullValue()));
        assertThat(flowResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlows_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlows_noListReturned() throws FlowStoreServiceConnectorException {
        findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlows_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<Flow> findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllFlows();
    }
}
