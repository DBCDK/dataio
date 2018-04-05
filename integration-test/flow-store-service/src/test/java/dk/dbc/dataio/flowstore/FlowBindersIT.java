/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowStoreError;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import net.jodah.failsafe.RetryPolicy;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newIntegrationTestConnection;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for the flow binders collection part of the flow store service
 *
 * TODO: Rethink Flowstore integrationtest to avoid id number clashes due to JPA caching
 *
 */
public class FlowBindersIT {
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;
    private static FlowStoreServiceConnector flowStoreServiceConnector;

    private final static String FLOW_BINDER_UPDATED_NAME = "FlowBinderUpdatedName";
    private final static String FLOW_BINDER_UPDATED_DESTINATION = "FlowBinderUpdatedDestination";
    private final static String FLOW_BINDER_ORIGINAL_NAME = "FlowBinderOriginalName";
    private final static String FLOW_BINDER_ORIGINAL_DESTINATION = "FlowBinderOriginalDestination";
    private final static String FLOW_BINDER_PACKAGING_XML = "xml";
    private final static String FLOW_BINDER_PACKAGING_TEXT = "text";
    private final static long NONE_EXISTING_ID = 987363;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        restClient = HttpClient.newClient();
        dbConnection = newIntegrationTestConnection("flowstore");
        flowStoreServiceConnector = new FlowStoreServiceConnector(
            FailSafeHttpClient.create(restClient, new RetryPolicy().withMaxRetries(0)),
            baseUrl);
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
     * Then : a flow binder is created and returned
     * And  : The flow binder created has an id, a version and contains the same information as the flow binder content given as input
     * And  : assert that only one flow binder can be found in the underlying database
     * And  : assert that the same flow binder can be located through search indexes
     * And  : assert that the database table: flow_binders_submitters have been updated correctly
     */
    @Test
    public void createFlowBinder_ok() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        // When...
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        // Then...
        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        // And ...
        assertFlowBinderNotNull(flowBinder);
        assertThat(flowBinder.getContent(), is(flowBinderContent));

        // And ...
        final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
        assertThat(flowBinders.size(), is(1));

        // And...
        assertSearchIndexEquals(flowBinder, submitter.getContent().getNumber());

