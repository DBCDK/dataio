package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.flowstore.ITUtil.clearDbTables;
import static dk.dbc.dataio.flowstore.ITUtil.doGet;
import static dk.dbc.dataio.flowstore.ITUtil.doPostWithFormData;
import static dk.dbc.dataio.flowstore.ITUtil.doPostWithJson;
import static dk.dbc.dataio.flowstore.ITUtil.getResourceIdentifierFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.flowstore.ITUtil.newDbConnection;
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

    private final ITUtil.ResourceIdentifier flowRes = new ITUtil.ResourceIdentifier(0L, new Date().getTime());
    private final ITUtil.ResourceIdentifier componentRes = new ITUtil.ResourceIdentifier(0L, new Date().getTime());
    private final String flowContent = "{\"name\":\"flowname\",\"components\":[]}";
    private final String componentContent = "{\"name\":\"componentName\"}";

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
        clearDbTables(dbConnection, ITUtil.FLOWS_TABLE_NAME, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
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
        final String flowContent = "{\"name\": \"testName\"}";
        final Response response = doPostWithJson(restClient, flowContent, baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final ITUtil.ResourceIdentifier resId = getResourceIdentifierFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOWS_TABLE_SELECT_CONTENT_STMT,
                resId.getId(), new Date(resId.getVersion()));

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the flows path
     * Then: request returns with a INTERNAL SERVER ERROR http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = doPostWithJson(restClient, "<invalid json />", baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: null value is POSTed to the flows path
     * Then: request returns with a INTERNAL SERVER ERROR http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenNull() {
        // When...
        final Response response = doPostWithJson(restClient, null, baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: empty value is POSTed to the flows path
     * Then: request returns with a INTERNAL SERVER ERROR http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenEmpty() {
        // When...
        final Response response = doPostWithJson(restClient, "", baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
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
        final ITUtil.ResourceIdentifier sortsFirst = new ITUtil.ResourceIdentifier(1L, new Date().getTime());
        final ITUtil.ResourceIdentifier sortsSecond = new ITUtil.ResourceIdentifier(2L, new Date().getTime());
        final ITUtil.ResourceIdentifier sortsThird = new ITUtil.ResourceIdentifier(3L, new Date().getTime());

        final String flowContent = "{}";

        JDBCUtil.update(dbConnection, ITUtil.FLOWS_TABLE_INSERT_STMT,
                sortsThird.getId(), new Date(sortsThird.getVersion()), flowContent, "c");
        JDBCUtil.update(dbConnection, ITUtil.FLOWS_TABLE_INSERT_STMT,
                sortsFirst.getId(), new Date(sortsFirst.getVersion()), flowContent, "a");
        JDBCUtil.update(dbConnection, ITUtil.FLOWS_TABLE_INSERT_STMT,
                sortsSecond.getId(), new Date(sortsSecond.getVersion()), flowContent, "b");

        // When...
        final Response response = doGet(restClient, baseUrl, ITUtil.FLOWS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) ITUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(3));
        assertThat(responseContentNode.get(0).get("id").getLongValue(), is(sortsFirst.getId()));
        assertThat(responseContentNode.get(1).get("id").getLongValue(), is(sortsSecond.getId()));
        assertThat(responseContentNode.get(2).get("id").getLongValue(), is(sortsThird.getId()));
    }

    /**
     * Given: a deployed flow-store service containing flow component
     * When: adding component to flow resource which do not exist
     * Then: request returns with a NOT FOUND http status code
     */
    @Test
    public void addFlowComponent_ErrorWhenFlowNotFound() throws Exception {
        // Given...
        JDBCUtil.update(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_INSERT_STMT,
                componentRes.getId(), new Date(componentRes.getVersion()), componentContent, "indxval");

        // When...
        final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("id", componentRes.getId().toString());
        formData.add("version", componentRes.getVersion().toString());
        final Response response = doPostWithFormData(restClient, formData, baseUrl,
                ITUtil.FLOWS_URL_PATH, flowRes.getId().toString(), flowRes.getVersion().toString(), ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow
     * When: adding non-existing component to flow
     * Then: request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void addFlowComponent_ErrorWhenComponentNotFound() throws Exception {
        // Given...
        JDBCUtil.update(dbConnection, ITUtil.FLOWS_TABLE_INSERT_STMT,
                flowRes.getId(), new Date(flowRes.getVersion()), flowContent, "indxval");

        // When...
        final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("id", componentRes.getId().toString());
        formData.add("version", componentRes.getVersion().toString());
        final Response response = doPostWithFormData(restClient, formData, baseUrl,
                ITUtil.FLOWS_URL_PATH, flowRes.getId().toString(), flowRes.getVersion().toString(), ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.PRECONDITION_FAILED.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow and flow component
     * When: adding component to flow
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created version of the flow resource
     * And: flow data with embedded component can be found in the underlying database
     */
    @Test
    public void addFlowComponent_Ok() throws Exception {
        // Given...
        JDBCUtil.update(dbConnection, ITUtil.FLOWS_TABLE_INSERT_STMT,
                flowRes.getId(), new Date(flowRes.getVersion()), flowContent, "indxval");
        JDBCUtil.update(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_INSERT_STMT,
                componentRes.getId(), new Date(componentRes.getVersion()), componentContent, "indxval");

        // When...
        final MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("id", componentRes.getId().toString());
        formData.add("version", componentRes.getVersion().toString());
        final Response response = doPostWithFormData(restClient, formData, baseUrl,
                ITUtil.FLOWS_URL_PATH, flowRes.getId().toString(), flowRes.getVersion().toString(), ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final ITUtil.ResourceIdentifier resId = getResourceIdentifierFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOWS_TABLE_SELECT_CONTENT_STMT,
                resId.getId(), new Date(resId.getVersion()));

        assertThat(rs.size(), is(1));
        final String createdContent = (String) rs.get(0).get(0);
        final JsonNode createdContentNode = ITUtil.getJsonRoot(createdContent);
        final ArrayNode createdContentComponentsNode = (ArrayNode) createdContentNode.get("components");
        assertThat(createdContentComponentsNode.size(), is(1));
        assertThat(createdContentComponentsNode.get(0).get("id").getLongValue(), is(componentRes.getId()));
        assertThat(createdContentComponentsNode.get(0).get("version").getLongValue(), is(componentRes.getVersion()));
        assertThat(createdContentComponentsNode.get(0).get("content").toString(), is(componentContent));
    }
}
