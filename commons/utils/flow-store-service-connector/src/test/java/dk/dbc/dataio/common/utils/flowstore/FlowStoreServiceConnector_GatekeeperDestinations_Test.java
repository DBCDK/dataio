package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
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

public class FlowStoreServiceConnector_GatekeeperDestinations_Test {
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector =
            new FlowStoreServiceConnector(failSafeHttpClient, FLOW_STORE_URL);

    // **************************************************** create gatekeeper destination tests *****************************************************************

    @Test
    public void createGatekeeperDestination_gatekeeperDestinationCreated_returnsGatekeeperDestination() throws FlowStoreServiceConnectorException {
        final GatekeeperDestination expectedGatekeeperDestination = new GatekeeperDestinationBuilder().build();
        final GatekeeperDestination gatekeeperDestination = createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.CREATED.getStatusCode(), expectedGatekeeperDestination);
        assertThat(gatekeeperDestination, is(expectedGatekeeperDestination));
    }

    @Test
    public void createGatekeeperDestination_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void createGatekeeperDestination_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private GatekeeperDestination createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().build();

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS)
                .withJsonData(gatekeeperDestination);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestination);
    }

    // ************************************************** find all gatekeeper destinations tests ****************************************************************

    @Test
    public void findAllGatekeeperDestinations_gatekeeperDestinationsRetrieved_returnsListOfGatekeeperDestinations() throws FlowStoreServiceConnectorException {
        final GatekeeperDestination gatekeeperDestinationA = new GatekeeperDestinationBuilder().setSubmitterNumber("1234").build();
        final GatekeeperDestination gatekeeperDestinationB = new GatekeeperDestinationBuilder().setSubmitterNumber("2345").build();
        final List<GatekeeperDestination> expected = Arrays.asList(gatekeeperDestinationA, gatekeeperDestinationB);

        // Subject under test
        final List<GatekeeperDestination> result = findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(
                Response.Status.OK.getStatusCode(), expected);

        // Verification
        assertThat(result, is(expected));
    }

    @Test
    public void findAllGatekeeperDestinations_noResults_returnsEmptyList() throws FlowStoreServiceConnectorException {
        // Subject under test
        final List<GatekeeperDestination> result = findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(
                Response.Status.OK.getStatusCode(), Collections.emptyList());

        // Verification
        assertThat(result, is(Collections.emptyList()));
    }

    @Test
    public void findAllGatekeeperDestinations_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findAllGatekeeperDestinations_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private List<GatekeeperDestination> findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS);

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.findAllGatekeeperDestinations();
    }

    // ************************************************** delete gatekeeper destination tests *****************************************************************

    @Test
    public void deleteGatekeeperDestination_gatekeeperDestinationIsDeleted() throws FlowStoreServiceConnectorException {
        deleteGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), 1);
    }

    @Test
    public void deleteGatekeeperDestination_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> deleteGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private void deleteGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.GATEKEEPER_DESTINATION)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        final HttpDelete httpDelete = new HttpDelete(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(pathBuilder.build());

        when(failSafeHttpClient.execute(httpDelete))
                .thenReturn(new MockedResponse<>(statusCode, null));

        flowStoreServiceConnector.deleteGatekeeperDestination(id);
    }

    // **************************************************** update gatekeeper destination tests *****************************************************************

    @Test
    public void updateGatekeeperDestination_gatekeeperDestinationIsUpdated_returnsGatekeeperDestination() throws FlowStoreServiceConnectorException {
        final GatekeeperDestination modifiedGatekeeperDestination = new GatekeeperDestinationBuilder().setPackaging("lin").build();

        // Subject under test
        final GatekeeperDestination updatedGatekeeperDestination = updateGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), modifiedGatekeeperDestination);

        // Verification
        assertThat(updatedGatekeeperDestination, is(modifiedGatekeeperDestination));
    }

    @Test
    public void updateGatekeeperDestination_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private GatekeeperDestination updateGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().build();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.GATEKEEPER_DESTINATION)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(gatekeeperDestination.getId()));

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withJsonData(gatekeeperDestination);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateGatekeeperDestination(gatekeeperDestination);
    }
}