        // And...
        assertFlowBindersSubmitters(flowBinder.getId(), flowBinder.getContent().getSubmitterIds());
    }

    /**
     * Given: a deployed flow-store service
     * When: JSON posted to the flow binders path causes JSONBException
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
     * And  : assert that only the same flow binder can be located through search indexes
     * And  : assert that the database table: flow_binders_submitters have been updated correctly
     */
    @Test
    public void createFlowBinder_duplicateName_notAcceptable() throws FlowStoreServiceConnectorException, SQLException {
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
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        final FlowBinderContent duplicateFlowBinderContent = new FlowBinderContentBuilder()
                .setName("UniqueName")
                .setDestination("base2")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(validFlowBinderContent);
        try {
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

            // And...
            assertSearchIndexEquals(flowBinder, submitter.getContent().getNumber());
            assertSearchIndexDoesNotExist(duplicateFlowBinderContent, submitter.getContent().getNumber());

            // And...
            assertFlowBindersSubmitters(flowBinder.getId(), flowBinder.getContent().getSubmitterIds());
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
                .setSubmitterIds(Collections.singletonList(77373736L))
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
     * And  : assert that no search index has been created
     */
    @Test
    public void createFlowBinder_referencedFlowNotFound_preconditionFailed() throws Exception {
        // Given...
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(77373736)
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
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

            // And...
            assertSearchIndexDoesNotExist(flowBinderContent, submitter.getContent().getNumber());
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : adding flow binder which references non-existing sink
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     * And  : assert that no flow binder have been created in the underlying database
     * And  : assert that no search index has been created
     */
    @Test
    public void createFlowBinder_referencedSinkNotFound_preconditionFailed() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(77373736)
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
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

            // And...
            assertSearchIndexDoesNotExist(flowBinderContent, submitter.getContent().getNumber());
        }
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with different name but matching search key
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one flow binder exist in the underlying database
     * And  : assert that only the same flow binder can be located through search indexes
     * And  : assert that the database table: flow_binders_submitters have been updated correctly
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
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        FlowBinderContent notAcceptableFlowBinderContent = new FlowBinderContentBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_2")
                .setDestination("matchingSearchKey")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(validFlowBinderContent);
        try {

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

            // And...
            assertSearchIndexEquals(flowBinder, submitter.getContent().getNumber());

            // And...
            assertFlowBindersSubmitters(flowBinder.getId(), flowBinder.getContent().getSubmitterIds());
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

        final FlowBinder flowBinderSortsThird = createFlowBinder("c-flowbinder", "destination1", flow.getId(), sink.getId(), Collections.singletonList(submitter.getId()));
        final FlowBinder flowBinderSortsFirst = createFlowBinder("a-flowbinder", "destination2", flow.getId(), sink.getId(), Collections.singletonList(submitter.getId()));
        final FlowBinder flowBinderSortsSecond = createFlowBinder("b-flowbinder", "destination3", flow.getId(), sink.getId(), Collections.singletonList(submitter.getId()));

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
        final FlowBinder originalFlowBinder = createFlowBinder(FLOW_BINDER_NAME, FLOW_BINDER_DESTINATION, flow.getId(), sink.getId(), Collections.singletonList(submitter.getId()));
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
     * When: GETing a non existing flow binder
     * Then: an exception is thrown, and the status code is 404
     */
    @Test
    public void getFlowBinder_notFound_throws() throws Exception {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());
        final String FLOW_BINDER_NAME = "The Flowbinder";
        final String FLOW_BINDER_DESTINATION = "The Destination";
        final FlowBinder originalFlowBinder = createFlowBinder(FLOW_BINDER_NAME, FLOW_BINDER_DESTINATION, flow.getId(), sink.getId(), Collections.singletonList(submitter.getId()));
        final long flowBinderId = originalFlowBinder.getId();

        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(flowBinderId + 1);  // Then we don't get the created FlowBinder
            fail("It seems as if we do get a FlowBinder, though we didn't expect one!");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(404));
        }

    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow binder with given id is already stored
     * When : valid JSON is POSTed to the flow binders path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the flow binder has not changed
     * And  : assert that the version number has been updated
     * And  : assert that updated data can be found in the underlying database and only one flow binder exists
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void updateFlowBinder_ok() throws Exception {
        // Given ...
        final FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        final Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));
        final Submitter submitterForUpdateA
                = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().setName("A").setNumber(2L).build());

        final Submitter submitterForUpdateB
                = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().setName("B").setNumber(3L).build());

        List<Long> submitterIds = new ArrayList<>(2);
        submitterIds.add(submitterForUpdateA.getId());
        submitterIds.add(submitterForUpdateB.getId());

        // Assert that new rows have been created in flow_binders_search_index and in flow_binders_submitters (packaging = xml)
        assertSearchIndexEquals(flowBinder, submitter.getContent().getNumber());
        assertFlowBindersSubmitters(flowBinder.getId(), flowBinder.getContent().getSubmitterIds());

        final FlowBinderContent newFlowBinderContent =
                getFlowBinderContentForUpdate(flowBinder.getContent().getFlowId(), flowBinder.getContent().getSinkId(), submitterIds);

        // When...
        FlowBinder updatedFlowBinder =
                flowStoreServiceConnector.updateFlowBinder(newFlowBinderContent, flowBinder.getId(), flowBinder.getVersion());

        // Then...
        assertFlowBinderNotNull(updatedFlowBinder);
        assertThat(updatedFlowBinder.getContent(), is(newFlowBinderContent));
        assertFlowBinderEquals(updatedFlowBinder, flowBinder, 1);

        // And...
        final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
        assertThat(flowBinders.size(), is(1));

        // And...
        // Assert that the rows created for the "old" flow binder (packaging = xml) has been removed from the database
        assertSearchIndexDoesNotExist(flowBinder.getContent(), submitter.getContent().getNumber());

        // Assert that new rows have been created for the updated flow binder (packaging = text)
        assertSearchIndexEquals(updatedFlowBinder, submitterForUpdateA.getContent().getNumber());
        assertSearchIndexEquals(updatedFlowBinder, submitterForUpdateB.getContent().getNumber());

        // Assert that the rows in flow_binders_submitters have been updated correctly
        assertFlowBindersSubmitters(flowBinder.getId(), updatedFlowBinder.getContent().getSubmitterIds());
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the flow binders path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateFlowBinder_invalidJson_BadRequest() throws FlowStoreServiceConnectorException{
        // Given ...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(flowBinder.getVersion())); // Set version = flow binder version
        final Response response = HttpClient.doPostWithJson(restClient, headers, "<invalid json />", baseUrl,
                FlowStoreServiceConstants.FLOW_BINDERS, Long.toString(flowBinder.getId()), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flow binder exists in the underlying database
     */
    @Test
    public void updateFlowBinder_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try{
            // When...
            final FlowBinderContent newFlowBinderContent = new FlowBinderContentBuilder().build();
            flowStoreServiceConnector.updateFlowBinder(newFlowBinderContent, 73528, 1L);

            fail("Wrong flow binder Id was not detected as input to updateFlowBinder().");

            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(404));

            // And...
            final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders, not(nullValue()));
            assertThat(flowBinders.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : Two valid flow binders are already stored
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but with a name that is already
     *        in use by one of the existing flow binders
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     * And  : assert that two flow binders exists in the underlying database
     * And  : updated data cannot be found in the underlying database
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void updateFlowBinder_duplicateName_NotAcceptable() throws SQLException, FlowStoreServiceConnectorException{

        final String SECOND_FLOW_BINDER_NAME = "SecondFlowBinderName";
        final String SECOND_FLOW_BINDER_DESTINATION = "SecondFlowBinderDestination";

        FlowBinder flowBinderA = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinderA.getContent().getSubmitterIds().get(0));

        final FlowBinderContent flowBinderContent2 = new FlowBinderContentBuilder()
                .setName(SECOND_FLOW_BINDER_NAME)
                .setDestination(SECOND_FLOW_BINDER_DESTINATION)
                .setPackaging(FLOW_BINDER_PACKAGING_XML)
                .setFlowId(flowBinderA.getContent().getFlowId())
                .setSinkId(flowBinderA.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        FlowBinder flowBinderB = flowStoreServiceConnector.createFlowBinder(flowBinderContent2);

        final FlowBinderContent invalidFlowBinderContent = new FlowBinderContentBuilder()
                .setName(flowBinderA.getContent().getName())
                .setDestination(FLOW_BINDER_UPDATED_DESTINATION)
                .setPackaging(FLOW_BINDER_PACKAGING_TEXT)
                .setFlowId(flowBinderA.getContent().getFlowId())
                .setSinkId(flowBinderA.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();
        try {
            // When... (Attempting to save the second flow binder created with the same name as the first flow binder created)
            flowStoreServiceConnector.updateFlowBinder(invalidFlowBinderContent, flowBinderB.getId(), flowBinderB.getVersion());

            fail("Primary key violation was not detected as input to updateFlowBinder().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(406));

            // And...
            final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders, not(nullValue()));
            assertThat(flowBinders.size(), is(2));

            // And...
            assertThat(flowBinders.get(0).getContent().getName(), is (FLOW_BINDER_ORIGINAL_NAME));
            assertThat(flowBinders.get(0).getContent().getDestination(), is (FLOW_BINDER_ORIGINAL_DESTINATION));
            assertThat(flowBinders.get(1).getContent().getName(), is (SECOND_FLOW_BINDER_NAME));
            assertThat(flowBinders.get(1).getContent().getDestination(), is(SECOND_FLOW_BINDER_DESTINATION));

            // And...
            assertSearchIndexEquals(flowBinderA, submitter.getContent().getNumber());
            assertSearchIndexEquals(flowBinderB, submitter.getContent().getNumber());
            assertSearchIndexDoesNotExist(invalidFlowBinderContent, submitter.getContent().getNumber());

            assertFlowBindersSubmitters(flowBinderA.getId(), flowBinderA.getContent().getSubmitterIds());
            assertFlowBindersSubmitters(flowBinderB.getId(), flowBinderB.getContent().getSubmitterIds());
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow binder with given id is already stored and the flow binder is opened for edit by two different users
     * And  : the first user updates the flow binder, valid JSON is POSTed to the flow binders path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the flow binder, valid JSON is POSTed to the flow binders
     *        path with an identifier (update) and wrong version number

     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     * And  : assert that only one flow binder exists in the underlying database
     * And  : assert that updated data from the first user can be found in the underlying database
     * And  : assert that the version number has been updated only by the first user
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void updateFlowBinder_wrongVersion_Conflict() throws FlowStoreServiceConnectorException, SQLException {
        // Given...
        final String FLOW_BINDER_NAME_FROM_FIRST_USER = "UpdatedFlowBinderNameFromFirstUser";
        final String FLOW_BINDER_NAME_FROM_SECOND_USER = "UpdatedFlowBinderNameFromSecondUser";

        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        final FlowBinderContent flowBinderContentFirstUser = new FlowBinderContentBuilder()
                .setName(FLOW_BINDER_NAME_FROM_FIRST_USER)
                .setPackaging(FLOW_BINDER_PACKAGING_TEXT)
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        final FlowBinderContent flowBinderContentSecondUser = new FlowBinderContentBuilder()
                .setName(FLOW_BINDER_NAME_FROM_SECOND_USER)
                .setPackaging(FLOW_BINDER_PACKAGING_XML)
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        // And... First user updates the flow binder
        FlowBinder updatedFlowBinder =
                flowStoreServiceConnector.updateFlowBinder(
                        flowBinderContentFirstUser,
                        flowBinder.getId(),
                        flowBinder.getVersion());

        try {
            // When... Second user attempts to update the same flow binder
            flowStoreServiceConnector.updateFlowBinder(flowBinderContentSecondUser, flowBinder.getId(), flowBinder.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateFlowBinder().");

            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(409));

            // And...
            final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders, not(nullValue()));
            assertThat(flowBinders.size(), is(1));
            assertThat(flowBinders.get(0).getContent().getName(), is(FLOW_BINDER_NAME_FROM_FIRST_USER));

            // And... Assert the version number has been updated after creation, but only by the first user.
            assertThat(flowBinders.get(0).getVersion(), is(flowBinder.getVersion() +1));

            // And... Assert that database tables: flow_binders_search_index and flow_binders_submitters have been updated correctly
            assertSearchIndexDoesNotExist(flowBinder.getContent(), submitter.getContent().getNumber());
            assertSearchIndexDoesNotExist(flowBinderContentSecondUser, submitter.getContent().getNumber());
            assertSearchIndexEquals(updatedFlowBinder, submitter.getContent().getNumber());
            assertFlowBindersSubmitters(updatedFlowBinder.getId(), updatedFlowBinder.getContent().getSubmitterIds());
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but the referenced
     *        flow could not be located in the underlying database
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     * And  : assert that only one flow binder exist in the underlying database
     * And  : assert that only the original data from creation can be found in the underlying database
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void updateFlowBinder_referencedFlowNotFound_ReferencedEntityNotFoundException() throws FlowStoreServiceConnectorException, SQLException {
        FlowBinder createdFlowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(createdFlowBinder.getContent().getSubmitterIds().get(0));
        FlowBinderContent flowBinderContentForUpdate = getFlowBinderContentForUpdate(
                NONE_EXISTING_ID,
                createdFlowBinder.getContent().getSinkId(),
                Collections.singletonList(submitter.getId()));

        assertReferencedEntityNotFoundException(flowBinderContentForUpdate, createdFlowBinder, submitter.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but the referenced
     *        sink could not be located in the underlying database
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     * And  : assert that only one flow binder exist in the underlying database
     * And  : assert that only the original data from creation can be found in the underlying database
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void updateFlowBinder_referencedSinkNotFound_ReferencedEntityNotFoundException() throws FlowStoreServiceConnectorException, SQLException {
        FlowBinder createdFlowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(createdFlowBinder.getContent().getSubmitterIds().get(0));
        FlowBinderContent flowBinderContentForUpdate = getFlowBinderContentForUpdate(
                createdFlowBinder.getContent().getFlowId(),
                NONE_EXISTING_ID,
                Collections.singletonList(submitter.getId()));

        assertReferencedEntityNotFoundException(flowBinderContentForUpdate, createdFlowBinder, submitter.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but the referenced
     *        submitter could not be located in the underlying database
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     * And  : assert that only one flow binder exist in the underlying database
     * And  : assert that only the original data from creation can be found in the underlying database
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void updateFlowBinder_referencedSubmitterNotFound_ReferencedEntityNotFoundException() throws FlowStoreServiceConnectorException, SQLException {
        // Given...
        FlowBinder createdFlowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(createdFlowBinder.getContent().getSubmitterIds().get(0));
        FlowBinderContent flowBinderContentForUpdate = getFlowBinderContentForUpdate(
                createdFlowBinder.getContent().getFlowId(),
                createdFlowBinder.getContent().getSinkId(),
                Collections.singletonList(NONE_EXISTING_ID));

        // When...
        assertReferencedEntityNotFoundException(flowBinderContentForUpdate, createdFlowBinder, submitter.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service and a flow binder is stored and belonging search-index and entry in flow_binders_submitters exists
     * When : attempting to delete the flow binder
     * Then : the flow binder is deleted
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void deleteFlowBinder_Ok() throws FlowStoreServiceConnectorException, SQLException {
        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        // When...
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        // Then...
        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        //Assert that the flow binder can be located through an existing search index
        assertSearchIndexEquals(flowBinder, submitter.getContent().getNumber());

        // Assert that the expected rows exist in the database table: flow_binders_submitters
        assertFlowBindersSubmitters(flowBinder.getId(), flowBinder.getContent().getSubmitterIds());

        // When...
        flowStoreServiceConnector.deleteFlowBinder(flowBinder.getId(), flowBinder.getVersion());

        // Then... Verify that the flow binder is deleted
        try {
            flowStoreServiceConnector.getFlowBinder(flowBinder.getId());
            fail("FlowBinder was not deleted");
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {

            // We expect this exception from getFlowBinder(...) method when no flow binder exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));

            // And...
            // Assert that the search index rows created for the flow binder has been removed from the database
            assertSearchIndexDoesNotExist(flowBinder.getContent(), submitter.getContent().getNumber());

            // Assert that the rows in flow_binders_submitters have been removed
            assertFlowBindersSubmitters(flowBinder.getId(), new ArrayList<>());
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to delete a flow binder that does not exist
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void deleteFlowBinder_NoFlowBinderToDelete() throws ProcessingException {

        // Given...
        final long flowBinderIdNotExists = 9999;
        final long versionNotExists = 9;

        try {
            // When...
            flowStoreServiceConnector.deleteFlowBinder(flowBinderIdNotExists, versionNotExists);
            fail("None existing flow binder was not detected");

            // Then ...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service and a flow binder is stored.
     * And  : the flow binder is updated and, valid JSON is POSTed to the flow binders path with an identifier (update)
     *        and correct version number
     * When : attempting to delete the flow binder with the previous version number, valid JSON is POSTed to the flow binders
     *        path with an identifier (delete) and wrong version number

     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     * And  : assert that database tables: flow_binders_search_index and flow_binders_submitters have been
     *        updated correctly
     */
    @Test
    public void deleteFlowBinder_OptimisticLocking() throws FlowStoreServiceConnectorException, SQLException {

        // Given...
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        // When...
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        // Then...
        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(flowBinderContent);
        long flowBinderId = flowBinder.getId();
        long versionFirst = flowBinder.getVersion();
        long versionSecond = versionFirst + 1;

        final FlowBinderContent flowBinderContentFirstUser = new FlowBinderContentBuilder()
                .setName("UpdatedFlowBinderName")
                .setPackaging(FLOW_BINDER_PACKAGING_TEXT)
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        // And
        final FlowBinder flowBinderUpdated = flowStoreServiceConnector.updateFlowBinder(flowBinderContentFirstUser, flowBinderId, versionFirst);
        assertThat(flowBinderUpdated.getVersion(), is(versionSecond));

        // Verify before delete
        FlowBinder flowBinderBeforeDelete = flowStoreServiceConnector.getFlowBinder(flowBinderId);
        assertThat(flowBinderBeforeDelete.getId(), is(flowBinderId));
        assertThat(flowBinderBeforeDelete.getVersion(), is(versionSecond));

        try {
            // When...
            flowStoreServiceConnector.deleteFlowBinder(flowBinderId, versionFirst);
            fail("FlowBinder was deleted");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));

            // And...
            assertSearchIndexDoesNotExist(flowBinder.getContent(), submitter.getContent().getNumber());
            assertSearchIndexEquals(flowBinderUpdated, submitter.getContent().getNumber());
            assertFlowBindersSubmitters(flowBinderUpdated.getId(), flowBinderUpdated.getContent().getSubmitterIds());
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with a valid identifier a flow binder is found and returned
     * Then : assert that the flow binder found has an id, a version and contains the same information as the flow binder created
     */
    @Test
    public void getFlowBinderBySearchIndex_ok() throws Exception{
        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();

        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        // When...
        FlowBinder flowBinderToGet = flowStoreServiceConnector.getFlowBinder(
                flowBinder.getContent().getPackaging(),
                flowBinder.getContent().getFormat(),
                flowBinder.getContent().getCharset(),
                submitter.getContent().getNumber(),
                flowBinder.getContent().getDestination());

        // Then...
        assertFlowBinderNotNull(flowBinder);
        assertThat(flowBinderToGet, is(flowBinder));
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow binder but with wrong packaging given as input
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlowBinder_wrongPackagingNotFound_throws() throws FlowStoreServiceConnectorException{
        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));
        assertNotFoundException(
                FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
                "invalidPackaging",
                flowBinder.getContent().getFormat(),
                flowBinder.getContent().getCharset(),
                submitter.getContent().getNumber(),
                flowBinder.getContent().getDestination());
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow binder but with wrong format given as input
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlowBinder_wrongFormatNotFound_throws() throws FlowStoreServiceConnectorException{
        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));
        assertNotFoundException(
                FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
                flowBinder.getContent().getPackaging(),
                "invalidFormat",
                flowBinder.getContent().getCharset(),
                submitter.getContent().getNumber(),
                flowBinder.getContent().getDestination());
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow binder but with wrong charset given as input
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlowBinder_wrongCharsetNotFound_throws() throws FlowStoreServiceConnectorException{
        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));
        assertNotFoundException(
                FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
                flowBinder.getContent().getPackaging(),
                flowBinder.getContent().getFormat(),
                "invalidCharset",
                submitter.getContent().getNumber(),
                flowBinder.getContent().getDestination());
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow binder but with an unknown submitter number given as input
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlowBinder_wrongSubmitterNumberNotFound_throws() throws FlowStoreServiceConnectorException{
        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        assertNotFoundException(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                flowBinder.getContent().getPackaging(),
                flowBinder.getContent().getFormat(),
                flowBinder.getContent().getCharset(),
                2777,
                flowBinder.getContent().getDestination());
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow binder but with wrong destination given as input
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlowBinder_wrongDestinationNotFound_throws() throws FlowStoreServiceConnectorException{
        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects();
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));
        assertNotFoundException(
                FlowStoreError.Code.EXISTING_SUBMITTER_NONEXISTING_DESTINATION,
                flowBinder.getContent().getPackaging(),
                flowBinder.getContent().getFormat(),
                flowBinder.getContent().getCharset(),
                submitter.getContent().getNumber(),
                "invalidDestination");
    }

    /*
     Private methods
     */

    /**
     * This method attempts to locate a none existing flow binder search index
     *
     * @param flowBinderContent holding values used to locate search index
     * @param submitterNumber of the submitter referenced by the flow binder
     *
     * @throws FlowStoreServiceConnectorException
     */
    private void assertSearchIndexDoesNotExist(FlowBinderContent flowBinderContent, long submitterNumber) throws FlowStoreServiceConnectorException {
        try {
            flowStoreServiceConnector.getFlowBinder(
                    flowBinderContent.getPackaging(),
                    flowBinderContent.getFormat(),
                    flowBinderContent.getCharset(),
                    submitterNumber,
                    flowBinderContent.getDestination());

            fail("search index found for none existing flow binder");
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e1) {
            assertThat(e1.getStatusCode(), is(404));
        }
    }

    /**
     * Retrieves a flow binder through search index.
     * Asserts that the flow binder returned is the same as expected
     *
     * @param flowBinder the existing flow binder
     * @param submitterNumber of the submitter referenced by the flow binder
     *
     * @throws FlowStoreServiceConnectorException
     */
    private void assertSearchIndexEquals(FlowBinder flowBinder, long submitterNumber) throws FlowStoreServiceConnectorException {
        FlowBinder flowBinderThroughSearchIndex =
                flowStoreServiceConnector.getFlowBinder(
                        flowBinder.getContent().getPackaging(),
                        flowBinder.getContent().getFormat(),
                        flowBinder.getContent().getCharset(),
                        submitterNumber,
                        flowBinder.getContent().getDestination());

        assertFlowBinderNotNull(flowBinderThroughSearchIndex);
        assertFlowBinderEquals(flowBinder, flowBinderThroughSearchIndex, 0);
    }

    /**
     * This method attempts to update an existing flow binder with content that does not reference either
     * a flow, a sink or a submitter
     *
     * @param flowBinderContentForUpdate the flow binder content to be used for update
     * @param flowBinder the existing flow binder
     * @throws SQLException
     * @throws FlowStoreServiceConnectorException
     */
    private void assertReferencedEntityNotFoundException(FlowBinderContent flowBinderContentForUpdate, FlowBinder flowBinder, long submitterNumber) throws SQLException, FlowStoreServiceConnectorException{
        try {
            flowStoreServiceConnector.updateFlowBinder(flowBinderContentForUpdate, flowBinder.getId(), flowBinder.getVersion());
            fail("None existing reference was not detected as input to updateFlowBinder().");

            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(412));

            // And...
            final List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            assertThat(flowBinders, not(nullValue()));
            assertThat(flowBinders.size(), is(1));

            // And... Assert the flow binder has not been updated
            assertThat(flowBinders.get(0).getVersion(), is(flowBinder.getVersion()));
            assertThat(flowBinders.get(0).getContent().getName(), is(FLOW_BINDER_ORIGINAL_NAME));
            assertThat(flowBinders.get(0).getContent().getDestination(), is(FLOW_BINDER_ORIGINAL_DESTINATION));

            // And... assert that the tables: flow_binder_search_index and flow_binders_submitters have not been updated
            assertSearchIndexDoesNotExist(flowBinderContentForUpdate, submitterNumber);
            assertSearchIndexEquals(flowBinder, submitterNumber);
            assertFlowBindersSubmitters(flowBinder.getId(), flowBinder.getContent().getSubmitterIds());
        }
    }

    /**
     * This method attempts to retrieve a flow binder through search indexes, but with an invalid argument.
     *
     * @param packaging of the flow binder
     * @param format of the flow binder
     * @param charset of the flow binder
     * @param submitterNumber of the submitter referenced by the flow binder
     * @param destination of the flow binder
     * @throws FlowStoreServiceConnectorException NOT FOUND
     */
    private void assertNotFoundException(FlowStoreError.Code code, String packaging, String format, String charset, long submitterNumber, String destination) throws FlowStoreServiceConnectorException{
        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(packaging, format, charset, submitterNumber, destination);
            fail("Invalid request to getFlowBinder() was not detected.");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
            assertThat(e.getFlowStoreError(), not(nullValue()));
            assertThat(e.getFlowStoreError().getCode(), is(code));
        }
    }

    /**
     * Creates a new flow binder with pre-defined values. The referenced sink, flow and submitter is also created
     * @return the created flow binder
     * @throws FlowStoreServiceConnectorException
     */
    private FlowBinder createFlowBinderWithReferencedObjects() throws FlowStoreServiceConnectorException{
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        FlowBinderContent flowBinderContent =  new FlowBinderContentBuilder()
                .setName(FLOW_BINDER_ORIGINAL_NAME)
                .setDestination(FLOW_BINDER_ORIGINAL_DESTINATION)
                .setPackaging(FLOW_BINDER_PACKAGING_XML)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        return flowStoreServiceConnector.createFlowBinder(flowBinderContent);
    }

    /**
     * Private method creating a new flow binder content with the ids for flow, sink and submitters given
     * as input
     *
     * @param flowId to set on the flow binder content
     * @param sinkId to set on the flow binder content
     * @param submitterIds to set on the flow binder content
     * @return a flowBinderContent containing the input values
     */
    private FlowBinderContent getFlowBinderContentForUpdate(long flowId, long sinkId, List<Long> submitterIds) {
        return new FlowBinderContentBuilder()
                .setName(FLOW_BINDER_UPDATED_NAME)
                .setDestination(FLOW_BINDER_UPDATED_DESTINATION)
                .setPackaging(FLOW_BINDER_PACKAGING_TEXT)
                .setFlowId(flowId)
                .setSinkId(sinkId)
                .setSubmitterIds(submitterIds)
                .build();
    }

    /**
     * This method validates that the expected rows exist in the database table: flow_binders_submitters
     *
     * @param flowBinderId of the updated flow binder
     * @param submitterIds the new submitterIds referenced by the flow binder
     * @throws SQLException if executing the query fails
     */
    private void assertFlowBindersSubmitters(long flowBinderId, List<Long> submitterIds) throws SQLException {
        final String selectSQL = String.format(
                "SELECT submitter_id " +
                        "FROM %s " +
                        "WHERE flow_binder_id = ?"
                , ITUtil.FLOW_BINDERS_SUBMITTER_JOIN_TABLE_NAME);

        PreparedStatement preparedStatement = dbConnection.prepareStatement(selectSQL);
        final String TABLE_FLOW_BINDERS_SUBMITTERS_SUBMITTER_ID = "submitter_id";
        preparedStatement.setLong(1, flowBinderId);
        ResultSet resultSet = preparedStatement.executeQuery();
        int numberOfSubmitterIdsLocated = 0;
        int tableRowsFoundInTotal = 0;

        while (resultSet.next()) {
            tableRowsFoundInTotal++;
            String flowBindersSubmittersId = resultSet.getString(TABLE_FLOW_BINDERS_SUBMITTERS_SUBMITTER_ID);
            for (Long submitterId : submitterIds) {
                if (flowBindersSubmittersId.equals(Long.toString(submitterId))) {
                    numberOfSubmitterIdsLocated++;
                }
            }
        }
        // Assert that each submitter, referenced by the updated flow binder, has been located
        assertThat(numberOfSubmitterIdsLocated, is(submitterIds.size()));
        // Assert that only the expected entries exist in the underlying database
        assertThat(numberOfSubmitterIdsLocated, is(tableRowsFoundInTotal));
        resultSet.close();
        preparedStatement.close();
    }

    private void assertFlowBinderNotNull(FlowBinder flowBinder) {
        assertThat(flowBinder, not(nullValue()));
        assertThat(flowBinder.getContent(), not(nullValue()));
        assertThat(flowBinder.getId(), not(nullValue()));
        assertThat(flowBinder.getVersion(), not(nullValue()));
    }

    private void assertFlowBinderEquals(FlowBinder flowBinderA, FlowBinder flowBinderB, int versionIncrement) {
        assertThat(flowBinderA.getId(), is(flowBinderB.getId()));
        assertThat(flowBinderA.getVersion(), is(flowBinderB.getVersion() + versionIncrement));
    }

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
