package dk.dbc.dataio.flowstore;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlow;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the flows collection part of the flow store service
 */
public class FlowsIT {
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        restClient = HttpClient.newClient();
        dbConnection = newDbConnection("flow_store");
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
     * Given: a deployed flow-store service
     * When: valid JSON is POSTed to the flows path
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createFlow_Ok() throws SQLException {
        // When...
        final String flowContent = new FlowContentJsonBuilder().build();
        final Response response = HttpClient.doPostWithJson(restClient, flowContent, baseUrl, FlowStoreServiceEntryPoint.FLOWS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final long id = getResourceIdFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOWS_TABLE_SELECT_CONTENT_STMT, id);

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the flows path
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceEntryPoint.FLOWS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow resource
     * When: adding flow with the same name
     * Then: request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlow_duplicateName() throws Exception {
        final String flowContent = new FlowContentJsonBuilder().build();

        // Given...
        createFlow(restClient, baseUrl, flowContent);

        // When...
        final Response response = HttpClient.doPostWithJson(restClient, flowContent, baseUrl, FlowStoreServiceEntryPoint.FLOWS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing no flow resources
     * When: looking up non-existing flow
     * Then: request returns with a NOT FOUND http status code
     */
    @Test
    public void getFlow_flowNotFound() throws Exception {
        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceEntryPoint.FLOWS, Long.toString(420L));

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing a flow resources
     * When: looking up existing flow
     * Then: request returns with a OK http status code
     * And; request returns requested flow
     */
    @Test
    public void getFlow_flowFound() throws Exception {
        // Given...
        final String flowContent = new FlowContentJsonBuilder().build();
        final long flowId = getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, flowContent, baseUrl, FlowStoreServiceEntryPoint.FLOWS));

        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceEntryPoint.FLOWS, Long.toString(flowId));

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        assertThat(JsonUtil.getLongValueOrThrow(JsonUtil.getJsonRoot(responseContent).path("id"), "id assertion"), is(flowId));
    }

    /**
     * Given: a deployed flow-store service containing no flows
     * When: GETing flow collection
     * Then: request returns with a OK http status code
     * And: request returns with empty list as JSON
     */
    @Test
    public void findAllFlows_emptyResult() throws Exception {
        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceEntryPoint.FLOWS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three flows
     * When: GETing flow collection
     * Then: request returns with a OK http status code
     * And: request returns with list as JSON of flows sorted alphabetically by name
     */
    @Test
    public void findAllFlows_Ok() throws Exception {
        // Given...
        String flowContent = new FlowContentJsonBuilder()
                .setName("c")
                .build();
        final long sortsThird = createFlow(restClient, baseUrl, flowContent);

        flowContent = new FlowContentJsonBuilder()
                .setName("a")
                .build();
        final long sortsFirst = createFlow(restClient, baseUrl, flowContent);

        flowContent = new FlowContentJsonBuilder()
                .setName("b")
                .build();
        final long sortsSecond = createFlow(restClient, baseUrl, flowContent);

        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceEntryPoint.FLOWS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(3));
        assertThat(responseContentNode.get(0).get("id").longValue(), is(sortsFirst));
        assertThat(responseContentNode.get(1).get("id").longValue(), is(sortsSecond));
        assertThat(responseContentNode.get(2).get("id").longValue(), is(sortsThird));
    }

}
