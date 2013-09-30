package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlow;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlowBinder;
import static dk.dbc.dataio.integrationtest.ITUtil.createSubmitter;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the flow binders collection part of the flow store service
 */
public class FlowBindersIT {
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        restClient = HttpClient.newClient();
        dbConnection = newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        JDBCUtil.closeConnection(dbConnection);
    }

    @After
    public void tearDown() throws SQLException {
        clearAllDbTables(dbConnection);
    }

     /**
     * Given: a deployed flow-store service with a flow and a submitter
     * When: valid JSON is POSTed to the flow binders path referencing the flow and submitter
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createFlowBinder_ok() throws Exception {
        // Given...
        final long flowId = createFlow(restClient, baseUrl,
                new ITUtil.FlowContentJsonBuilder().build());
        final long submitterId = createSubmitter(restClient, baseUrl,
                new ITUtil.SubmitterContentJsonBuilder().build());

        // When...
        final String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceEntryPoint.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final long id = getResourceIdFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOW_BINDERS_TABLE_SELECT_CONTENT_STMT, id);

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowBinderContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: JSON posted to the flow binders path causes JsonException
     * Then: request returns with a NOT ACCEPTED http status code
     */
    @Test
    public void createFlowBinder_errorWhenJsonExceptionIsThrown() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceEntryPoint.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with the same name
     * Then: request returns with a CONFLICT http status code
     */
    @Test
    public void createFlowBinder_duplicateName() throws Exception {
        // Note that we set different destinations to ensure we don't risk matching search keys.

        // Given...
        final long flowId = createFlow(restClient, baseUrl,
                new ITUtil.FlowContentJsonBuilder().build());
        final long submitterId = createSubmitter(restClient, baseUrl,
                new ITUtil.SubmitterContentJsonBuilder().build());

        final String name = "createFlowBinder_duplicateName";
        final String firstFlowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setName(name)
                .setDestination("base1")
                .setFlowId(flowId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        createFlowBinder(restClient, baseUrl, firstFlowBinderContent);

        // When...
        final String secondFlowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setName(name)
                .setDestination("base2")
                .setFlowId(flowId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = HttpClient.doPostWithJson(restClient, secondFlowBinderContent, baseUrl, FlowStoreServiceEntryPoint.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: adding flow binder which references non-existing submitter
     * Then: request returns with a GONE http status code
     */
    @Test
    public void createFlowBinder_referencedSubmitterNotFound() throws Exception {
        // When...
        final long flowId = createFlow(restClient, baseUrl,
                new ITUtil.FlowContentJsonBuilder().build());
        final String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .setSubmitterIds(Arrays.asList(123456789L))
                .build();
        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceEntryPoint.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.GONE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: adding flow binder which references non-existing flow
     * Then: request returns with a GONE http status code
     */
    @Test
    public void createFlowBinder_referencedFlowNotFound() throws Exception {
        // When...
        final long submitterId = createSubmitter(restClient, baseUrl,
                new ITUtil.SubmitterContentJsonBuilder().build());
        final String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setFlowId(987654321L)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceEntryPoint.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.GONE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with different name but matching search key
     * Then: request returns with a CONFLICT http status code
     */
    @Test
    public void createFlowBinder_searchKeyExistsInSearchIndex() throws Exception {
        // Given...
        final long flowId = createFlow(restClient, baseUrl,
                new ITUtil.FlowContentJsonBuilder().build());
        final long submitterId = createSubmitter(restClient, baseUrl,
                new ITUtil.SubmitterContentJsonBuilder().build());

        String flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_1")
                .setFlowId(flowId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        createFlowBinder(restClient, baseUrl, flowBinderContent);

        // When...
        flowBinderContent = new ITUtil.FlowBinderContentJsonBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_2")
                .setFlowId(flowId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceEntryPoint.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
    }
}
