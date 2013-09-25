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
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createSubmitter;
import static dk.dbc.dataio.integrationtest.ITUtil.doGet;
import static dk.dbc.dataio.integrationtest.ITUtil.doPostWithJson;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the submitters collection part of the flow store service
 */
public class SubmittersIT {
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
     * When: valid JSON is POSTed to the submitters path
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createSubmitter_Ok() throws SQLException {
        // When...
        final String submitterContent = new SubmitterContentJsonBuilder().build();
        final Response response = doPostWithJson(restClient, submitterContent, baseUrl, ITUtil.SUBMITTERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final long id = getResourceIdFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.SUBMITTERS_TABLE_SELECT_CONTENT_STMT, id);

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(submitterContent));
    }


    /**
     * Given: a deployed flow-store service
     * When: JSON posted to the submitters path causes JsonException
     * Then: request returns with a NOT ACCEPTED http status code
     */
    @Test
    public void createSubmitter_ErrorWhenJsonExceptionIsThrown() {
        // When...
        final Response response = doPostWithJson(restClient, "<invalid json />", baseUrl, ITUtil.SUBMITTERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing submitter resource
     * When: adding submitter with the same name
     * Then: request returns with a CONFLICT http status code
     */
    @Test
    public void createSubmitter_duplicateName() throws Exception {
        // Given...
        final String submitterContent1 = new SubmitterContentJsonBuilder()
                .setNumber(1L)
                .build();
        createSubmitter(restClient, baseUrl, submitterContent1);

        // When...
        final String submitterContent2 = new SubmitterContentJsonBuilder()
                .setNumber(2L)
                .build();
        final Response response = doPostWithJson(restClient, submitterContent2, baseUrl, ITUtil.SUBMITTERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing submitter resource
     * When: adding submitter with the same number
     * Then: request returns with a CONFLICT http status code
     */
    @Test
    public void createSubmitter_duplicateNumber() throws Exception {
        // Given...
        final String submitterContent1 = new SubmitterContentJsonBuilder()
                .setName("test1")
                .build();
        createSubmitter(restClient, baseUrl, submitterContent1);

        // When...
        final String submitterContent2 = new SubmitterContentJsonBuilder()
                .setName("test2")
                .build();
        final Response response = doPostWithJson(restClient, submitterContent2, baseUrl, ITUtil.SUBMITTERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing no submitters
     * When: GETing submitters collection
     * Then: request returns with a OK http status code
     * And: request returns with empty list as JSON
     */
    @Test
    public void findAllSubmitters_emptyResult() throws Exception {
        // When...
        final Response response = doGet(restClient, baseUrl, ITUtil.SUBMITTERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three submitters
     * When: GETing submitters collection
     * Then: request returns with a OK http status code
     * And: request returns with list as JSON of submitters sorted alphabetically by name
     */
    @Test
    public void findAllSubmitters_Ok() throws Exception {
        // Given...
        String submitterContent = new SubmitterContentJsonBuilder()
                .setName("c")
                .setNumber(1L)
                .build();
        final long sortsThird = createSubmitter(restClient, baseUrl, submitterContent);

        submitterContent = new SubmitterContentJsonBuilder()
                .setName("a")
                .setNumber(2L)
                .build();
        final long sortsFirst = createSubmitter(restClient, baseUrl, submitterContent);

        submitterContent = new SubmitterContentJsonBuilder()
                .setName("b")
                .setNumber(3L)
                .build();
        final long sortsSecond = createSubmitter(restClient, baseUrl, submitterContent);

        // When...
        final Response response = doGet(restClient, baseUrl, ITUtil.SUBMITTERS_URL_PATH);

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

    public static class SubmitterContentJsonBuilder extends ITUtil.JsonBuilder {
        private String name = "name";
        private String description = "description";
        private Long number = 42L;

        public SubmitterContentJsonBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public SubmitterContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SubmitterContentJsonBuilder setNumber(Long number) {
            this.number = number;
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongMember("number", number));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }
}
