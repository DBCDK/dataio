package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.httpclient.HttpClient;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration tests for the flows collection part of the flow store service
 */
public class FlowsIT extends AbstractFlowStoreServiceContainerTest {
    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path without an identifier
     * Then : a flow it created and returned
     * And  : assert that the flow created contains the same information as the flowContent given as input
     * And  : the flow view is updated
     */
    @Test
    public void createFlow_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.createFlow_ok")
                .build();

        // Then...
        Flow flow = flowStoreServiceConnector.createFlow(content);

        // And...
        assertThat(flow.getContent(), is(content));

        // And...
        final FlowView flowView = getFlowView(flow.getId());
        assertThat("flow view version", flowView.getVersion(), is(1L));
        assertThat("flow view name", flowView.getName(), is(content.getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the flows path
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createFlow_invalidJson_BadRequest() {
        // When...
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                "<invalid json />", flowStoreServiceBaseUrl, FlowStoreServiceConstants.FLOWS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow resource
     * When : adding flow with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlow_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent flowContent = new FlowContentBuilder()
                .setName("FlowsIT.createFlow_duplicateName_NotAcceptable")
                .build();

        try {
            flowStoreServiceConnector.createFlow(flowContent);
            // When...
            flowStoreServiceConnector.createFlow(flowContent);
            fail("Primary key violation was not detected as input to createFlow().");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
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
            flowStoreServiceConnector.getFlow(new Date().getTime());

            fail("Invalid request to getFlow() was not detected.");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with a valid identifier
     * Then : a flow is found and returned
     * And  : assert that the flow found contains the same information as the flow created
     */
    @Test
    public void getFlow_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.getFlow_ok")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(content);

        // Then...
        Flow flowToGet = flowStoreServiceConnector.getFlow(flow.getId());

        // And...
        assertThat(flowToGet.getContent(), is(content));
    }

    /**
     * Given: a deployed flow-store service containing three flows
     * When: GETing flows collection
     * Then: request returns with 3 flows
     * And: the flows are sorted alphabetically by name
     */
    @Test
    public void findAllFlows_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent contentA = new FlowContentBuilder()
                .setName("a_FlowsIT.findAllFlows_ok")
                .build();
        final FlowContent contentB = new FlowContentBuilder()
                .setName("b_FlowsIT.findAllFlows_ok")
                .build();
        final FlowContent contentC = new FlowContentBuilder()
                .setName("c_FlowsIT.findAllFlows_ok")
                .build();

        Flow flowSortsThird = flowStoreServiceConnector.createFlow(contentC);
        Flow flowSortsFirst = flowStoreServiceConnector.createFlow(contentA);
        Flow flowSortsSecond = flowStoreServiceConnector.createFlow(contentB);

        // When...
        List<FlowView> listOfFlowViews = flowStoreServiceConnector.findAllFlows();

        // Then...
        assertThat(listOfFlowViews.size() >= 3, is(true));

        // And...
        assertThat(listOfFlowViews.get(0).getName(),
                is(flowSortsFirst.getContent().getName()));
        assertThat(listOfFlowViews.get(1).getName(),
                is(flowSortsSecond.getContent().getName()));
        assertThat(listOfFlowViews.get(2).getName(),
                is(flowSortsThird.getContent().getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with a valid identifier
     * Then : a flow is found and returned
     * And  : assert that the flow found has an id, a version and contains the same information as the flow created
     */
    @Test
    public void findFlowByName_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.findFlowByName_ok")
                .build();

        // Then...
        Flow flow = flowStoreServiceConnector.createFlow(content);
        Flow flowToGet = flowStoreServiceConnector.findFlowByName(content.getName());

        // And...
        assertNotNull(flowToGet);
        assertThat(flowToGet.getContent(), is(content));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an none existing identifier
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void findFlowByName_notFound() throws FlowStoreServiceConnectorException {
        try {
            // When...
            flowStoreServiceConnector.findFlowByName("FlowsIT.findFlowByName_notFound");

            fail("Invalid request to findFlowByName() was not detected");
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
     */
    @Test
    public void refreshFlowComponents_ok() throws FlowStoreServiceConnectorException {
        // Given...
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(
                new FlowComponentContentBuilder()
                        .setName("FlowsIT.refreshFlowComponents_ok")
                        .build());

        // Create flow containing the flow component created above
        final FlowContent flowContent = new FlowContentBuilder()
                .setName("FlowsIT.refreshFlowComponents_ok")
                .setComponents(Collections.singletonList(flowComponent))
                .build();
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

        flowStoreServiceConnector.updateFlowComponent(
                updatedFlowComponentContent, flowComponent.getId(), flowComponent.getVersion());

        // When...
        // Update the flow component embedded within the flow to the latest svn revision
        Flow updatedFlow = flowStoreServiceConnector.refreshFlowComponents(flow.getId(), flow.getVersion());

        // Then...
        assertFlowContentEquals(true, updatedFlow.getContent(), flow.getContent());

        // And...
        assertThat(updatedFlow.getId(), is(flow.getId()));

        // And...
        assertThat(updatedFlow.getVersion(), is(flow.getVersion() + 1));
        assertThat(updatedFlow.getContent().getTimeOfFlowComponentUpdate(), is(notNullValue()));
    }

    /**
     * Given: a deployed flow-store service where a valid flow with given id is already stored
     * When : valid JSON is POSTed to the flow path with an identifier (update)
     * Then : assert that the timeOfFlowComponentUpdate has not been set
     */
    @Test
    public void refreshFlowComponents_noFlowComponentChanges_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName("FlowsIT.refreshFlowComponents_noFlowComponentChanges_ok")
                .build();
        FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        final FlowContent flowContent = new FlowContentBuilder()
                .setName("FlowsIT.refreshFlowComponents_noFlowComponentChanges_ok")
                .setComponents(Collections.singletonList(flowComponent))
                .build();
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
     */
    @Test
    public void refreshFlowComponents_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try{
            // When...
            flowStoreServiceConnector.refreshFlowComponents(new Date().getTime(), 1L);

            fail("Wrong flow Id was not detected as input to refreshFlowComponents().");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
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
     */
    @Test
    public void refreshFlowComponents_wrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        try {
            // Create flow component
            final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                    .setName("FlowsIT.refreshFlowComponents_wrongVersion_Conflict")
                    .build();
            FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

            final List<FlowComponent> flowComponents = new ArrayList<>();
            flowComponents.add(flowComponent);

            // Create flow containing the flow component created above
            final FlowContent flowContent = new FlowContentBuilder()
                    .setName("FlowsIT.refreshFlowComponents_wrongVersion_Conflict")
                    .setComponents(flowComponents)
                    .build();
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

            flowStoreServiceConnector.updateFlowComponent(
                    updatedFlowComponentContent, flowComponent.getId(), flowComponent.getVersion());

            // And... First user updates the flow
            flowStoreServiceConnector.refreshFlowComponents(flow.getId(), flow.getVersion());

            // When... Second user attempts to update the same flow
            flowStoreServiceConnector.refreshFlowComponents(flow.getId(), flow.getVersion());
            fail("Edit conflict, in the case of multiple updates, was not detected as input to refreshFlowComponents().");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(409));
        }
    }

    /*
     * Given: a deployed flow-store service where a valid flow with given id is already stored
     * When : valid JSON is POSTed to the flows path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the flow has not changed
     * And  : assert that the version number has been updated and that the timeOfFlowComponentUpdate has been been set
     *        as the nested flowComponents were unchanged
     * And  : the flow view is updated
     */
    @Test
    public void updateFlow_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.updateFlow_ok")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(content);

        // When...
        final FlowContent updatedContent = new FlowContentBuilder()
                .setName(content.getName())
                .setDescription("UpdatedDescription")
                .build();
        Flow updatedFlow = flowStoreServiceConnector.updateFlow(
                updatedContent, flow.getId(), flow.getVersion());

        // Then...
        assertFlowContentEquals(false, updatedFlow.getContent(), updatedContent);

        // And...
        assertThat(updatedFlow.getId(), is(flow.getId()));

        // And...
        assertThat(updatedFlow.getVersion(), is(flow.getVersion() + 1));
        assertThat(updatedFlow.getContent().getTimeOfFlowComponentUpdate(), is(nullValue()));

        // And...
        final FlowView flowView = getFlowView(flow.getId());
        assertThat("flow view version", flowView.getVersion(), is(flow.getVersion() + 1));
        assertThat("flow view description", flowView.getDescription(), is(updatedContent.getDescription()));
    }


    @Test
    public void updateFlow_setTimeOfFlowComponentUpdate_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.updateFlow_setTimeOfFlowComponentUpdate_ok")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(content);

        // When...
        final FlowComponent newFlowComponent = new FlowComponentBuilder()
                .setContent(new FlowComponentContentBuilder()
                        .setName(content.getName())
                        .setSvnRevision(123456L)
                        .build())
                .build();
        final FlowContent updatedContent = new FlowContentBuilder()
                .setName(content.getName())
                .setComponents(Collections.singletonList(newFlowComponent))
                .build();
        Flow updatedFlow = flowStoreServiceConnector.updateFlow(
                updatedContent, flow.getId(), flow.getVersion());

        // Then...
        assertFlowContentEquals(false, updatedFlow.getContent(), updatedContent);

        // And...
        assertThat(updatedFlow.getId(), is(flow.getId()));

        // And...
        assertThat(updatedFlow.getVersion(), is(flow.getVersion() + 1));
        assertThat(updatedFlow.getContent().getTimeOfFlowComponentUpdate(), is(notNullValue()));
    }

    /*
     * Given: a deployed flow-store service with a flow
     * When : JSON posted to the flows path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateFlow_invalidJson_BadRequest() throws FlowStoreServiceConnectorException {
        // Given ...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.updateFlow_invalidJson_BadRequest")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(content);

        // Assume, that the very first created flow has version number 1:
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");  // Set version = 1
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                headers, "<invalid json />", flowStoreServiceBaseUrl,
                FlowStoreServiceConstants.FLOWS, Long.toString(flow.getId()), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /*
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flows exist in the underlying database
     */
    @Test
    public void updateFlow_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try {
            // When...
            final FlowContent content = new FlowContentBuilder()
                    .setName("FlowsIT.updateFlow_wrongIdNumber_NotFound")
                    .build();
            flowStoreServiceConnector.updateFlow(content, new Date().getTime(), 1L);

            fail("Wrong flow Id was not detected as input to updateFlow()");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    /*
     * Given: a deployed flow-store service where two valid flows are already stored
     * When : valid JSON is POSTed to the flows path with an identifier (update) but with a flow name that is already in use by another existing flow
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void updateFlow_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        final String FIRST_FLOW_NAME = "FlowsIT.updateFlow_duplicateName_NotAcceptable.1";
        final String SECOND_FLOW_NAME = "FlowsIT.updateFlow_duplicateName_NotAcceptable.2";

        try {
            final FlowContent content1 = new FlowContentBuilder()
                    .setName(FIRST_FLOW_NAME)
                    .setDescription("UpdatedDescription1")
                    .build();
            flowStoreServiceConnector.createFlow(content1);

            final FlowContent content2 = new FlowContentBuilder()
                    .setName(SECOND_FLOW_NAME)
                    .setDescription("UpdatedDescription2")
                    .build();
            Flow flow = flowStoreServiceConnector.createFlow(content2);

            // When... (Attempting to save the second flow created with the same name as the first flow created)
            flowStoreServiceConnector.updateFlow(content1, flow.getId(), flow.getVersion());

            fail("Primary key violation was not detected as input to updateFlow()");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
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
    public void updateFlow_wrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final String FLOW_NAME_FROM_FIRST_USER = "FlowsIT.updateFlow_wrongVersion_Conflict.1";
        final String FLOW_NAME_FROM_SECOND_USER = "FlowsIT.updateFlow_wrongVersion_Conflict.2";

        try {
            Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                    .setName("FlowsIT.updateFlow_wrongVersion_Conflict")
                    .build());

            // And... First user updates the flow
            FlowContent content1 = new FlowContentBuilder()
                    .setName(FLOW_NAME_FROM_FIRST_USER)
                    .build();
            flowStoreServiceConnector.updateFlow(content1, flow.getId(), flow.getVersion());

            // When... Second user attempts to update the same flow
            FlowContent content2 = new FlowContentBuilder()
                    .setName(FLOW_NAME_FROM_SECOND_USER)
                    .build();
            flowStoreServiceConnector.updateFlow(content2, flow.getId(), flow.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateFlow()");

            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
        }
    }

    /**
     * Given: a deployed flow-store service and a none referenced flow is stored
     * When : attempting to delete the flow
     * Then : the flow is deleted
     */
    @Test
    public void deleteFlow_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent flowContent = new FlowContentBuilder()
                .setName("FlowsIT.deleteFlow_ok")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // When...
        flowStoreServiceConnector.deleteFlow(flow.getId(), flow.getVersion());

        // Then... Verify that the flow is deleted
        try {
            flowStoreServiceConnector.getFlow(flow.getId());
            fail("Flow was not deleted");
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
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
    public void deleteFlow_noFlowToDelete() {
        // Given...
        final long nonExistingFlowId = new Date().getTime();

        try {
            // When...
            flowStoreServiceConnector.deleteFlow(nonExistingFlowId, 1L);
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
    public void deleteFlow_optimisticLocking() throws FlowStoreServiceConnectorException {

        // Given...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.deleteFlow_optimisticLocking")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(content);
        long versionFirst = flow.getVersion();
        long versionSecond = versionFirst + 1;

        // And
        final FlowContent updatedContent = new FlowContentBuilder()
                .setName(content.getName())
                .setDescription("updated")
                .build();
        final Flow updatedFlow = flowStoreServiceConnector.updateFlow(
                updatedContent, flow.getId(), versionFirst);
        assertThat(updatedFlow.getVersion(), is(versionSecond));

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
    public void deleteFlow_flowBinderExists_Conflict() throws FlowStoreServiceConnectorException {
        // Given
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
                .setNumber(new Date().getTime())
                .build());

        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
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

    private FlowView getFlowView(long flowId) {
        try (PreparedStatement stmt = JDBCUtil.query(flowStoreDbConnection,
                "SELECT view FROM flows WHERE id=?", flowId)) {
            final ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            return new JSONBContext().unmarshall(resultSet.getString(1), FlowView.class);
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
