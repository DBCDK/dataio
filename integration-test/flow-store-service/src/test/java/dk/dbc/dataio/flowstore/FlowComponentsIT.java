package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
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
import static dk.dbc.dataio.integrationtest.ITUtil.createFlowComponent;
import static dk.dbc.dataio.integrationtest.ITUtil.doGet;
import static dk.dbc.dataio.integrationtest.ITUtil.doPostWithJson;
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
     * When: valid JSON is POSTed to the components path
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createComponent_Ok() throws SQLException {
        // When...
        final String flowComponentContent = new FlowComponentContentJsonBuilder().build();
        final Response response = doPostWithJson(restClient, flowComponentContent, baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

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
        final Response response = doGet(restClient, baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(3));
        assertThat(responseContentNode.get(0).get("id").getLongValue(), is(sortsFirst));
        assertThat(responseContentNode.get(1).get("id").getLongValue(), is(sortsSecond));
        assertThat(responseContentNode.get(2).get("id").getLongValue(), is(sortsThird));
    }

    public static class FlowComponentJsonBuilder extends ITUtil.JsonBuilder {
        private Long id = 42L;
        private Long version = 1L;
        private String content = new FlowComponentContentJsonBuilder().build();

        public FlowComponentJsonBuilder setId(Long id) {
            this.id = id;
            return this;
        }

        public FlowComponentJsonBuilder setVersion(Long version) {
            this.version = version;
            return this;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asLongMember("id", id)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongMember("version", version)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asObjectMember("content", content));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

    public static class FlowComponentContentJsonBuilder extends ITUtil.JsonBuilder {
        private String name = "name";
        private String invocationMethod = "invocationMethod";
        private List<String> javascripts = new ArrayList<>(Arrays.asList(
                new JavaScriptJsonBuilder().build()));

        public FlowComponentContentJsonBuilder setInvocationMethod(String invocationMethod) {
            this.invocationMethod = invocationMethod;
            return this;
        }

        public FlowComponentContentJsonBuilder setJavascripts(List<String> javascripts) {
            this.javascripts = new ArrayList<>(javascripts);
            return this;
        }

        public FlowComponentContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

       public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
           stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("invocationMethod", invocationMethod)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asObjectArray("javascripts", javascripts));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

    public static class JavaScriptJsonBuilder extends ITUtil.JsonBuilder {
        private String javascript = "javascript";
        private String moduleName = "moduleName";

        public JavaScriptJsonBuilder setJavascript(String javascript) {
            this.javascript = javascript;
            return this;
        }

        public JavaScriptJsonBuilder setModuleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("javascript", javascript)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("moduleName", moduleName));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }


}
