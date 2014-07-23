package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
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
 * Integration tests for the flow components collection part of the flow store service
 */
public class FlowComponentsIT {
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
     * When : valid JSON is POSTed to the flow component path without an identifier
     * Then : a flow component is created and returned
     * And  : assert that the flow component created has an id, a version and contains the same information as the flowComponentContent given as input
     * And  : assert that only one flowComponent can be found in the underlying database
     */
    @Test
    public void createFlowComponent_ok() throws Exception{

        // When...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        // Then...
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        // And...
        assertNotNull(flowComponent);
        assertNotNull(flowComponent.getContent());
        assertNotNull(flowComponent.getId());
        assertNotNull(flowComponent.getVersion());
        assertThat(flowComponent.getContent().getName(), is(flowComponentContent.getName()));
        // And ...
        final List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();
        assertThat(flowComponents.size(), is(1));
    }


    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the components path
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceConstants.FLOW_COMPONENTS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow resource
     * When : adding flow component with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one flow component exist in the underlying database
     */
    @Test
    public void createFlowComponent_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().setName("UniqueName").build();

        try {
            flowStoreServiceConnector.createFlowComponent(flowComponentContent);
            // When...
            flowStoreServiceConnector.createFlowComponent(flowComponentContent);
            fail("Primary key violation was not detected as input to createFlowComponent().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(406));
            // And...
            List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();
            assertThat(flowComponents.size(), is(1));
        }
    }

    /**
     * Given: a deployed flow-store service containing no flow components
     * When: GETing flow components collection
     * Then: request returns with empty list
     */
    @Test
    public void findAllFlowComponents_emptyResult() throws Exception {
        // When...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();

        // Then...
        assertThat(flowComponents, is(notNullValue()));
        assertThat(flowComponents.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three flows
     * When: GETing flows collection
     * Then: request returns with 3 flows
     * And: the flows are sorted alphabetically by name
     */
    @Test
    public void findAllFlowComponents_Ok() throws Exception {
        // Given...
        final FlowComponentContent flowComponentContentA = new FlowComponentContentBuilder().setName("a").build();
        final FlowComponentContent flowComponentContentB = new FlowComponentContentBuilder().setName("b").build();
        final FlowComponentContent flowComponentContentC = new FlowComponentContentBuilder().setName("c").build();

        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        FlowComponent flowComponentSortsFirst = flowStoreServiceConnector.createFlowComponent(flowComponentContentA);
        FlowComponent flowComponentSortsSecond = flowStoreServiceConnector.createFlowComponent(flowComponentContentB);
        FlowComponent flowComponentSortsThird = flowStoreServiceConnector.createFlowComponent(flowComponentContentC);

        // When...
        List<FlowComponent> listOfFlowComponents = flowStoreServiceConnector.findAllFlowComponents();

        // Then...
        assertNotNull(listOfFlowComponents);
        assertFalse(listOfFlowComponents.isEmpty());
        assertThat(listOfFlowComponents.size(), is (3));

        // And...
        assertThat(listOfFlowComponents.get(0).getContent().getName(), is(flowComponentSortsFirst.getContent().getName()));
        assertThat(listOfFlowComponents.get(1).getContent().getName(), is(flowComponentSortsSecond.getContent().getName()));
        assertThat(listOfFlowComponents.get(2).getContent().getName(), is(flowComponentSortsThird.getContent().getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flowComponent path with a valid identifier
     * Then : a flowComponent is found and returned
     * And  : assert that the flowComponent found has an id, a version and contains the same information as the flowComponent created
     */
    @Test
    public void getFlowComponent_ok() throws Exception{

        // When...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);

        // Then...
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        FlowComponent flowComponentToGet = flowStoreServiceConnector.getFlowComponent(flowComponent.getId());

        // And...
        assertNotNull(flowComponentToGet);
        assertNotNull(flowComponentToGet.getContent());
        assertThat(flowComponentToGet.getContent().getName(), is(flowComponent.getContent().getName()));
        assertThat(flowComponentToGet.getContent().getInvocationJavascriptName(), is(flowComponent.getContent().getInvocationJavascriptName()));
        assertThat(flowComponentToGet.getContent().getInvocationMethod(), is(flowComponent.getContent().getInvocationMethod()));
        assertThat(flowComponentToGet.getContent().getSvnProjectForInvocationJavascript(), is(flowComponent.getContent().getSvnProjectForInvocationJavascript()));
        assertThat(flowComponentToGet.getContent().getSvnRevision(), is(flowComponent.getContent().getSvnRevision()));
        assertThat(flowComponentToGet.getVersion(), is (flowComponent.getVersion()));
        assertThat(flowComponentToGet.getContent().getJavascripts().size(), is (flowComponent.getContent().getJavascripts().size()));

        for (int i= 0; i > flowComponentToGet.getContent().getJavascripts().size(); i++){
            JavaScript javaScriptReturned = flowComponentToGet.getContent().getJavascripts().get(i);
            JavaScript javaScriptOriginal = flowComponent.getContent().getJavascripts().get(i);
            assertThat(javaScriptReturned, is (javaScriptOriginal));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow component with an unknown flow component id
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlowComponent_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException{
        try{
            // Given...
            final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
            flowStoreServiceConnector.getFlowComponent(432);

            fail("Invalid request to getFlowComponent() was not detected.");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

}
