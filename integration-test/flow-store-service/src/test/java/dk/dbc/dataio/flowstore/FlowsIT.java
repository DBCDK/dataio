package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
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
 * Integration tests for the flows collection part of the flow store service
 */
public class FlowsIT {
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
     * When : valid JSON is POSTed to the flows path without an identifier
     * Then : a flow it created and returned
     * And  : assert that the flow created has an id, a version and contains the same information as the flowContent given as input
     * And  : assert that only one flow can be found in the underlying database
     */
    @Test
    public void createFlow_ok() throws Exception{

        // When...
        final FlowContent flowContent = new FlowContentBuilder().build();
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        // Then...
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // And...
        assertNotNull(flow);
        assertNotNull(flow.getContent());
        assertNotNull(flow.getId());
        assertNotNull(flow.getVersion());
        assertThat(flow.getContent().getName(), is(flowContent.getName()));
        assertThat(flow.getContent().getDescription(), is(flowContent.getDescription()));
        // And ...
        final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
        assertThat(flows.size(), is(1));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the flows path
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createFlow_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceConstants.FLOWS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow resource
     * When : adding flow with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one flow exist in the underlying database
     */
    @Test
    public void createFlow_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final FlowContent flowContent = new FlowContentBuilder().setName("UniqueName").build();

        try {
            flowStoreServiceConnector.createFlow(flowContent);
            // When...
            flowStoreServiceConnector.createFlow(flowContent);
            fail("Primary key violation was not detected as input to createFlow().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(406));
            // And...
            List<Flow> flows = flowStoreServiceConnector.findAllFlows();
            assertThat(flows.size(), is(1));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow with an unknown flow id
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlow_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException{
        try{
            // Given...
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
            flowStoreServiceConnector.getFlow(234);

            fail("Invalid request to getFlow() was not detected.");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with a valid identifier
     * Then : a flow is found and returned
     * And  : assert that the flow found has an id, a version and contains the same information as the flow created
     */
    @Test
    public void getFlow_ok() throws Exception{

        // When...
        final FlowContent flowContent = new FlowContentBuilder().build();
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        // Then...
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);
        Flow flowToGet = flowStoreServiceConnector.getFlow(flow.getId());

        // And...
        assertNotNull(flowToGet);
        assertNotNull(flowToGet.getContent());
        assertThat(flowToGet.getContent().getName(), is(flowToGet.getContent().getName()));
        assertThat(flowToGet.getContent().getDescription(), is(flow.getContent().getDescription()));
    }

    /**
     * Given: a deployed flow-store service containing no flows
     * When: GETing flows collection
     * Then: request returns with empty list
     */
    @Test
    public void findAllFlows_emptyResult() throws Exception {
        // When...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final List<Flow> flows = flowStoreServiceConnector.findAllFlows();

        // Then...
        assertThat(flows, is(notNullValue()));
        assertThat(flows.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three flows
     * When: GETing flows collection
     * Then: request returns with 3 flows
     * And: the flows are sorted alphabetically by name
     */
    @Test
    public void findAllFlows_Ok() throws Exception {
        // Given...
        final FlowContent flowContentA = new FlowContentBuilder().setName("a").build();
        final FlowContent flowContentB = new FlowContentBuilder().setName("b").build();
        final FlowContent flowContentC = new FlowContentBuilder().setName("c").build();

        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        Flow flowSortsThird = flowStoreServiceConnector.createFlow(flowContentC);
        Flow flowSortsFirst = flowStoreServiceConnector.createFlow(flowContentA);
        Flow flowSortsSecond = flowStoreServiceConnector.createFlow(flowContentB);

        // When...
        List<Flow> listOfFlows = flowStoreServiceConnector.findAllFlows();

        // Then...
        assertNotNull(listOfFlows);
        assertFalse(listOfFlows.isEmpty());
        assertThat(listOfFlows.size(), is (3));

        // And...
        assertThat(listOfFlows.get(0).getContent().getName(), is (flowSortsFirst.getContent().getName()));
        assertThat(listOfFlows.get(1).getContent().getName(), is (flowSortsSecond.getContent().getName()));
        assertThat(listOfFlows.get(2).getContent().getName(), is (flowSortsThird.getContent().getName()));
    }
}
