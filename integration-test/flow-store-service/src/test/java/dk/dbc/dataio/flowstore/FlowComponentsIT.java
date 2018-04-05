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
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import net.jodah.failsafe.RetryPolicy;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlowComponent;
import static dk.dbc.dataio.integrationtest.ITUtil.newIntegrationTestConnection;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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
     * When : valid JSON is POSTed to the flow component path without an identifier
     * Then : a flow component is created and returned
     * And  : assert that the flow component created has an id, a version and contains the same information as the flowComponentContent given as input
     * And  : assert that only one flowComponent can be found in the underlying database
     */
    @Test
    public void createFlowComponent_ok() throws Exception{

        // When...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();

        // Then...
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        // And...
        assertNotNull(flowComponent);
        assertNotNull(flowComponent.getContent());
        assertNotNull(flowComponent.getId());
        assertNotNull(flowComponent.getVersion());
        assertThat(flowComponent.getContent(), is(flowComponentContent));
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
            flowStoreServiceConnector.getFlowComponent(100432);

            fail("Invalid request to getFlowComponent() was not detected.");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow component with given id is already stored
     * When : valid JSON is POSTed to the flow component path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the flow component has not changed
     * And  : assert that the version number has been updated
     * And  : assert that updated data can be found in the underlying database and only one flow component exists
     */
    @Test
    public void updateFlowComponent_ok() throws Exception{
        // Given...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        // When...
        final FlowComponentContent newFlowComponentContent = new FlowComponentContentBuilder().setSvnRevision(2).setInvocationJavascriptName("updatedInvocationJavascriptName").build();
        FlowComponent updatedFlowComponent = flowStoreServiceConnector.updateFlowComponent(newFlowComponentContent, flowComponent.getId(), flowComponent.getVersion());

        // Then...
        assertNotNull(updatedFlowComponent);
        assertNotNull(updatedFlowComponent.getContent());
        assertNotNull(updatedFlowComponent.getId());
        assertNotNull(updatedFlowComponent.getVersion());
        assertThat(updatedFlowComponent.getContent().getSvnRevision(), is(newFlowComponentContent.getSvnRevision()));
        assertThat(updatedFlowComponent.getContent().getInvocationJavascriptName(), is(newFlowComponentContent.getInvocationJavascriptName()));

        // And...
        assertThat(updatedFlowComponent.getId(), is(flowComponent.getId()));

        // And...
        assertThat(updatedFlowComponent.getVersion(), not(flowComponent.getVersion()));
        assertThat(updatedFlowComponent.getVersion(), is(flowComponent.getVersion() + 1));

        // And...
        final List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();
        assertThat(flowComponents.size(), is(1));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the flow component path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateFlowComponent_invalidJson_BadRequest() {
        // Given ...
        final long id = createFlowComponent(restClient, baseUrl, new FlowComponentContentJsonBuilder().build());

        // Assume, that the very first created flow component has version number 1:
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");  // Set version = 1
        final Response response = HttpClient.doPostWithJson(restClient, headers, "<invalid json />", baseUrl,
                FlowStoreServiceConstants.FLOW_COMPONENTS, Long.toString(id), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow components path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flow components exist in the underlying database
     */
    @Ignore("Failing on jenkins with status code 409 - to be further investigated")
    @Test
    public void updateFlowComponent_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        try{
            // When...
            final FlowComponentContent newFlowComponentContent = new FlowComponentContentBuilder().build();
            flowStoreServiceConnector.updateFlowComponent(newFlowComponentContent, 1234, 1L);

            fail("Wrong flow component Id was not detected as input to updateFlowComponent().");

            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(404));

            // And...
            final List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();
            assertNotNull(flowComponents);
            assertThat(flowComponents.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : Two valid flow components are already stored
     * When : valid JSON is POSTed to the flow components path with an identifier (update) but with a flow component name,
     *        that is already in use by another existing flow component
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     * And  : assert that two flow components exists in the underlying database
     * And  : updated data cannot be found in the underlying database
     */
    @Test
    public void updateFlowComponent_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        final String FIRST_FLOW_COMPONENT_NAME = "FirstFlowComponentName";
        final String SECOND_FLOW_COMPONENT_NAME = "SecondFlowComponentName";

        try {
            // And...
            final FlowComponentContent flowComponentContent1 = new FlowComponentContentBuilder().setName(FIRST_FLOW_COMPONENT_NAME).build();
            flowStoreServiceConnector.createFlowComponent(flowComponentContent1);

            final FlowComponentContent flowComponentContent2 = new FlowComponentContentBuilder().setName(SECOND_FLOW_COMPONENT_NAME).build();
            FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent2);

            // When... (Attempting to save the second flow component created with the same name as the first flow component created)
            flowStoreServiceConnector.updateFlowComponent(flowComponentContent1, flowComponent.getId(), flowComponent.getVersion());

            fail("Primary key violation was not detected as input to updateFlowComponent().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(406));

            // And...
            final List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();
            assertNotNull(flowComponents);
            assertThat(flowComponents.size(), is(2));

            // And...
            assertThat(flowComponents.get(0).getContent().getName(), is(FIRST_FLOW_COMPONENT_NAME));
            assertThat(flowComponents.get(1).getContent().getName(), is (SECOND_FLOW_COMPONENT_NAME));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow component with given id is already stored and the flow component is opened for edit by two different users
     * And  : the first user updates the flow component, valid JSON is POSTed to the flow components path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the flow component, valid JSON is POSTed to the flow components
     *        path with an identifier (update) and wrong version number

     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     * And  : assert that only one flow component exists in the underlying database
     * And  : assert that updated data from the first user can be found in the underlying database
     * And  : assert that the version number has been updated only by the first user
     */
    @Test
    public void updateFlowComponent_WrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final String FLOW_COMPONENT_NAME_FROM_FIRST_USER = "UpdatedFlowComponentNameFromFirstUser";
        final String FLOW_COMPONENT_NAME_FROM_SECOND_USER = "UpdatedFlowComponentNameFromSecondUser";
        long version = -1;

        try {
            // And...
            final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
            FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
            version = flowComponent.getVersion();

            // And... First user updates the flow component
            flowStoreServiceConnector.updateFlowComponent(new FlowComponentContentBuilder().setName(FLOW_COMPONENT_NAME_FROM_FIRST_USER).build(),
                    flowComponent.getId(),
                    flowComponent.getVersion());

            // When... Second user attempts to update the same flow component
            flowStoreServiceConnector.updateFlowComponent(new FlowComponentContentBuilder().setName(FLOW_COMPONENT_NAME_FROM_SECOND_USER).build(),
                    flowComponent.getId(),
                    flowComponent.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateFlowComponent().");

            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(409));

            // And...
            final List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();
            assertNotNull(flowComponents);
            assertThat(flowComponents.size(), is(1));
            assertThat(flowComponents.get(0).getContent().getName(), is(FLOW_COMPONENT_NAME_FROM_FIRST_USER));

            // And... Assert the version number has been updated after creation, but only by the first user.
            assertThat(flowComponents.get(0).getVersion(), is(version +1));
        }
    }


    /**
     * Given: a deployed flow-store service
     * And  : a valid flow component with given id is already stored
     * When : valid JSON is POSTed to the flow component path with an identifier (update next)
     * Then : assert the correct fields have been
     * And  : assert that the id of the flow component has not changed
     * And  : assert that the version number has been updated
     * And  : assert that content and next contains the expected values
     * And  : assert that updated data can be found in the underlying database and only one flow component exists
     */
    @Test
    public void updateNextWithFlowComponentContent_ok() throws Exception{
        // Given...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        assertThat("created flowComponent.getNext() is null", flowComponent.getNext(), is(nullValue()));

        // When...
        final FlowComponentContent next = new FlowComponentContentBuilder().setSvnRevision(2).setInvocationJavascriptName("updatedInvocationJavascriptName").build();
        FlowComponent updatedFlowComponent = flowStoreServiceConnector.updateNext(next, flowComponent.getId(), flowComponent.getVersion());

        // Then...
        assertNotNull("updated flowComponent not null", updatedFlowComponent);
        assertNotNull("updated flowComponent content not null", updatedFlowComponent.getContent());
        assertNotNull("updated flowComponent next not null", updatedFlowComponent.getNext());

        // And...
        assertThat("updated flowComponent id", updatedFlowComponent.getId(), is(flowComponent.getId()));

        // And...
        assertThat("updated flowComponent version", updatedFlowComponent.getVersion(), is(flowComponent.getVersion() + 1));

        // And...
        assertThat("next SVN revision", updatedFlowComponent.getNext().getSvnRevision(), is(next.getSvnRevision()));
        assertThat("next invocation javascript name", updatedFlowComponent.getNext().getInvocationJavascriptName(), is(next.getInvocationJavascriptName()));

        assertThat("flowComponent content SVN revision", updatedFlowComponent.getContent().getSvnRevision(), is(flowComponent.getContent().getSvnRevision()));
        assertThat("flowComponent content invocation javascript name", updatedFlowComponent.getContent().getInvocationJavascriptName(), is(flowComponent.getContent().getInvocationJavascriptName()));

        // And...
        final List<FlowComponent> flowComponents = flowStoreServiceConnector.findAllFlowComponents();
        assertThat("1 flow component stored in the underlying database", flowComponents.size(), is(1));
    }

    /**
     * Given: a deployed flow-store service and a none referenced flow component is stored
     * When : attempting to delete the flow component
     * Then : the flow component is deleted
     */
    @Test
    public void deleteFlowComponent_Ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        assertThat("created flowComponent.getNext() is null", flowComponent.getNext(), is(nullValue()));

        // When...
        flowStoreServiceConnector.deleteFlowComponent(flowComponent.getId(), flowComponent.getVersion());

        // Then... Verify that the flow component is deleted
        try {
            flowStoreServiceConnector.getFlowComponent(flowComponent.getId());
            fail("Flow component was not deleted");
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to delete a flow component that does not exist
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void deleteFlowComponent_NoFlowComponentToDelete() throws ProcessingException {
        // Given...
        final long flowComponentIdNotExists = 9999;
        final long versionNotExists = 9;

        try {
            // When...
            flowStoreServiceConnector.deleteFlowComponent(flowComponentIdNotExists, versionNotExists);
            fail("None existing flow component was not detected");

            // Then ...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

}
