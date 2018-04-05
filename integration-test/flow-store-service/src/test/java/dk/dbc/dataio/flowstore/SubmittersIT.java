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
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import net.jodah.failsafe.RetryPolicy;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createSubmitter;
import static dk.dbc.dataio.integrationtest.ITUtil.newIntegrationTestConnection;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
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

    private static FlowStoreServiceConnector flowStoreServiceConnector;
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

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

    @Before
    @After
    public void tearDown() throws SQLException {
        clearAllDbTables(dbConnection);
    }

    private Submitter createSubmitterFromConnector(SubmitterContent submitterContent) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException  {
        return flowStoreServiceConnector.createSubmitter(submitterContent);
    }

    private Submitter updateSubmitterFromConnector(SubmitterContent submitterContent, Long submitterId, Long version) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException  {
        return flowStoreServiceConnector.updateSubmitter(submitterContent, submitterId, version);
    }

    /*
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

        // Then...
        final Submitter submitter = this.createSubmitterFromConnector(submitterContent);

        // And...
        assertNotNull(submitter);
        assertNotNull(submitter.getContent());
        assertNotNull(submitter.getId());
        assertNotNull(submitter.getVersion());
        assertThat(submitter.getContent(), is(submitterContent));
        // And ...
        final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
        assertThat(submitters.size(), is(1));
    }

    @Test
    public void deleteSubmitter_Ok() throws FlowStoreServiceConnectorException {

        // Configuration
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();

        // Preconditions
        Submitter submitter = this.createSubmitterFromConnector(submitterContent);
        long submitterId = submitter.getId();
        long version = submitter.getVersion();

        // Verify before delete
        Submitter submitterBeforeDelete = flowStoreServiceConnector.getSubmitter(submitterId);
        assertThat(submitterBeforeDelete.getId(), is(submitterId));

        // Subject Under Test
        flowStoreServiceConnector.deleteSubmitter(submitterId, version);

        // Verify that the submitter is deleted
        try {

            flowStoreServiceConnector.getSubmitter(submitterId);

        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {

            // We expect this exception from getSubmitter(...) method when no submitter exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    @Test
    public void deleteSubmitter_NoSubmitterToDelete() throws ProcessingException{

        // Configuration
        final long submitterIdNotExists = 9999;
        final long versionNotExists = 9;

        try {

            // Subject Under Test
            flowStoreServiceConnector.deleteSubmitter(submitterIdNotExists, versionNotExists);

        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {

            // We expect this exception from getSubmitter(...) method when no submitter exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    @Test
    public void deleteSubmitter_OptimisticLocking() throws FlowStoreServiceConnectorException {

        // Configuration
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();

        // Preconditions
        Submitter submitter = this.createSubmitterFromConnector(submitterContent);
        long submitterId = submitter.getId();
        long versionFirst = submitter.getVersion();
        long versionSecond = versionFirst + 1;

        // Update submitter to bump version no.
        final SubmitterContent newSubmitterContent = new SubmitterContentBuilder().setName("UpdatedSubmitterName").setNumber(43L).build();
        final Submitter submitterUpdated = this.updateSubmitterFromConnector(newSubmitterContent, submitterId, versionFirst);
        assertThat(submitterUpdated.getVersion(), is(versionSecond));

        // Verify before delete
        Submitter submitterBeforeDelete = flowStoreServiceConnector.getSubmitter(submitterId);
        assertThat(submitterBeforeDelete.getId(), is(submitterId));
        assertThat(submitterBeforeDelete.getVersion(), is(versionSecond));

        try {

            // Subject Under Test
            flowStoreServiceConnector.deleteSubmitter(submitterId, versionFirst);

        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {

            // We expect this exception from getSubmitter(...) method when no submitter exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
        }
    }

    @Test
    public void deleteSubmitter_FlowBinderExists() throws FlowStoreServiceConnectorException {

        // Preconditions
        final Flow flow           = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink           = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());
        final long submitterId = submitter.getId();

        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        // Get submitter - then the version is set correct before deletion
        Submitter submitterBeforeDelete = flowStoreServiceConnector.getSubmitter(submitterId);

        try {

            // Subject Under Test
            flowStoreServiceConnector.deleteSubmitter(submitterId, submitterBeforeDelete.getVersion());

        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {

            // We expect this exception from getSubmitter(...) method when no submitter exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
        }

    }

    /*
     * Given: a deployed flow-store service
     * When: JSON posted to the submitters path causes JSONBException
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createSubmitter_ErrorWhenJsonExceptionIsThrown() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceConstants.SUBMITTERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /*
     * Given: a deployed flow-store service containing submitter resource
     * When : adding submitter with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one submitter exist in the underlying database
     */
    @Test
    public void createSubmitter_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
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

   /*
    * Given: a deployed flow-store service containing submitter resource
    * When : adding submitter with the same number
    * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
    * And  : request returns with a NOT ACCEPTABLE http status code
    * And  : assert that one submitter exist in the underlying database
    */
    @Test
    public void createSubmitter_duplicateNumber_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
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
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the submitters path with a valid identifier
     * Then : a submitter is found and returned
     * And  : assert that the submitter found has an id, a version and contains the same information as the submitter created
     */
    @Test
    public void getSubmitter_ok() throws Exception{

        // When...
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();

        // Then...
        Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);
        Submitter submitterToGet = flowStoreServiceConnector.getSubmitter(submitter.getId());

        // And...
        assertNotNull(submitterToGet);
        assertNotNull(submitterToGet.getContent());
        assertThat(submitterToGet.getContent(), is(submitter.getContent()));
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a submitter with an unknown submitter id
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getSubmitter_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException{
        try{
            // Given...
            // Stupid hack to avoid JPA cache problem when testing
            // @Before clears database outside but JPA don't know
            Date d=new Date();
            flowStoreServiceConnector.getSubmitter(14732L+d.getTime());

            fail("Invalid request to getSubmitter() was not detected.");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the submitters path with a valid identifier
     * Then : a submitter is found and returned
     * And  : assert that the submitter found has an id, a version and contains the same information as the submitter created
     */
    @Test
    public void getSubmitterBySubmitterNumber_ok() throws Exception{

        // When...
        final SubmitterContent submitterContent = new SubmitterContentBuilder().setNumber(32123L).build();

        // Then...
        Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);
        Submitter submitterToGet = flowStoreServiceConnector.getSubmitterBySubmitterNumber(submitter.getContent().getNumber());

        // And...
        assertNotNull(submitterToGet);
        assertNotNull(submitterToGet.getContent());
        assertThat(submitterToGet.getContent(), is(submitter.getContent()));
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a submitter with an unknown submitter number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getSubmitterBySubmitterNumber_WrongSubmitterNumber_NotFound() throws FlowStoreServiceConnectorException{
        try{
            // Given...
            flowStoreServiceConnector.getSubmitterBySubmitterNumber(4345532L);

            fail("Invalid request to getSubmitterBySubmitterNumber() was not detected.");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    /*
     * Given: a deployed flow-store service
     * And  : a valid submitter with given id is already stored
     * When : valid JSON is POSTed to the submitters path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the submitter has not changed
     * And  : assert that the version number has been updated
     * And  : assert that updated data can be found in the underlying database and only one submitter exists
     */
    @Test
    public void updateSubmitter_ok() throws Exception {
        // Given...
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);

        // When...
        final SubmitterContent newSubmitterContent = new SubmitterContentBuilder().setName("UpdatedSubmitterName").setNumber(43L).build();
        Submitter updatedSubmitter = flowStoreServiceConnector.updateSubmitter(newSubmitterContent, submitter.getId(), submitter.getVersion());

        // Then...
        assertSubmitterNotNull(updatedSubmitter);
        assertThat(updatedSubmitter.getContent(), is(newSubmitterContent));

        // And...
        assertThat(updatedSubmitter.getId(), is(submitter.getId()));

        // And...
        assertThat(updatedSubmitter.getVersion(), is(submitter.getVersion() + 1));

        // And...
        final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
        assertThat(submitters.size(), is(1));
        assertThat(submitters.get(0) , is(updatedSubmitter));
    }

    /*
     * Given: a deployed flow-store service with a submitter
     * When : JSON posted to the submitters path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateSubmitter_invalidJson_BadRequest() {
        // Given ...
        final long id = createSubmitter(restClient, baseUrl, new SubmitterContentJsonBuilder().build());

        // Assume, that the very first created submitter has version number 1:
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");  // Set version=1
        final Response response = HttpClient.doPostWithJson(restClient, headers, "<invalid json />", baseUrl,
                FlowStoreServiceConstants.SUBMITTERS, Long.toString(id), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

   /*
    * Given: a deployed flow-store service
    * When : valid JSON is POSTed to the submitters path with an identifier (update) and wrong id number
    * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
    * And  : request returns with a NOT_FOUND http status code
    * And  : assert that no submitters exist in the underlying database
    */
    @Test
    public void updateSubmitter_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        try{
            // When...
            final SubmitterContent newSubmitterContent = new SubmitterContentBuilder().build();
            // Stupid hack to avoid JPA cache problem when testing
            // @Before clears database outside but JPA don't know
            Date d=new Date();
            flowStoreServiceConnector.updateSubmitter(newSubmitterContent, 1234+d.getTime(), 1L);

            fail("Wrong submitter Id was not detected as input to updateSubmitter().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));

            // And...
            final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
            assertNotNull(submitters);
            assertThat(submitters.isEmpty(), is(true));
        }
    }

    /*
     * Given: a deployed flow-store service
     * And  : Two valid submitters are already stored
     * When : valid JSON is POSTed to the submitters path with an identifier (update) but with a submitter name that is already in use by another existing submitter
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     * And  : assert that two submitters exists in the underlying database
     * And  : updated data cannot be found in the underlying database
     */
    @Test
    public void updateSubmitter_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        final String FIRST_SUBMITTER_NAME = "FirstSubmitterName";
        final String SECOND_SUBMITTER_NAME = "SecondSubmitterName";

        try {
            // And...
            final SubmitterContent submitterContent1 = new SubmitterContentBuilder().setName(FIRST_SUBMITTER_NAME).setNumber(17L).build();
            flowStoreServiceConnector.createSubmitter(submitterContent1);

            final SubmitterContent submitterContent2 = new SubmitterContentBuilder().setName(SECOND_SUBMITTER_NAME).setNumber(31L).build();
            Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent2);

            // When... (Attempting to save the second submitter created with the same name as the first submitter created)
            flowStoreServiceConnector.updateSubmitter(submitterContent1, submitter.getId(), submitter.getVersion());

            fail("Primary key violation was not detected as input to updateSubmitter().");
        // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));

            // And...
            final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
            assertNotNull(submitters);
            assertThat(submitters.size(), is(2));

            // And...
            assertThat(submitters.get(0).getContent().getName(), is (FIRST_SUBMITTER_NAME));
            assertThat(submitters.get(1).getContent().getName(), is (SECOND_SUBMITTER_NAME));
        }
    }

    /*
     * Given: a deployed flow-store service
     * And  : a valid submitter with given id is already stored and the submitter is opened for edit by two different users
     * And  : the first user updates the submitter, valid JSON is POSTed to the submitters path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the submitter, valid JSON is POSTed to the submitters
     *        path with an identifier (update) and wrong version number

     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     * And  : assert that only one submitter exists in the underlying database
     * And  : assert that updated data from the first user can be found in the underlying database
     * And  : assert that the version number has been updated only by the first user
     */
    @Test
    public void updateSubmitter_WrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final String SUBMITTER_NAME_FROM_FIRST_USER = "UpdatedSubmitterNameFromFirstUser";
        final String SUBMITTER_NAME_FROM_SECOND_USER = "UpdatedSubmitterNameFromSecondUser";
        long version = -21;

        try {
            // And...
            Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());
            version = submitter.getVersion(); // stored for use in catch-clause

            // And... First user updates the submitter
            SubmitterContent submitterContent1 = new SubmitterContentBuilder().setName(SUBMITTER_NAME_FROM_FIRST_USER).setNumber(98L).build();
            flowStoreServiceConnector.updateSubmitter(submitterContent1, submitter.getId(), submitter.getVersion());

            // When... Second user attempts to update the same submitter
            SubmitterContent submitterContent2 = new SubmitterContentBuilder().setName(SUBMITTER_NAME_FROM_SECOND_USER).setNumber(99L).build();
            flowStoreServiceConnector.updateSubmitter(submitterContent2, submitter.getId(), submitter.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateSubmitter().");

            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {

            // And...
            assertThat(e.getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));

            // And...
            final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();
            assertNotNull(submitters);
            assertThat(submitters.size(), is(1));
            assertThat(submitters.get(0).getContent().getName(), is(SUBMITTER_NAME_FROM_FIRST_USER));

            // And... Assert the version number has been updated after creation, but only by the first user.
            assertThat(submitters.get(0).getVersion(), is(version + 1));
        }
    }

    /*
     * Given: a deployed flow-store service containing no submitters
     * When: GETing submitters collection
     * Then: request returns with empty list
     */
    @Test
    public void findAllSubmitters_emptyResult() throws Exception {
        // When...
        final List<Submitter> submitters = flowStoreServiceConnector.findAllSubmitters();

        // Then...
        assertThat(submitters, is(notNullValue()));
        assertThat(submitters.size(), is(0));
    }

    /*
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

    private void assertSubmitterNotNull(Submitter submitter) {
        assertNotNull(submitter);
        assertNotNull(submitter.getContent());
        assertNotNull(submitter.getId());
        assertNotNull(submitter.getVersion());

    }
}
