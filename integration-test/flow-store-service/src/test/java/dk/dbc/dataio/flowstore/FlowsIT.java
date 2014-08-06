package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
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
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.not;
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

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow with given id is already stored
     * When : valid JSON is POSTed to the flow path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the flow has not changed
     * And  : assert that the version number has been updated
     * And  : assert that updated data can be found in the underlying database and only one flow exists
     */
    @Test
    public void updateFlowComponentsInFlowToLatestVersion_ok() throws Exception{

        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        // And...
        // Create flow component
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        final List<FlowComponent> flowComponents = new ArrayList<>();
        flowComponents.add(flowComponent);

        // Create flow containing the flow component created above
        final FlowContent flowContent = new FlowContentBuilder().setComponents(flowComponents).build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // Update the component to a newer revision
        FlowComponentContent updatedFlowComponentContent = new FlowComponentContent(
                flowComponent.getContent().getName(),
                flowComponent.getContent().getSvnProjectForInvocationJavascript(),
                flowComponent.getContent().getSvnRevision() + 1,
                flowComponent.getContent().getInvocationJavascriptName(),
                flowComponent.getContent().getJavascripts(),
                flowComponent.getContent().getInvocationMethod());

        flowStoreServiceConnector.updateFlowComponent(updatedFlowComponentContent, flowComponent.getId(), flowComponent.getVersion());

        // When...
        // Update the flow component embedded within the flow to the latest svn revision
        Flow updatedFlow = flowStoreServiceConnector.updateFlowComponentsInFlowToLatestVersion(flow.getId(), flow.getVersion());

        // Then...
        assertNotNull(updatedFlow);
        assertNotNull(updatedFlow.getContent());
        assertNotNull(updatedFlow.getId());
        assertNotNull(updatedFlow.getVersion());
        assertThat(updatedFlow.getContent().getName(), is(flowContent.getName()));
        assertThat(updatedFlow.getContent().getDescription(), is(flowContent.getDescription()));

        for(FlowComponent updatedFlowComponent : updatedFlow.getContent().getComponents()){
            assertThat(updatedFlowComponent.getContent().getSvnRevision(), not(flowComponent.getContent().getSvnRevision()));
        }

        // And...
        assertThat(updatedFlow.getId(), is(flow.getId()));

        // And...
        assertThat(updatedFlow.getVersion(), is(flow.getVersion() + 1));

        // And...
        final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
        assertThat(flows.size(), is(1));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flows exist in the underlying database
     */
    @Test
    public void updateFlowComponentsInFlowToLatestVersion_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        try{
            // When...
            flowStoreServiceConnector.updateFlowComponentsInFlowToLatestVersion(1234, 1L);

            fail("Wrong flow Id was not detected as input to updateFlowComponentsInFlowToLatestVersion().");

            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(404));

            // And...
            final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
            assertNotNull(flows);
            assertThat(flows.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow with given id is already stored and the flow is opened for edit by two different users
     * And  : the first user updates the flow, valid JSON is POSTed to the flows path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the flow, valid JSON is POSTed to the flow components
     *        path with an identifier (update) and wrong version number

     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     * And  : assert the flow component embedded within the flow has been updated to the latest version
     * And  : assert that only one flow exists in the underlying database
     * And  : assert that updated data from the first user can be found in the underlying database
     * And  : assert that the version number has been updated only by the first user
     */
    @Test
    public void updateFlowComponentsInFlowToLatestVersion_WrongVersion_Conflict() throws FlowStoreServiceConnectorException {

        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        long flowVersion = -1;
        long flowComponentId = -1;
        long flowComponentVersion = -1;
        long flowId = -1;

        try {
            // Create flow component
            final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
            FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
            flowComponentVersion = flowComponent.getVersion();
            flowComponentId = flowComponent.getId();

            final List<FlowComponent> flowComponents = new ArrayList<>();
            flowComponents.add(flowComponent);

            // Create flow containing the flow component created above
            final FlowContent flowContent = new FlowContentBuilder().setComponents(flowComponents).build();
            Flow flow = flowStoreServiceConnector.createFlow(flowContent);
                flowVersion = flow.getVersion();
                flowId = flow.getId();

            // Update the component to a newer revision
            FlowComponentContent updatedFlowComponentContent = new FlowComponentContent(
                    flowComponent.getContent().getName(),
                    flowComponent.getContent().getSvnProjectForInvocationJavascript(),
                    flowComponent.getContent().getSvnRevision() + 1,
                    flowComponent.getContent().getInvocationJavascriptName(),
                    flowComponent.getContent().getJavascripts(),
                    flowComponent.getContent().getInvocationMethod());

            flowStoreServiceConnector.updateFlowComponent(updatedFlowComponentContent, flowComponent.getId(), flowComponent.getVersion());

            // And... First user updates the flow
            flowStoreServiceConnector.updateFlowComponentsInFlowToLatestVersion(flow.getId(), flow.getVersion());

            // When... Second user attempts to update the same flow
            flowStoreServiceConnector.updateFlowComponentsInFlowToLatestVersion(flow.getId(), flow.getVersion());
            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateFlowComponentsInFlowToLatestVersion().");

            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(409));

            // And... Find the flowComponent used by the flow, with the latest revision
            final FlowComponent flowComponent = flowStoreServiceConnector.getFlowComponent(flowComponentId);
            assertNotNull(flowComponent);
            assertThat(flowComponent.getVersion(), is(flowComponentVersion +1));

            // Assert that the flowComponent embedded in the flow matches the flow component found
            final Flow flow = flowStoreServiceConnector.getFlow(flowId);
            assertThat(flow.getContent().getComponents().get(0).getId(), is(flowComponent.getId()));
            assertThat(flow.getContent().getComponents().get(0).getVersion(), is(flowComponent.getVersion()));
            assertThat(flow.getContent().getComponents().get(0).getContent().getSvnRevision(), is(flowComponent.getContent().getSvnRevision()));

            // And ...
            assertThat(flowStoreServiceConnector.findAllFlows().size(), is(1));

            // And... Assert the version number of the flow has been updated after creation, but only by the first user.
            assertThat(flow.getVersion(), is(flowVersion + 1));
        }
    }

}
