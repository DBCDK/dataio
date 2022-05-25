package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.rest.FlowBinderResolveQuery;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlowStoreServiceConnector_FlowBinders_Test {
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector =
            new FlowStoreServiceConnector(failSafeHttpClient, FLOW_STORE_URL);

    // **************************************** create flow binder tests ****************************************
    @Test
    public void createFlowBinder_flowBinderContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> flowStoreServiceConnector.createFlowBinder(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void createFlowBinder_flowBinderIsCreated_returnsFlowBinder() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowBinder expectedFlowBinder = new FlowBinderBuilder().build();
        final FlowBinder flowBinder = createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlowBinder);
        assertThat(flowBinder, is(notNullValue()));
        assertThat(flowBinder.getId(), is(expectedFlowBinder.getId()));
    }

    @Test
    public void createFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    @Test
    public void createFlowBinder_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    @Test
    public void createFlowBinder_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void createFlowBinder_responseWithPreconditionFailed_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.PRECONDITION_FAILED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private FlowBinder createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().build();
        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.FLOW_BINDERS)
                .withJsonData(flowBinderContent);
        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        return flowStoreServiceConnector.createFlowBinder(flowBinderContent);
    }

    // *************************************** find all flow binders tests **************************************
    @Test
    public void findAllFlowBinders_flowBindersRetrieved_returnsFlowBinder() throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContentA = new FlowBinderContentBuilder().setName("a").build();
        final FlowBinderContent flowBinderContentB = new FlowBinderContentBuilder().setName("b").build();
        final FlowBinder expectedFlowBinderResultA = new FlowBinderBuilder().setContent(flowBinderContentA).build();
        final FlowBinder expectedFlowBinderResultB = new FlowBinderBuilder().setContent(flowBinderContentB).build();
        final List<FlowBinder> expectedFlowBinderResultList = Arrays.asList(expectedFlowBinderResultA, expectedFlowBinderResultB);
        final List<FlowBinder> flowBinderResultList = findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowBinderResultList);

        assertThat(flowBinderResultList, is(notNullValue()));
        assertThat(flowBinderResultList.isEmpty(), is(false));
        assertThat(flowBinderResultList.size(), is(2));
        assertThat(flowBinderResultList.get(0), is(notNullValue()));
        assertThat(flowBinderResultList.get(1), is(notNullValue()));
    }

    @Test
    public void findAllFlowBinders_noResults() throws FlowStoreServiceConnectorException {
        final List<FlowBinder> flowBinderResultList = findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), new ArrayList<FlowBinder>());
        assertThat(flowBinderResultList, is(notNullValue()));
        assertThat(flowBinderResultList.size(), is(0));
    }

    @Test
    public void findAllFlowBinders_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    @Test
    public void findAllFlowBinders_noListReturned() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    @Test
    public void findAllFlowBinders_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private List<FlowBinder> findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.FLOW_BINDERS);
        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        return flowStoreServiceConnector.findAllFlowBinders();
    }

    // *************************************** get flow binder tests **************************************
    @Test
    public void getFlowBinder_flowBinderExist_flowBinderReturned() throws FlowStoreServiceConnectorException {
        final FlowBinder expectedFlowBinder = new FlowBinderBuilder().build();
        final FlowBinder flowBinder = getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(
                expectedFlowBinder.getId(), Response.Status.OK.getStatusCode(), expectedFlowBinder);
        assertThat(flowBinder, is(expectedFlowBinder));
    }

    @Test
    public void getFlowBinder_nullFlowBinder_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(42L, Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    @Test
    public void getFlowBinder_internalServerError_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(42L, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private FlowBinder getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(long flowBinderId, int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, flowBinderId);

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        return flowStoreServiceConnector.getFlowBinder(flowBinderId);
    }

    // **************************************** update flow binder tests ****************************************
    @Test
    public void updateFlowBinder_flowBinderIsUpdated_returnsFlowBinder() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowBinder flowBinderToUpdate = new FlowBinderBuilder().build();
        final FlowBinder updatedFlowBinder = updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), flowBinderToUpdate, flowBinderToUpdate.getId(), flowBinderToUpdate.getVersion());
        assertThat(updatedFlowBinder, is(flowBinderToUpdate));
    }

    @Test
    public void updateFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private FlowBinder updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().build();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                .withJsonData(flowBinderContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateFlowBinder(flowBinderContent, id, version);
    }

    // **************************************** delete flow binder tests ****************************************
    @Test
    public void deleteFlowBinder_flowBinderIsDeleted() throws FlowStoreServiceConnectorException {
        deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), 1, 1);
    }

    @Test
    public void deleteFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private void deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id, long version) throws FlowStoreServiceConnectorException {
        final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        final HttpDelete httpDelete = new HttpDelete(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(pathBuilder.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version));

        when(failSafeHttpClient.execute(httpDelete))
                .thenReturn(new MockedResponse<>(statusCode, null));

        flowStoreServiceConnector.deleteFlowBinder(id, version);
    }

    // **************************************** get flow binder by search index tests ****************************************
    @Test
    public void getFlowBinder_flowBinderRetrieved_returnsFlowBinder() throws FlowStoreServiceConnectorException {
        final FlowBinder expectedFlowBinderResult = new FlowBinderBuilder().build();
        final FlowBinder flowBinderResult = getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowBinderResult);
        assertThat(flowBinderResult, is(expectedFlowBinderResult));
    }

    @Test
    public void getFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private FlowBinder getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(new String[]{FlowStoreServiceConstants.FLOW_BINDER_RESOLVE})
                .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_PACKAGING, "packaging")
                .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_FORMAT, "format")
                .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_CHARSET, "charset")
                .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_SUBMITTER, Long.toString(1))
                .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_DESTINATION, "destination");

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.getFlowBinder("packaging", "format", "charset", 1, "destination");
    }
}
