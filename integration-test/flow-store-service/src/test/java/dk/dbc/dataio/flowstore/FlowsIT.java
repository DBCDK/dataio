package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlow;
import static dk.dbc.dataio.integrationtest.ITUtil.doGet;
import static dk.dbc.dataio.integrationtest.ITUtil.doPostWithJson;
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
        restClient = ClientBuilder.newClient();
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
        System.out.println(flowContent);
        final Response response = doPostWithJson(restClient, flowContent, baseUrl, ITUtil.FLOWS_URL_PATH);

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
     * Then: request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = doPostWithJson(restClient, "<invalid json />", baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: null value is POSTed to the flows path
     * Then: request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenNull() {
        // When...
        final Response response = doPostWithJson(restClient, null, baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: empty value is POSTed to the flows path
     * Then: request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenEmpty() {
        // When...
        final Response response = doPostWithJson(restClient, "", baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
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
        final Response response = doGet(restClient, baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) ITUtil.getJsonRoot(responseContent);
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
        final Response response = doGet(restClient, baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) ITUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(3));
        assertThat(responseContentNode.get(0).get("id").getLongValue(), is(sortsFirst));
        assertThat(responseContentNode.get(1).get("id").getLongValue(), is(sortsSecond));
        assertThat(responseContentNode.get(2).get("id").getLongValue(), is(sortsThird));
    }

    public static class FlowContentJsonBuilder extends ITUtil.JsonBuilder {
        private String name = "name";
        private String description = "description";
        private List<String> components = new ArrayList<>(Arrays.asList(
                new FlowComponentsIT.FlowComponentJsonBuilder().build()));

        public FlowContentJsonBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public FlowContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public FlowContentJsonBuilder setComponents(List<String> components) {
            this.components = new ArrayList<>(components);
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asObjectArray("components", components));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }
}
