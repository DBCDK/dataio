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
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.doGet;
import static dk.dbc.dataio.integrationtest.ITUtil.doPostWithJson;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdentifierFromLocationHeaderAndAssertHasValue;
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
        restClient = ClientBuilder.newClient();
        dbConnection = newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        JDBCUtil.closeConnection(dbConnection);
    }

    @After
    public void tearDown() throws SQLException {
        clearDbTables(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
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
        final String flowComponentContent = "{\"name\": \"testComponentName\"}";
        final Response response = doPostWithJson(restClient, flowComponentContent, baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final ITUtil.ResourceIdentifier resId = getResourceIdentifierFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_SELECT_CONTENT_STMT,
                resId.getId(), new Date(resId.getVersion()));

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowComponentContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the components path
     * Then: request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = doPostWithJson(restClient, "<invalid json />", baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: null value is POSTed to the components path
     * Then: request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenNull() {
        // When...
        final Response response = doPostWithJson(restClient, null, baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: empty value is POSTed to the components path
     * Then: request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenEmpty() {
        // When...
        final Response response = doPostWithJson(restClient, "", baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

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
        final Response response = doGet(restClient, baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) ITUtil.getJsonRoot(responseContent);
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
        final ITUtil.ResourceIdentifier sortsFirst = new ITUtil.ResourceIdentifier(1L, new Date().getTime());
        final ITUtil.ResourceIdentifier sortsSecond = new ITUtil.ResourceIdentifier(2L, new Date().getTime());
        final ITUtil.ResourceIdentifier sortsThird = new ITUtil.ResourceIdentifier(3L, new Date().getTime());

        final String componentContent = "{}";

        JDBCUtil.update(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_INSERT_STMT,
                sortsThird.getId(), new Date(sortsThird.getVersion()), componentContent, "c");
        JDBCUtil.update(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_INSERT_STMT,
                sortsFirst.getId(), new Date(sortsFirst.getVersion()), componentContent, "a");
        JDBCUtil.update(dbConnection, ITUtil.FLOW_COMPONENTS_TABLE_INSERT_STMT,
                sortsSecond.getId(), new Date(sortsSecond.getVersion()), componentContent, "b");

        // When...
        final Response response = doGet(restClient, baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

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

    public static class FlowComponentJsonBuilder extends ITUtil.JsonBuilder {
        private Long id = 42L;
        private Long version = 1L;

        public FlowComponentJsonBuilder setId(Long id) {
            this.id = id;
            return this;
        }

        public FlowComponentJsonBuilder setVersion(Long version) {
            this.version = version;
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asLongMember("id", id)); stringBuilder.append(MEMBER_DELIMITER);
            //stringBuilder.append(asLongMember("version", version)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongMember("version", version));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

}
