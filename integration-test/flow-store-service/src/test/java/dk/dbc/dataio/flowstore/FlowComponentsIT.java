package dk.dbc.dataio.flowstore;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
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
import static dk.dbc.dataio.integrationtest.ITUtil.createFlowComponent;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the flow components collection part of the flow store service
 */
public class FlowComponentsIT {
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
     * When: valid JSON is POSTed to the components path
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createComponent_Ok() throws SQLException {
        // When...
        final String flowComponentContent = new FlowComponentContentJsonBuilder().build();
        final Response response = HttpClient.doPostWithJson(restClient, flowComponentContent, baseUrl, FlowStoreServiceEntryPoint.FLOW_COMPONENTS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final long id = getResourceIdFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_SELECT_CONTENT_STMT, id);

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowComponentContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the components path
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceEntryPoint.FLOW_COMPONENTS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow component resource
     * When: adding flow component with the same name
     * Then: request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlowComponent_duplicateName() throws Exception {
        final String flowComponentContent = new FlowComponentContentJsonBuilder().build();

        // Given...
        createFlowComponent(restClient, baseUrl, flowComponentContent);

        // When...
        final Response response = HttpClient.doPostWithJson(restClient, flowComponentContent, baseUrl, FlowStoreServiceEntryPoint.FLOW_COMPONENTS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing no flow components
     * When: GETing flow components collection
     * Then: request returns with a OK http status code
     * And: request returns with empty list as JSON
     */
    @Test
    public void findAllComponents_emptyResult() throws Exception {
        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceEntryPoint.FLOW_COMPONENTS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three flow components
     * When: GETing flow components collection
     * Then: request returns with a OK http status code
     * And: request returns with list as JSON of components sorted alphabetically by name
     */
    @Test
    public void findAllComponents_Ok() throws Exception {
        // Given...
        // Given...
        String flowComponentContent = new FlowComponentContentJsonBuilder()
                .setName("c")
                .build();
        final long sortsThird = createFlowComponent(restClient, baseUrl, flowComponentContent);

        flowComponentContent = new FlowComponentContentJsonBuilder()
                .setName("a")
                .build();
        final long sortsFirst = createFlowComponent(restClient, baseUrl, flowComponentContent);

        flowComponentContent = new FlowComponentContentJsonBuilder()
                .setName("b")
                .build();
        final long sortsSecond = createFlowComponent(restClient, baseUrl, flowComponentContent);

        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceEntryPoint.FLOW_COMPONENTS);

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
