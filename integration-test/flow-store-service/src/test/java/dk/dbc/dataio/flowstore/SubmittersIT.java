package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
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
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration tests for the submitters collection part of the flow store service
 */
public class SubmittersIT {
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = ITUtil.FLOW_STORE_BASE_URL;
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
     * When : valid JSON is POSTed to the submitters path without an identifier
     * Then : a submitter it created and returned
     * And  : assert that the submitter created has an id, a version and contains the same information as the submitterContent given as input
     * And  : assert that only one submitter can be found in the underlying database
     */
    @Test
    public void createSubmitter_Ok() throws Exception{

        // When...
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        // Then...
        Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);

        // And...
        assertNotNull(submitter);
        assertNotNull(submitter.getContent());
        assertNotNull(submitter.getId());
        assertNotNull(submitter.getVersion());
        assertThat(submitter.getContent().getName(), is(submitterContent.getName()));
        assertThat(submitter.getContent().getDescription(), is(submitterContent.getDescription()));
        assertThat(submitter.getContent().getNumber(), is(submitterContent.getNumber()));
        // And ...
        final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
        assertThat(submitters.size(), is(1));
    }


    /**
     * Given: a deployed flow-store service
     * When: JSON posted to the submitters path causes JsonException
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createSubmitter_ErrorWhenJsonExceptionIsThrown() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceConstants.SUBMITTERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing submitter resource
     * When : adding submitter with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one submitter exist in the underlying database
     */
    @Test
    public void createSubmitter_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final SubmitterContent submitterContent1 = new SubmitterContentBuilder().setName("UniqueName").setNumber(1L).build();
        final SubmitterContent submitterContent2 = new SubmitterContentBuilder().setName("UniqueName").setNumber(2L).build();

        try {
            flowStoreServiceConnector.createSubmitter(submitterContent1);
            // When...
            flowStoreServiceConnector.createSubmitter(submitterContent2);
            fail("Primary key violation was not detected as input to createSubmitter().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(406));
            // And...
            List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
            assertThat(submitters.size(), is(1));
        }
    }

    /**
    * Given: a deployed flow-store service containing submitter resource
    * When : adding submitter with the same number
    * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
    * And  : request returns with a NOT ACCEPTABLE http status code
    * And  : assert that one submitter exist in the underlying database
    */
    @Test
    public void createSubmitter_duplicateNumber_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final SubmitterContent submitterContent1 = new SubmitterContentBuilder().setName("NameA").setNumber(1L).build();
        final SubmitterContent submitterContent2 = new SubmitterContentBuilder().setName("NameB").setNumber(1L).build();

        try {
            flowStoreServiceConnector.createSubmitter(submitterContent1);
            // When...
            flowStoreServiceConnector.createSubmitter(submitterContent2);
            fail("Primary key violation was not detected as input to createSubmitter().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(406));
            // And...
            List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
            assertThat(submitters.size(), is(1));
        }
    }

    /**
     * Given: a deployed flow-store service containing no submitters
     * When: GETing submitters collection
     * Then: request returns with empty list
     */
    @Test
    public void findAllSubmitters_emptyResult() throws Exception {
        // When...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();

        // Then...
        assertThat(submitters, is(notNullValue()));
        assertThat(submitters.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three submitters
     * When: GETing submitters collection
     * Then: request returns with 3 submitters
     * And: the submitters are sorted alphabetically by number
     */
    @Test
    public void findAllSubmitters_Ok() throws Exception {
        // Given...
        final SubmitterContent submitterContentA = new SubmitterContentBuilder().setName("a").setNumber(1L).setDescription("submitterA").build();
        final SubmitterContent submitterContentB = new SubmitterContentBuilder().setName("b").setNumber(2L).setDescription("submitterB").build();
        final SubmitterContent submitterContentC = new SubmitterContentBuilder().setName("c").setNumber(3L).setDescription("submitterC").build();

        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        Submitter submitterSortsFirst = flowStoreServiceConnector.createSubmitter(submitterContentA);
        Submitter submitterSortsSecond = flowStoreServiceConnector.createSubmitter(submitterContentB);
        Submitter submitterSortsThird = flowStoreServiceConnector.createSubmitter(submitterContentC);

        // When...
        List<Submitter> listOfSubmitters = flowStoreServiceConnector.findAllSubmitters();

        // Then...
        assertNotNull(listOfSubmitters);
        assertFalse(listOfSubmitters.isEmpty());
        assertThat(listOfSubmitters.size(), is (3));

        // And...
        assertThat(listOfSubmitters.get(0).getContent().getNumber(), is (submitterSortsFirst.getContent().getNumber()));
        assertThat(listOfSubmitters.get(1).getContent().getNumber(), is (submitterSortsSecond.getContent().getNumber()));
        assertThat(listOfSubmitters.get(2).getContent().getNumber(), is (submitterSortsThird.getContent().getNumber()));
    }
}
