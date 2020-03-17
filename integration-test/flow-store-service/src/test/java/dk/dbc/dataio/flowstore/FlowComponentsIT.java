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
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowComponentView;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.httpclient.HttpClient;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration tests for the flow components collection part of the flow store service
 */
public class FlowComponentsIT extends AbstractFlowStoreServiceContainerTest {
    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow component path without an identifier
     * Then : a flow component is created and returned
     * And  : assert that the flow component created contains the same information
     *        as the flowComponentContent given as input
     * And  : the flow component view is updated
     */
    @Test
    public void createFlowComponent_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.createFlowComponent_ok")
                .build();

        // Then...
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        // And...
        assertThat(flowComponent.getContent(), is(flowComponentContent));

        // And...
        final FlowComponentView flowComponentView = getFlowComponentView(flowComponent.getId());
        assertThat("flow component view version", flowComponentView.getVersion(),
                is(1L));
        assertThat("flow component view name", flowComponentView.getName(),
                is(flowComponent.getContent().getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the components path
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenInvalidJson() {
        // When...
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                "<invalid json />", flowStoreServiceBaseUrl, FlowStoreServiceConstants.FLOW_COMPONENTS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow resource
     * When : adding flow component with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlowComponent_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.createFlowComponent_duplicateName_NotAcceptable")
                .build();

        try {
            flowStoreServiceConnector.createFlowComponent(flowComponentContent);
            // When...
            flowStoreServiceConnector.createFlowComponent(flowComponentContent);
            fail("Primary key violation was not detected as input to createFlowComponent().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /**
     * Given: a deployed flow-store service containing three flows
     * When : GETing flows collection
     * Then : request returns with 3 flows
     * And  : the flows are sorted alphabetically by name
     */
    @Test
    public void findAllFlowComponents_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowComponentContent flowComponentContentA = new FlowComponentContentBuilder()
                .setName("a_FlowComponentsIT.findAllFlowComponents_ok")
                .build();
        final FlowComponentContent flowComponentContentB = new FlowComponentContentBuilder()
                .setName("b_FlowComponentsIT.findAllFlowComponents_ok")
                .build();
        final FlowComponentContent flowComponentContentC = new FlowComponentContentBuilder()
                .setName("c_FlowComponentsIT.findAllFlowComponents_ok")
                .build();

        final FlowComponent flowComponentSortsFirst =
                flowStoreServiceConnector.createFlowComponent(flowComponentContentA);
        final FlowComponent flowComponentSortsSecond =
                flowStoreServiceConnector.createFlowComponent(flowComponentContentB);
        final FlowComponent flowComponentSortsThird =
                flowStoreServiceConnector.createFlowComponent(flowComponentContentC);

        // When...
        final List<FlowComponentView> components = flowStoreServiceConnector.findAllFlowComponents();

        // Then...
        assertThat(components.size() >= 3, is (true));

        // And...
        assertThat(components.get(0).getName(), is(flowComponentSortsFirst.getContent().getName()));
        assertThat(components.get(1).getName(), is(flowComponentSortsSecond.getContent().getName()));
        assertThat(components.get(2).getName(), is(flowComponentSortsThird.getContent().getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flowComponent path with a valid identifier
     * Then : a flowComponent is found and returned
     * And  : assert that the flowComponent found contains the same information as the flowComponent created
     */
    @Test
    public void getFlowComponent_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.getFlowComponent_ok")
                .build();

        // Then...
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        FlowComponent flowComponentToGet = flowStoreServiceConnector.getFlowComponent(flowComponent.getId());

        // And...
        assertNotNull(flowComponentToGet);
        assertNotNull(flowComponentToGet.getContent());
        assertThat(flowComponentToGet.getContent().getName(),
                is(flowComponent.getContent().getName()));
        assertThat(flowComponentToGet.getContent().getInvocationJavascriptName(),
                is(flowComponent.getContent().getInvocationJavascriptName()));
        assertThat(flowComponentToGet.getContent().getInvocationMethod(),
                is(flowComponent.getContent().getInvocationMethod()));
        assertThat(flowComponentToGet.getContent().getSvnProjectForInvocationJavascript(),
                is(flowComponent.getContent().getSvnProjectForInvocationJavascript()));
        assertThat(flowComponentToGet.getContent().getSvnRevision(),
                is(flowComponent.getContent().getSvnRevision()));
        assertThat(flowComponentToGet.getVersion(),
                is(flowComponent.getVersion()));
        assertThat(flowComponentToGet.getContent().getJavascripts().size(),
                is(flowComponent.getContent().getJavascripts().size()));

        for (int i = 0; i < flowComponentToGet.getContent().getJavascripts().size(); i++) {
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
    public void getFlowComponent_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException{
        try{
            // Given...
            flowStoreServiceConnector.getFlowComponent(new Date().getTime());

            fail("Invalid request to getFlowComponent() was not detected.");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
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
     * And  : the flow component view is updated
     */
    @Test
    public void updateFlowComponent_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowComponentContent content = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.updateFlowComponent_ok")
                .build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(content);

        // When...
        final FlowComponentContent updatedContent = new FlowComponentContentBuilder()
                .setName(content.getName())
                .setSvnRevision(2)
                .setInvocationJavascriptName("updatedInvocationJavascriptName")
                .build();
        FlowComponent updatedFlowComponent = flowStoreServiceConnector.updateFlowComponent(
                updatedContent, flowComponent.getId(), flowComponent.getVersion());

        // Then...
        assertThat(updatedFlowComponent.getContent().getSvnRevision(),
                is(updatedContent.getSvnRevision()));
        assertThat(updatedFlowComponent.getContent().getInvocationJavascriptName(),
                is(updatedContent.getInvocationJavascriptName()));

        // And...
        assertThat(updatedFlowComponent.getId(), is(flowComponent.getId()));

        // And...
        assertThat(updatedFlowComponent.getVersion(), is(flowComponent.getVersion() + 1));

        // And...
        final FlowComponentView flowComponentView = getFlowComponentView(flowComponent.getId());
        assertThat("flow component view version", flowComponentView.getVersion(),
                is(flowComponent.getVersion() + 1));
        assertThat("flow component view method", flowComponentView.getScriptName(),
                is(updatedContent.getInvocationJavascriptName()));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the flow component path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateFlowComponent_invalidJson_BadRequest() throws FlowStoreServiceConnectorException {
        // Given ...
        final FlowComponentContent content = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.updateFlowComponent_invalidJson_BadRequest")
                .build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(content);

        // Assume, that the very first created flow component has version number 1:
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");  // Set version = 1
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                headers, "<invalid json />", flowStoreServiceBaseUrl,
                FlowStoreServiceConstants.FLOW_COMPONENTS,
                Long.toString(flowComponent.getId()), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow components path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void updateFlowComponent_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        try{
            // When...
            final FlowComponentContent content = new FlowComponentContentBuilder()
                    .setName("FlowComponentsIT.updateFlowComponent_wrongIdNumber_NotFound")
                    .build();
            flowStoreServiceConnector.updateFlowComponent(content, new Date().getTime(), 1L);

            fail("Wrong flow component Id was not detected as input to updateFlowComponent().");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : Two valid flow components are already stored
     * When : valid JSON is POSTed to the flow components path with an identifier (update) but with a flow component name,
     *        that is already in use by another existing flow component
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void updateFlowComponent_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        final String FIRST_FLOW_COMPONENT_NAME = "FlowComponentsIT.updateFlowComponent_duplicateName_NotAcceptable.1";
        final String SECOND_FLOW_COMPONENT_NAME = "FlowComponentsIT.updateFlowComponent_duplicateName_NotAcceptable.2";

        try {
            // And...
            final FlowComponentContent content1 = new FlowComponentContentBuilder()
                    .setName(FIRST_FLOW_COMPONENT_NAME)
                    .build();
            flowStoreServiceConnector.createFlowComponent(content1);

            final FlowComponentContent content2 = new FlowComponentContentBuilder()
                    .setName(SECOND_FLOW_COMPONENT_NAME)
                    .build();
            FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(content2);

            // When... (Attempting to save the second flow component created with the same name as the first flow component created)
            flowStoreServiceConnector.updateFlowComponent(content1, flowComponent.getId(), flowComponent.getVersion());

            fail("Primary key violation was not detected as input to updateFlowComponent().");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
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
     */
    @Test
    public void updateFlowComponent_wrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final String FLOW_COMPONENT_NAME_FROM_FIRST_USER = "FlowComponentsIT.updateFlowComponent_wrongVersion_Conflict.1";
        final String FLOW_COMPONENT_NAME_FROM_SECOND_USER = "FlowComponentsIT.updateFlowComponent_wrongVersion_Conflict.1";

        try {
            // And...
            final FlowComponentContent content = new FlowComponentContentBuilder()
                    .setName("FlowComponentsIT.updateFlowComponent_wrongVersion_Conflict")
                    .build();
            FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(content);

            // And... First user updates the flow component
            flowStoreServiceConnector.updateFlowComponent(new FlowComponentContentBuilder()
                            .setName(FLOW_COMPONENT_NAME_FROM_FIRST_USER)
                            .build(),
                    flowComponent.getId(),
                    flowComponent.getVersion());

            // When... Second user attempts to update the same flow component
            flowStoreServiceConnector.updateFlowComponent(new FlowComponentContentBuilder()
                            .setName(FLOW_COMPONENT_NAME_FROM_SECOND_USER)
                            .build(),
                    flowComponent.getId(),
                    flowComponent.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateFlowComponent().");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(409));
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
     */
    @Test
    public void updateNextWithFlowComponentContent_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowComponentContent content = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.updateNextWithFlowComponentContent_ok")
                .build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(content);
        assertThat("created flowComponent.getNext() is null", flowComponent.getNext(), is(nullValue()));

        // When...
        final FlowComponentContent next = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.updateNextWithFlowComponentContent_ok.next")
                .setSvnRevision(2)
                .setInvocationJavascriptName("updatedInvocationJavascriptName")
                .build();
        FlowComponent updatedFlowComponent = flowStoreServiceConnector.updateNext(
                next, flowComponent.getId(), flowComponent.getVersion());

        // Then...
        assertNotNull("updated flowComponent not null", updatedFlowComponent);
        assertNotNull("updated flowComponent content not null", updatedFlowComponent.getContent());
        assertNotNull("updated flowComponent next not null", updatedFlowComponent.getNext());

        // And...
        assertThat("updated flowComponent id", updatedFlowComponent.getId(),
                is(flowComponent.getId()));

        // And...
        assertThat("updated flowComponent version", updatedFlowComponent.getVersion(),
                is(flowComponent.getVersion() + 1));

        // And...
        assertThat("next SVN revision", updatedFlowComponent.getNext().getSvnRevision(),
                is(next.getSvnRevision()));
        assertThat("next invocation javascript name", updatedFlowComponent.getNext().getInvocationJavascriptName(),
                is(next.getInvocationJavascriptName()));

        assertThat("flowComponent content SVN revision", updatedFlowComponent.getContent().getSvnRevision(),
                is(flowComponent.getContent().getSvnRevision()));
        assertThat("flowComponent content invocation javascript name", updatedFlowComponent.getContent().getInvocationJavascriptName(),
                is(flowComponent.getContent().getInvocationJavascriptName()));
    }

    /**
     * Given: a deployed flow-store service and a no referenced flow component is stored
     * When : attempting to delete the flow component
     * Then : the flow component is deleted
     */
    @Test
    public void deleteFlowComponent_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowComponentContent content = new FlowComponentContentBuilder()
                .setName("FlowComponentsIT.deleteFlowComponent_ok")
                .build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(content);

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
    public void deleteFlowComponent_noFlowComponentToDelete() {
        // Given...
        final long nonExistingflowComponentId = new Date().getTime();
        try {
            // When...
            flowStoreServiceConnector.deleteFlowComponent(nonExistingflowComponentId, 1);
            fail("None existing flow component was not detected");

            // Then ...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    private FlowComponentView getFlowComponentView(long flowComponentId) {
        try (PreparedStatement stmt = JDBCUtil.query(flowStoreDbConnection,
                "SELECT view FROM flow_components WHERE id=?", flowComponentId)) {
            final ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            return new JSONBContext().unmarshall(resultSet.getString(1), FlowComponentView.class);
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
