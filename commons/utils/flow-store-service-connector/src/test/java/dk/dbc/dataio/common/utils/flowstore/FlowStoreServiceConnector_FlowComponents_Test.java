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

package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,})
public class FlowStoreServiceConnector_FlowComponents_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // ************************************* create flow component tests *************************************
    @Test(expected = NullPointerException.class)
    public void createFlowComponent_flowComponentContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowComponent(null);
    }

    @Test
    public void createFlowComponent_flowComponentIsCreated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponent expectedFlowComponent = new FlowComponentBuilder().build();
        final FlowComponent flowComponent
                = createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlowComponent);

        assertThat(flowComponent, is(notNullValue()));
        assertThat(flowComponent.getId(), is(expectedFlowComponent.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlowComponent_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    // Helper method
    private FlowComponent createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowComponentContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_COMPONENTS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createFlowComponent(flowComponentContent);
    }

    // *************************************** get flow component tests **************************************
    @Test
    public void getFlowComponent_flowComponentRetrieved_returnsFlowComponent() throws FlowStoreServiceConnectorException {
        final FlowComponent expectedFlowComponentResult = new FlowComponentBuilder().build();

        final FlowComponent flowComponentResult
                = getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowComponentResult, ID);

        assertThat(flowComponentResult, is(notNullValue()));
        assertThat(flowComponentResult.getId(), is(expectedFlowComponentResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getFlowComponent_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null, ID);
    }

    // Helper method
    private FlowComponent getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getFlowComponent(id);
    }

    // ************************************** update flow component tests **************************************
    @Test
    public void updateFlowComponent_flowComponentIsUpdated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().build();

        FlowComponent updatedFlowComponent
                = updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(),
                flowComponentToUpdate,
                flowComponentToUpdate.getId(),
                flowComponentToUpdate.getVersion());

        assertThat(updatedFlowComponent, is(notNullValue()));
        assertThat(updatedFlowComponent.getContent(), is(notNullValue()));
        assertThat(updatedFlowComponent.getId(), is(flowComponentToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponent_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowComponent_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    // Helper method
    private FlowComponent updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);
        when(HttpClient.doPostWithJson(CLIENT, headers, flowComponentContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateFlowComponent(flowComponentContent, id, version);
    }

    // ******************************************** update next tests ********************************************

    @Test
    public void updateNext_flowComponentIsUpdated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        FlowComponentContent next = new FlowComponentContentBuilder().setSvnRevision(34).build();
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().setNext(next).build();

        FlowComponent updatedFlowComponent
                = updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(),
                flowComponentToUpdate,
                flowComponentToUpdate.getId(),
                flowComponentToUpdate.getVersion());

        assertThat(updatedFlowComponent, is(notNullValue()));
        assertThat(updatedFlowComponent.getContent(), is(notNullValue()));
        assertThat(updatedFlowComponent.getId(), is(flowComponentToUpdate.getId()));
        assertThat(updatedFlowComponent.getNext(), is(notNullValue()));
        assertThat(updatedFlowComponent.getNext(), is(next));
    }

    @Test
    public void updateNext_flowComponentIsUpdatedWithNull_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().setNext(null).build();

        FlowComponent updatedFlowComponent
                = updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(),
                flowComponentToUpdate,
                flowComponentToUpdate.getId(),
                flowComponentToUpdate.getVersion());

        assertThat(updatedFlowComponent, is(notNullValue()));
        assertThat(updatedFlowComponent.getContent(), is(notNullValue()));
        assertThat(updatedFlowComponent.getId(), is(flowComponentToUpdate.getId()));
        assertThat(updatedFlowComponent.getNext(), is(nullValue()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateNext_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateNext_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateNext_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        updateNext_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateNext_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        updateNext_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    // Helper method
    private FlowComponent updateNext_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowComponentContent next = new FlowComponentContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT_NEXT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);
        when(HttpClient.doPostWithJson(CLIENT, headers, next, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateNext(next, id, version);
    }

    // *********************************** find all flow components tests ***********************************
    @Test
    public void findAllFlowComponents_flowComponentsRetrieved_returnsflowComponents() throws FlowStoreServiceConnectorException {

        final FlowComponentContent flowComponentContentA = new FlowComponentContentBuilder().setName("a").build();
        final FlowComponentContent flowComponentContentB = new FlowComponentContentBuilder().setName("b").build();
        final FlowComponent expectedFlowComponentResultA = new FlowComponentBuilder().setContent(flowComponentContentA).build();
        final FlowComponent expectedFlowComponentResultB = new FlowComponentBuilder().setContent(flowComponentContentB).build();

        List<FlowComponent> expectedFlowComponentResultList = new ArrayList<>();
        expectedFlowComponentResultList.add(expectedFlowComponentResultA);
        expectedFlowComponentResultList.add(expectedFlowComponentResultB);

        final List<FlowComponent> flowComponentResultList
                = getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowComponentResultList);

        assertNotNull(flowComponentResultList);
        assertFalse(flowComponentResultList.isEmpty());
        assertThat(flowComponentResultList.size(), is(2));

        for (FlowComponent flowComponent : flowComponentResultList) {
            assertThat(flowComponent, is(notNullValue()));
        }
    }

    @Test
    public void findAllFlowComponents_noResults() throws FlowStoreServiceConnectorException {
        List<FlowComponent> flowComponentResultList
                = getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<FlowComponent>());
        assertThat(flowComponentResultList, is(notNullValue()));
        assertThat(flowComponentResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowComponents_noListReturned() throws FlowStoreServiceConnectorException {
        getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowComponents_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlowComponents_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<FlowComponent> getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_COMPONENTS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllFlowComponents();
    }


    // **************************************** delete flow component tests ****************************************
    @Test
    public void deleteFlowComponent_flowComponentIsDeleted() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponent flowComponentToDelete = new FlowComponentBuilder().build();
        deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), flowComponentToDelete.getId(), flowComponentToDelete.getVersion());
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void deleteFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void deleteFlowComponent_responseWithVersionConflict_throws() throws FlowStoreServiceConnectorException{
        deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void deleteFlowComponent_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), ID, VERSION);
    }

    // Helper method
    private void deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id, long version) throws FlowStoreServiceConnectorException {
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        when(HttpClient.doDelete(CLIENT, headers, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.deleteFlowComponent(id, version);
    }

}
