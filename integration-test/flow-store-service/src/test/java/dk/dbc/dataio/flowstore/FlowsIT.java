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
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlow;
import static dk.dbc.dataio.integrationtest.ITUtil.newIntegrationTestConnection;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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
    private static FlowStoreServiceConnector flowStoreServiceConnector;

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
            // When...
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
     * When : valid JSON is POSTed to the flows path with a valid identifier
     * Then : a flow is found and returned
     * And  : assert that the flow found has an id, a version and contains the same information as the flow created
     */
    @Test
    public void findFlowByName_ok() throws Exception {

        // When...
        final FlowContent flowContent = new FlowContentBuilder().setName("test flow").build();

        // Then...
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);
        Flow flowToGet = flowStoreServiceConnector.findFlowByName(flowContent.getName());

        // And...
        assertNotNull(flowToGet);
        assertNotNull(flowToGet.getContent());
        assertThat(flowToGet.getContent().getName(), is(flowToGet.getContent().getName()));
        assertThat(flowToGet.getContent().getDescription(), is(flow.getContent().getDescription()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an none existing identifier
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void findFlowByName_notFound() throws Exception {
        try {
            // When...
            flowStoreServiceConnector.findFlowByName("test flow");

            fail("Invalid request to findFlowByName() was not detected.");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service where a valid flow with given id is already stored
     * When : valid JSON is POSTed to the flow path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the flow has not changed
     * And  : assert that the version number has been updated
     * And  : assert that updated data can be found in the underlying database and only one flow exists
     */
    @Test
    public void refreshFlowComponents_ok() throws Exception{

        // Given...
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(new FlowComponentContentBuilder().build());

        // Create flow containing the flow component created above
        final FlowContent flowContent = new FlowContentBuilder().setComponents(Collections.singletonList(flowComponent)).build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // Update the component to a newer revision
        FlowComponentContent updatedFlowComponentContent = new FlowComponentContent(
                flowComponent.getContent().getName(),
                flowComponent.getContent().getSvnProjectForInvocationJavascript(),
                flowComponent.getContent().getSvnRevision() + 1,
                flowComponent.getContent().getInvocationJavascriptName(),
                flowComponent.getContent().getJavascripts(),
                flowComponent.getContent().getInvocationMethod(),
                flowComponent.getContent().getDescription());

        flowStoreServiceConnector.updateFlowComponent(updatedFlowComponentContent, flowComponent.getId(), flowComponent.getVersion());

        // When...
        // Update the flow component embedded within the flow to the latest svn revision
        Flow updatedFlow = flowStoreServiceConnector.refreshFlowComponents(flow.getId(), flow.getVersion());

        // Then...
        assertFlowNotNull(updatedFlow);
        assertFlowContentEquals(true, updatedFlow.getContent(), flow.getContent());

        // And...
        assertThat(updatedFlow.getId(), is(flow.getId()));

        // And...
        assertThat(updatedFlow.getVersion(), is(flow.getVersion() + 1));
        assertThat(updatedFlow.getContent().getTimeOfFlowComponentUpdate(), is(notNullValue()));

        // And...
        final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
        assertThat(flows.size(), is(1));
        assertFlowEquals(true, flows.get(0), updatedFlow);
    }

    /**
     * Given: a deployed flow-store service where a valid flow with given id is already stored
     * When : valid JSON is POSTed to the flow path with an identifier (update)
     * Then : assert that the timeOfFlowComponentUpdate has been set
     */
    @Test
    public void refreshFlowComponents_noFlowComponentChanges_ok() throws Exception{

        // Given...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        final FlowContent flowContent = new FlowContentBuilder().setComponents(Collections.singletonList(flowComponent)).build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        Flow updatedFlow = flowStoreServiceConnector.refreshFlowComponents(flow.getId(), flow.getVersion());

        // Then...
        assertFlowNotNull(updatedFlow);
        assertThat(updatedFlow.getVersion(), is(flow.getVersion()));
        assertThat(updatedFlow.getContent().getTimeOfFlowComponentUpdate(), is(nullValue()));
    }


    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flows exist in the underlying database
     */
    @Test
    public void refreshFlowComponents_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try{
            // When...
            flowStoreServiceConnector.refreshFlowComponents(12347, 1L);

            fail("Wrong flow Id was not detected as input to refreshFlowComponents().");

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
     * Given: a deployed flow-store service where a valid flow with given id is already stored and the flow
     *        is opened for edit by two different users
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
    public void refreshFlowComponents_WrongVersion_Conflict() throws FlowStoreServiceConnectorException {

        // Given...
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
                    flowComponent.getContent().getInvocationMethod(),
                    flowComponent.getContent().getDescription());

            flowStoreServiceConnector.updateFlowComponent(updatedFlowComponentContent, flowComponent.getId(), flowComponent.getVersion());

            // And... First user updates the flow
            flowStoreServiceConnector.refreshFlowComponents(flow.getId(), flow.getVersion());

            // When... Second user attempts to update the same flow
            flowStoreServiceConnector.refreshFlowComponents(flow.getId(), flow.getVersion());
            fail("Edit conflict, in the case of multiple updates, was not detected as input to refreshFlowComponents().");

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

            assertFlowNotNull(flow);
            assertThat(flow.getContent().getComponents().get(0).getId(), is(flowComponent.getId()));
            assertThat(flow.getContent().getComponents().get(0).getVersion(), is(flowComponent.getVersion()));
            assertThat(flow.getContent().getComponents().get(0).getContent().getSvnRevision(), is(flowComponent.getContent().getSvnRevision()));

            // And ...
            assertThat(flowStoreServiceConnector.findAllFlows().size(), is(1));

            // And... Assert the version number of the flow has been updated after creation, but only by the first user.
            assertThat(flow.getVersion(), is(flowVersion + 1));
        }
    }

    /*
     * Given: a deployed flow-store service where a valid flow with given id is already stored
     * When : valid JSON is POSTed to the flows path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the flow has not changed
     * And  : assert that the version number has been updated and that the timeOfFlowComponentUpdate has been been set
     *        as the nested flowComponents were unchanged
     * And  : assert that updated data can be found in the underlying database and only one flow exists
     */
    @Test
    public void updateFlow_ok() throws Exception {

        // Given...
        final FlowContent flowContent = new FlowContentBuilder().build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // When...
        final FlowContent newFlowContent = new FlowContentBuilder().setName("UpdatedFlowName").setDescription("UpdatedDescription").build();
        Flow updatedFlow = flowStoreServiceConnector.updateFlow(newFlowContent, flow.getId(), flow.getVersion());

        // Then...
        assertFlowNotNull(updatedFlow);
        assertFlowContentEquals(false, updatedFlow.getContent(), newFlowContent);

        // And...
        assertThat(updatedFlow.getId(), is(flow.getId()));

        // And...
        assertThat(updatedFlow.getVersion(), is(flow.getVersion() + 1));
        assertThat(updatedFlow.getContent().getTimeOfFlowComponentUpdate(), is(nullValue()));

        // And...
        final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
        assertThat(flows.size(), is(1));
        assertFlowEquals(false, flows.get(0), updatedFlow);
    }


    @Test
    public void updateFlowSetTimeOfFlowComponentUpdate_ok() throws Exception {

        // Given...
        final FlowContent flowContent = new FlowContentBuilder().build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // When...
        final FlowComponent newFlowComponent = new FlowComponentBuilder().setContent(new FlowComponentContentBuilder().setSvnRevision(123456L).build()).build();
        final FlowContent newFlowContent = new FlowContentBuilder().setComponents(Collections.singletonList(newFlowComponent)).build();
        Flow updatedFlow = flowStoreServiceConnector.updateFlow(newFlowContent, flow.getId(), flow.getVersion());

        // Then...
        assertFlowNotNull(updatedFlow);
        assertFlowContentEquals(false, updatedFlow.getContent(), newFlowContent);

        // And...
        assertThat(updatedFlow.getId(), is(flow.getId()));

        // And...
        assertThat(updatedFlow.getVersion(), is(flow.getVersion() + 1));
        assertThat(updatedFlow.getContent().getTimeOfFlowComponentUpdate(), is(notNullValue()));

        // And...
        final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
        assertThat(flows.size(), is(1));
        assertFlowEquals(false, flows.get(0), updatedFlow);
    }

    /*
     * Given: a deployed flow-store service with a flow
     * When : JSON posted to the flows path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateFlow_invalidJson_BadRequest() {
        // Given ...
        final long id = createFlow(restClient, baseUrl, new FlowContentJsonBuilder().build());

        // Assume, that the very first created flow has version number 1:
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");  // Set version = 1
        final Response response = HttpClient.doPostWithJson(restClient, headers, "<invalid json />", baseUrl,
                FlowStoreServiceConstants.FLOWS, Long.toString(id), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /*
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flows exist in the underlying database
     */
    @Test
    public void updateFlow_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try{
            // When...
            final FlowContent newFlowContent = new FlowContentBuilder().build();
            flowStoreServiceConnector.updateFlow(newFlowContent, 12345, 1L);

            fail("Wrong flow Id was not detected as input to updateFlow().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));

            // And...
            final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
            assertNotNull(flows);
            assertThat(flows.isEmpty(), is(true));
        }
    }

    /*
     * Given: a deployed flow-store service where two valid flows are already stored
     * When : valid JSON is POSTed to the flows path with an identifier (update) but with a flow name that is already in use by another existing flow
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     * And  : assert that two flows exists in the underlying database
     * And  : updated data cannot be found in the underlying database
     */
    @Test
    public void updateFlow_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        final String FIRST_FLOW_NAME = "FirstFlowName";
        final String SECOND_FLOW_NAME = "SecondFlowName";

        try {
            final FlowContent flowContent1 = new FlowContentBuilder().setName(FIRST_FLOW_NAME).setDescription("UpdatedDescription1").build();
            flowStoreServiceConnector.createFlow(flowContent1);

            final FlowContent flowContent2 = new FlowContentBuilder().setName(SECOND_FLOW_NAME).setDescription("UpdatedDescription2").build();
            Flow flow = flowStoreServiceConnector.createFlow(flowContent2);

            // When... (Attempting to save the second flow created with the same name as the first flow created)
            flowStoreServiceConnector.updateFlow(flowContent1, flow.getId(), flow.getVersion());

            fail("Primary key violation was not detected as input to updateFlow().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));

            // And...
            final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
            assertNotNull(flows);
            assertThat(flows.size(), is(2));

            // And...
            assertThat(flows.get(0).getContent().getName(), is(FIRST_FLOW_NAME));
            assertThat(flows.get(1).getContent().getName(), is (SECOND_FLOW_NAME));
        }
    }

    /*
     * Given: a deployed flow-store service where a valid flow with given id is already stored and
     *        the flow is opened for edit by two different users
     * And  : the first user updates the flow, valid JSON is POSTed to the flows path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the flow, valid JSON is POSTed to the submitters
     *        path with an identifier (update) and wrong version number

     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     * And  : assert that only one flow exists in the underlying database
     * And  : assert that updated data from the first user can be found in the underlying database
     * And  : assert that the version number has been updated only by the first user
     */
    @Test
    public void updateFlow_WrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final String FLOW_NAME_FROM_FIRST_USER = "UpdatedFlowNameFromFirstUser";
        final String FLOW_NAME_FROM_SECOND_USER = "UpdatedFlowNameFromSecondUser";
        long version = -21;

        try {
            Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
            version = flow.getVersion(); // stored for use in catch-clause

            // And... First user updates the flow
            FlowContent flowContent1 = new FlowContentBuilder().setName(FLOW_NAME_FROM_FIRST_USER).build();
            flowStoreServiceConnector.updateFlow(flowContent1, flow.getId(), flow.getVersion());

            // When... Second user attempts to update the same flow
            FlowContent flowContent2 = new FlowContentBuilder().setName(FLOW_NAME_FROM_SECOND_USER).build();
            flowStoreServiceConnector.updateFlow(flowContent2, flow.getId(), flow.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateFlow().");

            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {

            // And...
            assertThat(e.getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));

            // And...
            final List<Flow> flows = flowStoreServiceConnector.findAllFlows();
            assertNotNull(flows);
            assertThat(flows.size(), is(1));
            assertThat(flows.get(0).getContent().getName(), is(FLOW_NAME_FROM_FIRST_USER));

            // And... Assert the version number has been updated after creation, but only by the first user.
            assertThat(flows.get(0).getVersion(), is(version + 1));
        }
    }

    /**
     * Given: a deployed flow-store service and a none referenced flow is stored
     * When : attempting to delete the flow
     * Then : the flow is deleted
     */
    @Test
    public void deleteFlow_Ok() throws FlowStoreServiceConnectorException {

        // Given...
        final FlowContent flowContent = new FlowContentBuilder().build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // When...
        flowStoreServiceConnector.deleteFlow(flow.getId(), flow.getVersion());

        // Then... Verify that the flow is deleted
        try {
            flowStoreServiceConnector.getFlow(flow.getId());
            fail("Flow was not deleted");
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // We expect this exception from getSink(...) method when no flow exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to delete a flow that does not exist
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void deleteFlow_NoFlowToDelete() throws ProcessingException {

        // Given...
        final long flowIdNotExists = 9999;
        final long versionNotExists = 9;

        try {
            // When...
            flowStoreServiceConnector.deleteFlow(flowIdNotExists, versionNotExists);
            fail("None existing flow was not detected");

            // Then ...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service and a none referenced flow is stored.
     * And  : the flow is updated and, valid JSON is POSTed to the flows path with an identifier (update)
     *        and correct version number
     * When : attempting to delete the flow with the previous version number, valid JSON is POSTed to the flows
     *        path with an identifier (delete) and wrong version number

     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void deleteFlow_OptimisticLocking() throws FlowStoreServiceConnectorException {

        // Given...
        final FlowContent flowContent = new FlowContentBuilder().build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);
        long versionFirst = flow.getVersion();
        long versionSecond = versionFirst + 1;

        // And
        final FlowContent newFlowContent = new FlowContentBuilder().setName("UpdatedFlowName").build();
        final Flow flowUpdated = flowStoreServiceConnector.updateFlow(newFlowContent, flow.getId(), versionFirst);
        assertThat(flowUpdated.getVersion(), is(versionSecond));

        // Verify before delete
        Flow flowBeforeDelete = flowStoreServiceConnector.getFlow(flow.getId());
        assertThat(flowBeforeDelete.getId(), is(flow.getId()));
        assertThat(flowBeforeDelete.getVersion(), is(versionSecond));

        try {
            // When...
            flowStoreServiceConnector.deleteFlow(flow.getId(), versionFirst);
            fail("Flow was deleted");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
        }
    }

    /**
     * Given: a deployed flow-store service and a flow referenced by a flow binder is stored.
     * When : attempting to delete the referenced flow
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void deleteFlow_FlowBinderExists() throws FlowStoreServiceConnectorException {

        // Given
        final Flow flow             = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
        final Sink sink             = flowStoreServiceConnector.createSink(new SinkContentBuilder().build());
        final Submitter submitter   = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder().build());

        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        try {
            // When...
            flowStoreServiceConnector.deleteFlow(flow.getId(), flow.getVersion());
            fail("Flow was deleted");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
        }
    }

    private void assertFlowNotNull(Flow flow) {
        assertNotNull(flow);
        assertNotNull(flow.getContent());
        assertNotNull(flow.getId());
        assertNotNull(flow.getVersion());
    }

    private void assertFlowContentEquals(boolean isRefresh, FlowContent flowContent1, FlowContent flowContent2) {
        assertThat(flowContent1.getName(), is(flowContent2.getName()));
        assertThat(flowContent1.getDescription(), is(flowContent2.getDescription()));
        assertThat(flowContent1.getComponents().size(), is(flowContent2.getComponents().size()));
        if(isRefresh){
            assertFlowComponentDifferentVersion(flowContent1.getComponents(), flowContent2.getComponents());
        } else {
            assertFlowComponents(flowContent1.getComponents(), flowContent2.getComponents());
        }
    }

    private void assertFlowComponents(List<FlowComponent> flowComponents1, List<FlowComponent> flowComponents2) {
        assertThat(flowComponents1.size(), is(flowComponents2.size()));
        for(int i = 0; i < flowComponents1.size(); i++) {
            assertThat(flowComponents1.get(i), is(flowComponents2.get(i)));
        }
    }

    private void assertFlowComponentDifferentVersion(List<FlowComponent> flowComponents1, List<FlowComponent> flowComponents2) {
        boolean hasComponentsBeenUpdated = false;
        for(int i = 0; i < flowComponents1.size(); i++) {
            assertThat(flowComponents1.get(i).getId(), is(flowComponents2.get(i).getId()));
            if(flowComponents1.get(i).getVersion() != flowComponents2.get(i).getVersion()) {
                hasComponentsBeenUpdated = true;
            }
        }
        assertThat(hasComponentsBeenUpdated, is(true));
    }

    private void assertFlowEquals(boolean isRefresh, Flow flow1, Flow flow2) {
        assertThat(flow1.getId(), is(flow2.getId()));
        if(!isRefresh) {
            assertThat(flow1.getVersion(), is(flow2.getVersion()));
            assertFlowContentEquals(false, flow1.getContent(), flow2.getContent());
        }
    }

}
