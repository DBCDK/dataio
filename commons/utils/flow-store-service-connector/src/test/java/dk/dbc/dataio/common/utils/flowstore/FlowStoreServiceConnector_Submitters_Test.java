package dk.dbc.dataio.common.utils.flowstore;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.newFlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ProcessingException;
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
        HttpClient.class,
})
public class FlowStoreServiceConnector_Submitters_Test {


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

    // ************************************** create submitter tests **************************************
    @Test(expected = NullPointerException.class)
    public void createSubmitter_submitterContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSubmitter(null);
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

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createSubmitter_mockTestWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSubmitter_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createSubmitter_mockTestWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createSubmitter_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createSubmitter_mockTestWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    private void createSubmitter_mockTestWithSpecifiedReturnErrorCode(int statusCode, String returnValue) throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, submitterContent, FLOW_STORE_URL, FlowStoreServiceConstants.SUBMITTERS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSubmitter(submitterContent);
    }

    // **************************************** update submitter tests ****************************************
    @Test
    public void updateSubmitter_submitterIsUpdated_returnsSubmitter() throws FlowStoreServiceConnectorException, JsonException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        final Submitter submitterToUpdate = new SubmitterBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SUBMITTER_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doPostWithJson(CLIENT, headers, submitterContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), submitterToUpdate));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        Submitter updatedSubmitter = instance.updateSubmitter(submitterContent, submitterToUpdate.getId(), submitterToUpdate.getVersion());

        assertThat(updatedSubmitter, is(notNullValue()));
        assertThat(updatedSubmitter.getContent(), is(notNullValue()));
        assertThat(updatedSubmitter.getId(), is(submitterToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockTestWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSubmitter_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockTestWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSubmitter_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockTestWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSubmitter_responseWithSubmitterIDNotFound_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockTestWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    private void updateSubmitter_mockTestWithSpecifiedReturnErrorCode(int statusCode) throws ProcessingException, FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        when(HttpClient.interpolatePathVariables(eq(FlowStoreServiceConstants.SUBMITTER_CONTENT), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doPostWithJson(CLIENT, headers, submitterContent, FLOW_STORE_URL, "path"))
                .thenReturn(new MockedResponse<>(statusCode, ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateSubmitter(submitterContent, ID, VERSION);
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

        for (Submitter submitter : submitterResultList) {
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
    public void findAllSubmitters_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), eq(FLOW_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.findAllSubmitters();
    }
}
