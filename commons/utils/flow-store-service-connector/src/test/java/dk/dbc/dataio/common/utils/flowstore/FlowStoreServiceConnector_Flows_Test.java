package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlowStoreServiceConnector_Flows_Test {
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector =
            new FlowStoreServiceConnector(failSafeHttpClient, FLOW_STORE_URL);

    // **************************************** create flow tests ****************************************
    @Test
    public void createFlow_flowIsCreated_returnsFlow() throws FlowStoreServiceConnectorException, JSONBException {
        final Flow expectedFlow = new FlowBuilder().build();
        final Flow flow = createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlow);
        assertThat(flow, is(expectedFlow));
    }

    @Test
    public void createFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void createFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private Flow createFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.FLOWS)
                .withJsonData(flowContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.createFlow(flowContent);
    }

    // **************************************** get flow tests ****************************************
    @Test
    public void getFlow_flowRetrieved_returnsFlow() throws FlowStoreServiceConnectorException {
        final Flow expectedFlow = new FlowBuilder().build();
        final Flow flow = getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlow, expectedFlow.getId());
        assertThat(flow, is(expectedFlow));
    }

    @Test
    public void getFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, 1),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private Flow getFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.getFlow(id);
    }

    // ***************************************** update flow tests *****************************************
    @Test
    public void refreshFlowComponents_componentsInFlowAreUpdated_returnsFlow() throws FlowStoreServiceConnectorException, JSONBException {
        final Flow flowToUpdate = new FlowBuilder().build();
        final Flow updatedFlow = refreshFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), flowToUpdate, flowToUpdate.getId(), flowToUpdate.getVersion());
        assertThat(updatedFlow, is(flowToUpdate));
    }

    @Test
    public void refreshFlowComponents_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> refreshFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private Flow refreshFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withQueryParameter(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH, true)
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                .withJsonData("");

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.refreshFlowComponents(id, version);
    }

    @Test
    public void updateFlow_flowIsUpdated_returnsFlow() throws FlowStoreServiceConnectorException, JSONBException {
        final Flow flowToUpdate = new FlowBuilder().build();
        final Flow updatedFlow = updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), flowToUpdate, 1, 1);
        assertThat(updatedFlow, is(flowToUpdate));
    }

    @Test
    public void updateFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private Flow updateFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withQueryParameter(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH, false)
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                .withJsonData(flowContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateFlow(flowContent, id, version);
    }

    // ************************************* find all flows tests *************************************
    @Test
    public void findAllFlows_flowsRetrieved_returnsFlows() throws FlowStoreServiceConnectorException {
        final FlowContent flowContentA = new FlowContentBuilder().setName("a").build();
        final FlowContent flowContentB = new FlowContentBuilder().setName("b").build();
        final Flow expectedFlowResultA = new FlowBuilder().setContent(flowContentA).build();
        final Flow expectedFlowResultB = new FlowBuilder().setContent(flowContentB).build();
        final List<Flow> expected = Arrays.asList(expectedFlowResultA, expectedFlowResultB);
        final List<FlowView> flows = findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expected);
        assertThat(flows, is(expected));
    }

    @Test
    public void findAllFlows_noResults() throws FlowStoreServiceConnectorException {
        assertThat(findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), Collections.emptyList()),
                is(Collections.emptyList()));
    }

    @Test
    public void findAllFlows_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findAllFlows_nullListReturned() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private List<FlowView> findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.FLOWS);

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.findAllFlows();
    }

    // **************************************** delete flow tests ****************************************
    @Test
    public void deleteFlow_flowIsDeleted() throws FlowStoreServiceConnectorException {
        deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), 1, 1);
    }

    @Test
    public void deleteFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private void deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id, long version) throws FlowStoreServiceConnectorException {
        final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.FLOW)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        final HttpDelete httpDelete = new HttpDelete(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(pathBuilder.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version));

        when(failSafeHttpClient.execute(httpDelete))
                .thenReturn(new MockedResponse<>(statusCode, null));

        flowStoreServiceConnector.deleteFlow(id, version);
    }
}
