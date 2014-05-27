package dk.dbc.dataio.flowstore;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlow;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlowBinder;
import static dk.dbc.dataio.integrationtest.ITUtil.createSink;
import static dk.dbc.dataio.integrationtest.ITUtil.createSubmitter;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the flow binders collection part of the flow store service
 */
public class FlowBindersIT {
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
     * Given: a deployed flow-store service with a flow, a sink and a submitter
     * When: valid JSON is POSTed to the flow binders path referencing the flow, sink and submitter
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createFlowBinder_ok() throws Exception {
        // Given...
        final long flowId = createFlow(restClient, baseUrl,
                new FlowContentJsonBuilder().build());
        final long sinkId = createSink(restClient, baseUrl,
                new SinkContentJsonBuilder().build());
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmitterContentJsonBuilder().build());

        // When...
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .setSinkId(sinkId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final long id = getResourceIdFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOW_BINDERS_TABLE_SELECT_CONTENT_STMT, id);

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowBinderContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: JSON posted to the flow binders path causes JsonException
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createFlowBinder_errorWhenJsonExceptionIsThrown() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with the same name
     * Then: request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlowBinder_duplicateName() throws Exception {
        // Note that we set different destinations to ensure we don't risk matching search keys.

        // Given...
        final long flowId = createFlow(restClient, baseUrl,
                new FlowContentJsonBuilder().build());
        final long sinkId = createSink(restClient, baseUrl,
                new SinkContentJsonBuilder().build());
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmitterContentJsonBuilder().build());

        final String name = "createFlowBinder_duplicateName";
        final String firstFlowBinderContent = new FlowBinderContentJsonBuilder()
                .setName(name)
                .setDestination("base1")
                .setFlowId(flowId)
                .setSinkId(sinkId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        createFlowBinder(restClient, baseUrl, firstFlowBinderContent);

        // When...
        final String secondFlowBinderContent = new FlowBinderContentJsonBuilder()
                .setName(name)
                .setDestination("base2")
                .setFlowId(flowId)
                .setSinkId(sinkId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = HttpClient.doPostWithJson(restClient, secondFlowBinderContent, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: adding flow binder which references non-existing submitter
     * Then: request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void createFlowBinder_referencedSubmitterNotFound() throws Exception {
        // When...
        final long flowId = createFlow(restClient, baseUrl,
                new FlowContentJsonBuilder().build());
        final long sinkId = createSink(restClient, baseUrl,
                new SinkContentJsonBuilder().build());
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .setSubmitterIds(Arrays.asList(123456789L))
                .build();
        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.PRECONDITION_FAILED.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: adding flow binder which references non-existing flow
     * Then: request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void createFlowBinder_referencedFlowNotFound() throws Exception {
        // When...
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmitterContentJsonBuilder().build());
        final long sinkId = createSink(restClient, baseUrl,
                new SinkContentJsonBuilder().build());
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setFlowId(987654321L)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.PRECONDITION_FAILED.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: adding flow binder which references non-existing sink
     * Then: request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void createFlowBinder_referencedSinkNotFound() throws Exception {
        // When...
        final long flowId = createFlow(restClient, baseUrl,
                new FlowContentJsonBuilder().build());
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmitterContentJsonBuilder().build());
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .setSinkId(12121212L)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.PRECONDITION_FAILED.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with different name but matching search key
     * Then: request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlowBinder_searchKeyExistsInSearchIndex() throws Exception {
        // Given...
        final long flowId = createFlow(restClient, baseUrl,
                new FlowContentJsonBuilder().build());
        final long sinkId = createSink(restClient, baseUrl,
                new SinkContentJsonBuilder().build());
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmitterContentJsonBuilder().build());

        String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_1")
                .setFlowId(flowId)
                .setSinkId(sinkId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        createFlowBinder(restClient, baseUrl, flowBinderContent);

        // When...
        flowBinderContent = new FlowBinderContentJsonBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_2")
                .setFlowId(flowId)
                .setSinkId(sinkId)
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = HttpClient.doPostWithJson(restClient, flowBinderContent, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing no flow binders
     * When: GETing flow binders collection
     * Then: request returns with a OK http status code
     * And: request returns with empty list as JSON
     */
    @Test
    public void findAllFlowBinders_emptyResult() throws Exception {
        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));

        // And...
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three flow binders
     * When: GETing flow binders collection
     * Then: request returns with a OK http status code
     * And: request returns with list as JSON of flow binders sorted alphabetically by name
     */
    @Test
    public void findAllFlowBinders_Ok() throws Exception {
        // Given...
        // Create a Flow with name flow1
        String flowContent = new FlowContentJsonBuilder()
            .setName("flow1")
            .build();
        final long flow1Id = createFlow(restClient, baseUrl, flowContent);

        // Create a Sink with name sink1
        String sink = new SinkContentJsonBuilder()
            .setName("sink1")
            .build();
        final long sink1Id = createSink(restClient, baseUrl, sink);

        // Create Submitter with name submitter1
        String submitter = new SubmitterContentJsonBuilder()
            .setName("submitter1")
            .build();
        final long submitter1Id = createSubmitter(restClient, baseUrl, submitter);

        // Create Flowbinders with sortable names
        String flowBinderContent = new FlowBinderContentJsonBuilder()
            .setName("c-flowbinder")
            .setCharset("charset1")
            .setDescription("description1")
            .setDestination("destination1")
            .setFormat("format1")
            .setPackaging("packaging1")
            .setRecordSplitter("recordsplitter1")
            .setFlowId(flow1Id)
            .setSinkId(sink1Id)
            .setSubmitterIds(Arrays.asList(submitter1Id))
            .build();
        final long sortsThird = createFlowBinder(restClient, baseUrl, flowBinderContent);
        flowBinderContent = new FlowBinderContentJsonBuilder()
            .setName("a-flowbinder")
            .setCharset("charset2")
            .setDescription("description2")
            .setDestination("destination2")
            .setFormat("format2")
            .setPackaging("packaging2")
            .setRecordSplitter("recordsplitter2")
            .setFlowId(flow1Id)
            .setSinkId(sink1Id)
            .setSubmitterIds(Arrays.asList(submitter1Id))
            .build();
        final long sortsFirst = createFlowBinder(restClient, baseUrl, flowBinderContent);
        flowBinderContent = new FlowBinderContentJsonBuilder()
            .setName("b-flowbinder")
            .setCharset("charset3")
            .setDescription("description3")
            .setDestination("destination3")
            .setFormat("format3")
            .setPackaging("packaging3")
            .setRecordSplitter("recordsplitter3")
            .setFlowId(flow1Id)
            .setSinkId(sink1Id)
            .setSubmitterIds(Arrays.asList(submitter1Id))
            .build();
        final long sortsSecond = createFlowBinder(restClient, baseUrl, flowBinderContent);

        // When...
        final Response response = HttpClient.doGet(restClient, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

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
