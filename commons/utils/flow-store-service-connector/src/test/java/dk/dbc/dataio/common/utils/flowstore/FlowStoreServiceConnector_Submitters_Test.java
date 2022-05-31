package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlowStoreServiceConnector_Submitters_Test {
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector =
            new FlowStoreServiceConnector(failSafeHttpClient, FLOW_STORE_URL);

    // ************************************** create submitter tests **************************************
    @Test
    public void createSubmitter_submitterIsCreated_returnsSubmitter() throws FlowStoreServiceConnectorException, JSONBException {
        final Submitter expected = new SubmitterBuilder().build();
        final Submitter submitter = createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expected);
        assertThat(submitter, is(expected));
    }

    @Test
    public void createSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void createSubmitter_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private Submitter createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.SUBMITTERS)
                .withJsonData(submitterContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.createSubmitter(submitterContent);
    }

    // **************************************** get submitter tests ****************************************
    @Test
    public void getSubmitter_submitterRetrieved_returnsSubmitter() throws FlowStoreServiceConnectorException {
        final Submitter expected = new SubmitterBuilder().build();
        final Submitter submitter = getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expected, expected.getId());
        assertThat(submitter, is(expected));
    }

    @Test
    public void getSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getSubmitter_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, 1),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private Submitter getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.getSubmitter(id);
    }

    // ************************************ get submitter by submitter number tests ************************************
    @Test
    public void getSubmitterBySubmitterNumber_submitterRetrieved_returnsSubmitter() throws FlowStoreServiceConnectorException {
        final Submitter expected = new SubmitterBuilder().setContent(new SubmitterContentBuilder().setNumber(42L).build()).build();
        final Submitter submitter = getSubmitterBySubmitterNumber_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expected, expected.getContent().getNumber());
        assertThat(submitter, is(expected));
    }

    @Test
    public void getSubmitterBySubmitterNumber_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getSubmitterBySubmitterNumber_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 42),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getSubmitterBySubmitterNumber_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getSubmitterBySubmitterNumber_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, 42),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private Submitter getSubmitterBySubmitterNumber_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long number) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_SEARCHES_NUMBER)
                .bind(FlowStoreServiceConstants.SUBMITTER_NUMBER_VARIABLE, number);

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.getSubmitterBySubmitterNumber(number);
    }

    // **************************************** update submitter tests ****************************************
    @Test
    public void updateSubmitter_submitterIsUpdated_returnsSubmitter() throws FlowStoreServiceConnectorException, JSONBException {
        final Submitter submitterToUpdate = new SubmitterBuilder().build();
        final Submitter updatedSubmitter = updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), submitterToUpdate, submitterToUpdate.getId(), submitterToUpdate.getVersion());
        assertThat(updatedSubmitter, is(submitterToUpdate));
    }

    @Test
    public void updateSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private Submitter updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                .withJsonData(submitterContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateSubmitter(submitterContent, id, version);
    }

    // *********************************** find all submitters tests ***********************************
    @Test
    public void findAllSubmitters_submittersRetrieved_returnsSubmitters() throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContentA = new SubmitterContentBuilder().setName("a").setNumber(1L).setDescription("submitterA").build();
        final SubmitterContent submitterContentB = new SubmitterContentBuilder().setName("B").setNumber(2L).setDescription("submitterB").build();
        final Submitter expectedSubmitterResultA = new SubmitterBuilder().setContent(submitterContentA).build();
        final Submitter expectedSubmitterResultB = new SubmitterBuilder().setContent(submitterContentB).build();

        final List<Submitter> expected = new ArrayList<>();
        expected.add(expectedSubmitterResultA);
        expected.add(expectedSubmitterResultB);

        final List<Submitter> submitters = findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), expected);

        assertThat(submitters, is(expected));
    }

    @Test
    public void findAllSubmitters_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findAllSubmitters_noResults() throws FlowStoreServiceConnectorException {
        assertThat(findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), Collections.emptyList()),
                is(Collections.emptyList()));
    }

    @Test
    public void findAllSubmitters_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private List<Submitter> findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(FlowStoreServiceConstants.SUBMITTERS);

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.findAllSubmitters();
    }
}
