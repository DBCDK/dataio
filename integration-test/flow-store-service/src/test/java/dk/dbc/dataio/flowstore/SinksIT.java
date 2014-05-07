package dk.dbc.dataio.flowstore;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
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

import static dk.dbc.dataio.integrationtest.ITUtil.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

public class SinksIT {

    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        dbConnection = newDbConnection("flow_store");
        restClient = HttpClient.newClient();
        //logger = LoggerFactory.getLogger(SinksIT.class);
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        JDBCUtil.closeConnection(dbConnection);
    }

    @After
    public void tearDown() throws SQLException {
        clearDbTables(dbConnection, ITUtil.SINKS_TABLE_NAME);
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the sinks path without an identifier
     * Then : a sink it created and returned
     * And  : the sink created has an id, a version and contains the same information as the sinkContent given as input
     * And  : posted data can be found in the underlying database
     */
    @Test
    public void createSink_ok(){
        try{
            // When...
            final SinkContent sinkContent = new SinkContentBuilder().build();
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
            Sink sink = flowStoreServiceConnector.createSink(sinkContent);
            // Then...
            assertNotNull(sink);
            assertNotNull(sink.getContent());
            assertNotNull(sink.getId());
            assertNotNull(sink.getVersion());
            assertThat(sink.getContent().getName(), is(sinkContent.getName()));
            assertThat(sink.getContent().getResource(), is(sinkContent.getResource()));
            // And ...
            final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.SINKS_TABLE_SELECT_CONTENT_STMT, sink.getId());
            assertThat(rs.size(), is(1));
        }catch(Exception e){
            fail("Unexpected error when creating new sink.");
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : given sinkContent that contains an empty parameter for name
     * Then : IllegalArgumentException is thrown
     */
    @Test
    public void createSink_InvalidRequest(){
        try{
            // When...
            final SinkContent sinkContent = new SinkContent("", "resource");
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
            flowStoreServiceConnector.createSink(sinkContent);
            fail("Invalid input to createSink() was not detected.");
        }catch(Exception e){
            // Then...
            assertTrue(e instanceof IllegalArgumentException);
            assertTrue(e.getMessage().toString().contains("Value of parameter"));
            assertTrue(e.getMessage().toString().contains("cannot be empty"));
        }
    }

    /**
     * Given: a deployed flow-store service containing sink resource
     * When : adding sink with the same name
     * Then : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createSink_duplicateName_NotAcceptable(){
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        // Given...
        final SinkContent sinkContent1 = new SinkContentBuilder().build();
        final SinkContent sinkContent2 = new SinkContentBuilder().build();
        // When...
        try {
            flowStoreServiceConnector.createSink(sinkContent1);
            flowStoreServiceConnector.createSink(sinkContent2);
            fail("No primary key violation was detected.");
        }catch(Exception e){
            assertTrue(e instanceof FlowStoreServiceConnectorException);
            assertTrue(e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException);
            assertTrue(e.getMessage().toString().equals("flow-store service returned with unexpected status code: Not Acceptable"));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid sink with given id is already stored
     * When : valid JSON is POSTed to the sinks path with an identifier (update)
     * Then : request returns with a OK http status code
     * And  : updated data can be found in the underlying database
     */
    @Test
    public void updateSink_ok() throws SQLException {
        // Given ...
        final long id = createSink(restClient, baseUrl, new SinkContentJsonBuilder().build());
        // Do update
        final String newSinkContent = new SinkContentJsonBuilder().setName("UpdatedSinkName").setResource("NewResourceName").build();
        // Assume, that the very first created sink has version number 1:
        final Response updateResponse = HttpClient.doPostWithJson(restClient, newSinkContent, baseUrl, FlowStoreServiceConstants.SINKS, Long.toString(id), Long.toString(1L), "content");
        // Then...
        assertThat(updateResponse.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));
        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.SINKS_TABLE_SELECT_CONTENT_STMT, id);
        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(newSinkContent));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the sinks path with update causes JsonException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateSink_invalidJson_BadRequest() {
        // Given ...
        final long id = createSink(restClient, baseUrl, new SinkContentJsonBuilder().build());
        // Do an attempt to update
        // Assume, that the very first created sink has version number 1:
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceConstants.SINKS, Long.toString(id), Long.toString(1L), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid sink with given id is already stored
     * When : valid JSON is POSTed to the sinks path with an identifier (update) and wrong id number
     * Then : request returns with a NOT_FOUND http status code
     * And  : updated data can NOT be found in the underlying database
     */
    @Test
    public void updateSink_WrongIdNumber_NotFound() throws SQLException {
        // Given ...
        final String defaultSinkContent = new SinkContentJsonBuilder().build();
        final long id = createSink(restClient, baseUrl, defaultSinkContent);
        // Do update
        final String newSinkContent = new SinkContentJsonBuilder().setName("UpdatedSinkName").setResource("NewResourceName").build();
        // Assume, that the very first created sink has version number 1:
        final Response updateResponse = HttpClient.doPostWithJson(restClient, newSinkContent, baseUrl, FlowStoreServiceConstants.SINKS, "1234", Long.toString(1L), "content");
        // Then...
        assertThat(updateResponse.getStatusInfo().getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.SINKS_TABLE_SELECT_CONTENT_STMT, id);
        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(defaultSinkContent));  // Test that the old sink is still there - ie. no update has been done
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid sink with given id is already stored
     * When : valid JSON is POSTed to the sinks path with an identifier (update) and wrong version number
     * Then : request returns with a CONFLICT http status code
     * And  : updated data can NOT be found in the underlying database
     */
    @Test
    public void updateSink_WrongVersion_Conflict() throws SQLException {
        // Given ...
        final String defaultSinkContent = new SinkContentJsonBuilder().build();
        final long id = createSink(restClient, baseUrl, defaultSinkContent);
        // Do update
        final String newSinkContent = new SinkContentJsonBuilder().setName("UpdatedSinkName").setResource("NewResourceName").build();
        // Assume, that the very first created sink has version number 1, but enforce error (use version 2 instead):
        final Response updateResponse = HttpClient.doPostWithJson(restClient, newSinkContent, baseUrl, FlowStoreServiceConstants.SINKS, Long.toString(id), Long.toString(2L), "content");
        // Then...
        assertThat(updateResponse.getStatusInfo().getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.SINKS_TABLE_SELECT_CONTENT_STMT, id);
        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(defaultSinkContent));  // Test that the old sink is still there - ie. no update has been done
    }

    /**
     * Given: a deployed flow-store service containing no sinks
     * When: GETing sinks collection
     * Then: request returns with a OK http status code
     * And: request returns with empty list as JSON
     */
    @Test
    public void findAllSinks_emptyResult() throws Exception {
        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceConstants.SINKS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three sinks
     * When: GETing sinks collection
     * Then: request returns with a OK http status code
     * And: request returns with list as JSON of sinks sorted alphabetically by name
     */
    @Test
    public void findAllSinks_Ok() throws Exception {
        // Given...
        String sinkContent = new SinkContentJsonBuilder()
                .setName("c")
                .build();
        final long sortsThird = createSink(restClient, baseUrl, sinkContent);

        sinkContent = new SinkContentJsonBuilder()
                .setName("a")
                .build();
        final long sortsFirst = createSink(restClient, baseUrl, sinkContent);

        sinkContent = new SinkContentJsonBuilder()
                .setName("b")
                .build();
        final long sortsSecond = createSink(restClient, baseUrl, sinkContent);

        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceConstants.SINKS);

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
