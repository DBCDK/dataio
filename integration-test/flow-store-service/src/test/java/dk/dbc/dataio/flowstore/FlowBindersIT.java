package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for the flow binders collection part of the flow store service
 */
public class FlowBindersIT {
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;
    private static FlowStoreServiceConnector flowStoreServiceConnector;

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

    @Before
    public void setUp() {
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    @After
    public void tearDown() throws SQLException {
        clearAllDbTables(dbConnection);
    }

    /**
     * Given: a deployed flow-store service with a flow, a sink and a submitter
     * When: valid JSON is POSTed to the flow binders path referencing the flow, sink and submitter
     * Then : a flow binder is created and returned
     * And  : The flow binder created has an id, a version and contains the same information as the flow binder content given as input
     * And  : assert that only one flow binder can be found in the underlying database
     */
    @Test
    public void createFlowBinder_ok() throws Exception{
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        // When...
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();

        // Then...
        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        // And ...
        assertThat(flowBinder, not(nullValue()));
        assertThat(flowBinder.getContent(), not(nullValue()));
        assertThat(flowBinder.getId(), not(nullValue()));
        assertThat(flowBinder.getVersion(), not(nullValue()));
        assertThat(flowBinder.getContent().getName(), is(flowBinderContent.getName()));
        assertThat(flowBinder.getContent().getDescription(), is(flowBinderContent.getDescription()));

        // And ...
        final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
        assertThat(flowBinders.size(), is(1));
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
     * Given: a deployed flow-store service containing flow binder resource and with a flow, a sink and a submitter
     * When : adding flow binder with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one flow binder exist in the underlying database
     */
    @Test
    public void createFlowBinder_duplicateName_notAcceptable() throws FlowStoreServiceConnectorException {
        // Note that we set different destinations to ensure we don't risk matching search keys.

        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        final FlowBinderContent validFlowBinderContent = new FlowBinderContentBuilder()
                .setName("UniqueName")
                .setDestination("base1")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();

        final FlowBinderContent duplicateFlowBinderContent = new FlowBinderContentBuilder()
                .setName("UniqueName")
                .setDestination("base2")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();
        try {
            flowStoreServiceConnector.createFlowBinder(validFlowBinderContent);
            // When...
            flowStoreServiceConnector.createFlowBinder(duplicateFlowBinderContent);
            fail("Primary key violation was not detected as input to createFlowBinder().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(406));
            // And...
            List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders.size(), is(1));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : adding flow binder which references non-existing submitter
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     * And  : assert that no flow binder have been created in the underlying database
     */
    @Test
    public void createFlowBinder_referencedSubmitterNotFound_preconditionFailed() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(77373736L))
                .build();
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(flowBinderContent);
            fail("Failed pre-condition was not detected as input to createFlowBinder().");
        // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(412));
            // And...
            List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : adding flow binder which references non-existing flow
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     * And  : assert that no flow binder have been created in the underlying database
     */
    @Test
    public void createFlowBinder_referencedFlowNotFound_preconditionFailed() throws Exception {
        // Given...
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(77373736)
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(flowBinderContent);
            fail("Failed pre-condition was not detected as input to createFlowBinder().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(412));
            // And...
            List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : adding flow binder which references non-existing sink
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     * And  : assert that no flow binder have been created in the underlying database
     */
    @Test
    public void createFlowBinder_referencedSinkNotFound_preconditionFailed() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(77373736)
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(flowBinderContent);
            fail("Failed pre-condition was not detected as input to createFlowBinder().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(412));
            // And...
            List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with different name but matching search key
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one flow binder exist in the underlying database
     */
    @Test
    public void createFlowBinder_searchKeyExistsInSearchIndex_notAcceptable() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        FlowBinderContent validFlowBinderContent = new FlowBinderContentBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_1")
                .setDestination("matchingSearchKey")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();

        FlowBinderContent notAcceptableFlowBinderContent = new FlowBinderContentBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_2")
                .setDestination("matchingSearchKey")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();

       try {
           flowStoreServiceConnector.createFlowBinder(validFlowBinderContent);
           // When...
           flowStoreServiceConnector.createFlowBinder(notAcceptableFlowBinderContent);
           fail("Unique constraint violation was not detected as input to createFlowBinder().");
           // Then...
       } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
           // And...
           assertThat(e.getStatusCode(), is(406));
           // And...
           List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
           assertThat(flowBinders.size(), is(1));
       }
    }

    /**
     * Given: a deployed flow-store service containing no flow binders
     * When: GETing flow binders collection
     * Then: request returns with empty list
     */
    @Test
    public void findAllFlowBinders_emptyResult() throws Exception {
        // When...
        final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();

        // Then...
        assertThat(flowBinders, is(notNullValue()));
        assertThat(flowBinders.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service containing three flow binders
     * When: GETing flow binders collection
     * Then: request returns with 3 flow binders
     * And: the flow binders are sorted alphabetically by name
     */
    @Test
    public void findAllFlowBinders_Ok() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        final FlowBinder flowBinderSortsThird = createFlowBinder("c-flowbinder", "destination1", flow.getId(), sink.getId(), Arrays.asList(submitter.getId()));
        final FlowBinder flowBinderSortsFirst = createFlowBinder("a-flowbinder", "destination2", flow.getId(), sink.getId(), Arrays.asList(submitter.getId()));
        final FlowBinder flowBinderSortsSecond = createFlowBinder("b-flowbinder", "destination3", flow.getId(), sink.getId(), Arrays.asList(submitter.getId()));

        // When...
        List<FlowBinder> listOfFlowBinders = flowStoreServiceConnector.findAllFlowBinders();

        // Then...
        assertThat(listOfFlowBinders, not(nullValue()));
        assertThat(listOfFlowBinders.size(), is(3));

        // And...
        assertThat(listOfFlowBinders.get(0).getContent().getName(), is(flowBinderSortsFirst.getContent().getName()));
        assertThat(listOfFlowBinders.get(1).getContent().getName(), is(flowBinderSortsSecond.getContent().getName()));
        assertThat(listOfFlowBinders.get(2).getContent().getName(), is(flowBinderSortsThird.getContent().getName()));
    }

    /**
     * Given: a deployed flow-store service containing one flow binder
     * When: GETing an existing flow binder
     * Then: request returns with 1 flow binder (the correct one)
     */
    @Test
    public void getFlowBinder_Ok() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());
        final String FLOW_BINDER_NAME = "The Flowbinder";
        final String FLOW_BINDER_DESTINATION = "The Destination";
        final FlowBinder originalFlowBinder = createFlowBinder(FLOW_BINDER_NAME, FLOW_BINDER_DESTINATION, flow.getId(), sink.getId(), Arrays.asList(submitter.getId()));
        final long flowBinderId = originalFlowBinder.getId();

        // When...
        FlowBinder flowBinder = flowStoreServiceConnector.getFlowBinder(flowBinderId);

        // Then...
        assertThat(flowBinder.getId(), is(flowBinderId));
        assertThat(flowBinder.getContent().getName(), is(FLOW_BINDER_NAME));
        assertThat(flowBinder.getContent().getDestination(), is(FLOW_BINDER_DESTINATION));
        assertThat(flowBinder.getContent().getFlowId(), is(flow.getId()));
        assertThat(flowBinder.getContent().getSinkId(), is(sink.getId()));
        assertTrue(flowBinder.getContent().getSubmitterIds().contains(submitter.getId()));
    }

    /**
     * Given: a deployed flow-store service containing one flow binder
     * When: GETing an existing flow binder
     * Then: request returns with 1 flow binder (the correct one)
     */
    @Test
    public void getFlowBinder_notFound_throws() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());
        final String FLOW_BINDER_NAME = "The Flowbinder";
        final String FLOW_BINDER_DESTINATION = "The Destination";
        final FlowBinder originalFlowBinder = createFlowBinder(FLOW_BINDER_NAME, FLOW_BINDER_DESTINATION, flow.getId(), sink.getId(), Arrays.asList(submitter.getId()));
        final long flowBinderId = originalFlowBinder.getId();

        try {
            // When...
            FlowBinder flowBinder = flowStoreServiceConnector.getFlowBinder(flowBinderId+1);  // Then we don't get the created FlowBinder
            fail("It seems as if we do get a FlowBinder, though we didn't expect one!");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(404));
        }

    }


    // Private methods

    private FlowBinder createFlowBinder(String name, String destination, long flowId, long sinkId, List<Long> submitterIds) throws Exception {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(name)
                .setDestination(destination)
                .setFlowId(flowId)
                .setSinkId(sinkId)
                .setSubmitterIds(submitterIds)
                .build();
        return flowStoreServiceConnector.createFlowBinder(flowBinderContent);
    }

}
