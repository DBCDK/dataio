package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SinksIT {

    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        dbConnection = newDbConnection("flow_store");
        restClient = HttpClient.newClient();
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
     * And  : assert that the sink created has an id, a version and contains the same information as the sinkContent given as input
     * And  : assert that only one sink can be found in the underlying database
     */
    @Test
    public void createSink_ok(){
        try{
            // When...
            final SinkContent sinkContent = new SinkContentBuilder().build();
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

            // Then...
            Sink sink = flowStoreServiceConnector.createSink(sinkContent);

            // And...
            assertNotNull(sink);
            assertNotNull(sink.getContent());
            assertNotNull(sink.getId());
            assertNotNull(sink.getVersion());
            assertThat(sink.getContent().getName(), is(sinkContent.getName()));
            assertThat(sink.getContent().getResource(), is(sinkContent.getResource()));
            // And ...
            final List<Sink> sinks = flowStoreServiceConnector.findAllSinks();
            assertThat(sinks.size(), is(1));
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
            final SinkContent sinkContent = new SinkContentBuilder().setName("").setResource("Resource").build();
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
            flowStoreServiceConnector.createSink(sinkContent);
            fail("Invalid request to createSink() was not detected.");
        }catch(Exception e){
            // Then...
            assertTrue(e instanceof IllegalArgumentException);
            assertTrue(e.getMessage().contains("Value of parameter"));
            assertTrue(e.getMessage().contains("cannot be empty"));
        }
    }

    /**
     * Given: a deployed flow-store service containing sink resource
     * When : adding sink with the same name
     * Then : assert that the exception thrown is of the type: FlowStoreServiceConnectorException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one sinks exist in the underlying database
     */
    @Test
    public void createSink_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        final SinkContent sinkContent = new SinkContentBuilder().setName("UniqueName").build();

        try {
            flowStoreServiceConnector.createSink(sinkContent);

            // When...
            flowStoreServiceConnector.createSink(sinkContent);
            fail("Primary key violation was not detected as input to createSink().");
        }catch(Exception e){

            // Then...
            assertTrue(e instanceof FlowStoreServiceConnectorException);
            assertTrue(e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException);

            // And...
            assertTrue(e.getMessage().equals("flow-store service returned with unexpected status code: Not Acceptable"));

            // And...
            List<Sink> sinks = flowStoreServiceConnector.findAllSinks();
            assertThat(sinks.size(), is(1));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid sink with given id is already stored
     * When : valid JSON is POSTed to the sinks path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the sink has not changed
     * And  : assert that the version number has been updated
     * And  : assert that updated data can be found in the underlying database and only one sink exists
     */
    @Test
    public void updateSink_ok(){
        try {
            // Given...
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

            // And...
            final SinkContent sinkContent = new SinkContentBuilder().build();
            Sink sink = flowStoreServiceConnector.createSink(sinkContent);

            // When...
            final SinkContent newSinkContent = new SinkContentBuilder().setName("UpdatedSinkName").setResource("NewResourceName").build();
            Sink updatedSink = flowStoreServiceConnector.updateSink(newSinkContent, sink.getId(), sink.getVersion());

            // Then...
            assertNotNull(updatedSink);
            assertNotNull(updatedSink.getContent());
            assertNotNull(updatedSink.getId());
            assertNotNull(updatedSink.getVersion());
            assertThat(updatedSink.getContent().getName(), is(newSinkContent.getName()));
            assertThat(updatedSink.getContent().getResource(), is(newSinkContent.getResource()));

            // And...
            assertThat(updatedSink.getId(), is(sink.getId()));

            // And...
            assertThat(updatedSink.getVersion(), not(sink.getVersion()));
            assertThat(updatedSink.getVersion(), is(sink.getVersion() + 1));

            // And...
            final List<Sink> sinks = flowStoreServiceConnector.findAllSinks();
            assertThat(sinks.size(), is(1));

        }catch(Exception e){
            e.printStackTrace();
            fail("Unexpected error when updating existing sink.");
       }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid sink with given id is already stored
     * When : JSON posted to the sinks path with update causes JsonException
     * Then : IllegalArgumentException is thrown
     */
    @Test
    public void updateSink_invalidRequest(){
        try{
            // Given...
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

            // And...
            final SinkContent sinkContent = new SinkContentBuilder().build();
            Sink sink = flowStoreServiceConnector.createSink(sinkContent);

            // When...
            final SinkContent newSinkContent = new SinkContentBuilder().setName("").setResource("Resource").build();
            flowStoreServiceConnector.updateSink(newSinkContent, sink.getId(), sink.getVersion());

            fail("Invalid request to updateSink() was not detected.");
        }catch(Exception e) {
            // Then...
            assertTrue(e instanceof IllegalArgumentException);
            assertTrue(e.getMessage().contains("Value of parameter"));
            assertTrue(e.getMessage().contains("cannot be empty"));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the sinks path with an identifier (update) and wrong id number
     * Then : assert that the exception thrown is of the type: FlowStoreServiceConnectorException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that only no sinks exist in the underlying database
     * And  : assert that updated data from the first user can be found in the underlying database
     */
    @Test
    public void updateSink_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        try{
            // When...
            final SinkContent newSinkContent = new SinkContentBuilder().build();
            flowStoreServiceConnector.updateSink(newSinkContent, 1234, 1L);

            fail("None existing id was input to updateSink() was not detected.");
        }catch(Exception e) {
            // Then...
            assertTrue(e instanceof FlowStoreServiceConnectorException);
            assertTrue(e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException);
            assertTrue(e.getMessage().equals("flow-store service returned with unexpected status code: Not Found"));

            // And...
            final List<Sink> sinks = flowStoreServiceConnector.findAllSinks();
            assertNotNull(sinks);
            assertThat(sinks.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : Two valid sinks are already stored
     * When : valid JSON is POSTed to the sinks path with an identifier (update) but with a sink name that is already in use by another existing sink
     * Then : assert that the exception thrown is of the type: FlowStoreServiceConnectorException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     * And  : assert that two sinks exists in the underlying database
     * And  : updated data cannot be found in the underlying database
     */
    @Test
    public void updateSink_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final String FIRST_SINK_NAME = "FirstSinkName";
        final String SECOND_SINK_NAME = "SecondSinkName";

        try {
            // And...
            final SinkContent sinkContent1 = new SinkContentBuilder().setName(FIRST_SINK_NAME).build();
            flowStoreServiceConnector.createSink(sinkContent1);

            final SinkContent sinkContent2 = new SinkContentBuilder().setName(SECOND_SINK_NAME).build();
            Sink sink = flowStoreServiceConnector.createSink(sinkContent2);

            // When... (Attempting to save the second sink created with the same name as the first sink created)
            flowStoreServiceConnector.updateSink(sinkContent1, sink.getId(), sink.getVersion());

            fail("Primary key violation was not detected as input to updateSink().");
        }catch(Exception e) {

            // Then...
            assertTrue(e instanceof FlowStoreServiceConnectorException);
            assertTrue(e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException);

            // And...
            assertTrue(e.getMessage().equals("flow-store service returned with unexpected status code: Not Acceptable"));

            // And...
            final List<Sink> sinks = flowStoreServiceConnector.findAllSinks();
            assertNotNull(sinks);
            assertThat(sinks.size(), is(2));

            // And...
            assertThat(sinks.get(0).getContent().getName(), is (FIRST_SINK_NAME));
            assertThat(sinks.get(1).getContent().getName(), is (SECOND_SINK_NAME));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid sink with given id is already stored and the sink is opened for edit by two different users
     * And  : the first user updates the sink, valid JSON is POSTed to the sinks path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the sink, valid JSON is POSTed to the sinks
     *        path with an identifier (update) and wrong version number

     * Then : assert that the exception thrown is of the type: FlowStoreServiceConnectorException
     * And  : request returns with a CONFLICT http status code
     * And  : assert that only one sink exists in the underlying database
     * And  : assert that updated data from the first user can be found in the underlying database
     * And  : assert that the version number has been updated only by the first user
     */
    @Test
    public void updateSink_WrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final String SINK_NAME_FROM_FIRST_USER = "UpdatedSinkNameFromFirstUser";
        final String SINK_NAME_FROM_SECOND_USER = "UpdatedSinkNameFromSecondUser";

        try {
            // And...
            final SinkContent sinkContent = new SinkContentBuilder().build();
            Sink sink = flowStoreServiceConnector.createSink(sinkContent);

            // And... First user updates the sink
            flowStoreServiceConnector.updateSink(new SinkContentBuilder().setName(SINK_NAME_FROM_FIRST_USER).build(),
                    sink.getId(),
                    sink.getVersion());

            // When... Second user attempts to update the same sink
            flowStoreServiceConnector.updateSink(new SinkContentBuilder().setName(SINK_NAME_FROM_SECOND_USER).build(),
                    sink.getId(),
                    sink.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateSink().");


        } catch (Exception e) {
            // Then...
            assertTrue(e instanceof FlowStoreServiceConnectorException);
            assertTrue(e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException);

            // And...
            assertTrue(e.getMessage().equals("flow-store service returned with unexpected status code: Conflict"));

            // And...
            final List<Sink> sinks = flowStoreServiceConnector.findAllSinks();
            assertNotNull(sinks);
            assertThat(sinks.size(), is(1));
            assertThat(sinks.get(0).getContent().getName(), is(SINK_NAME_FROM_FIRST_USER));

            // And... Assume that the initial version number, when the sink was first created, is 1
            assertThat(sinks.get(0).getVersion(), is(2L));
        }
    }

    /**
     * Given: a deployed flow-store service containing no sinks
     * When: GETing sinks collection
     * Then: request returns with empty list
     */
    @Test
    public void findAllSinks_emptyResult() throws Exception {
        // When...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final List<Sink> sinks = flowStoreServiceConnector.findAllSinks();

        // Then...
        assertThat(sinks, is(notNullValue()));
        assertThat(sinks.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three sinks
     * When: GETing sinks collection
     * Then: request returns with 3 sinks
     * And: the sinks are sorted alphabetically by name
     */
    @Test
    public void findAllSinks_Ok() throws Exception {
        // Given...
        final SinkContent sinkContentA = new SinkContentBuilder().setName("a").build();
        final SinkContent sinkContentB = new SinkContentBuilder().setName("b").build();
        final SinkContent sinkContentC = new SinkContentBuilder().setName("c").build();

        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        Sink sinkSortsFirst = flowStoreServiceConnector.createSink(sinkContentA);
        Sink sinkSortsSecond = flowStoreServiceConnector.createSink(sinkContentB);
        Sink sinkSortsThird = flowStoreServiceConnector.createSink(sinkContentC);

        // When...
        List<Sink> listOfSinks = flowStoreServiceConnector.findAllSinks();

        // Then...
        assertNotNull(listOfSinks);
        assertFalse(listOfSinks.isEmpty());
        assertThat(listOfSinks.size(), is (3));

        // And...
        assertThat(listOfSinks.get(0).getContent().getName(), is (sinkSortsFirst.getContent().getName()));
        assertThat(listOfSinks.get(1).getContent().getName(), is (sinkSortsSecond.getContent().getName()));
        assertThat(listOfSinks.get(2).getContent().getName(), is (sinkSortsThird.getContent().getName()));
    }
}
