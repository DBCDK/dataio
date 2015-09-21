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

import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,})
public class FlowStoreServiceConnector_FlowBinders_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // **************************************** create flow binder tests ****************************************
    @Test(expected = NullPointerException.class)
    public void createFlowBinder_flowBinderContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlowBinder(null);
    }

    @Test
    public void createFlowBinder_flowBinderIsCreated_returnsFlowBinder() throws FlowStoreServiceConnectorException, JsonException {
        final FlowBinder expectedFlowBinder = new FlowBinderBuilder().build();
        final FlowBinder flowBinder = createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlowBinder);
        assertThat(flowBinder, is(notNullValue()));
        assertThat(flowBinder.getId(), is(expectedFlowBinder.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlowBinder_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
     public void createFlowBinder_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlowBinder_responseWithPreconditionFailed_throws() throws FlowStoreServiceConnectorException {
        createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.PRECONDITION_FAILED.getStatusCode(), "");
    }

    // Helper method
    private FlowBinder createFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowBinderContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_BINDERS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createFlowBinder(flowBinderContent);
    }

    // *************************************** find all flow binders tests **************************************
    @Test
    public void findAllFlowBinders_flowBindersRetrieved_returnsFlowBinder() throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContentA = new FlowBinderContentBuilder().setName("a").build();
        final FlowBinderContent flowBinderContentB = new FlowBinderContentBuilder().setName("b").build();
        final FlowBinder expectedFlowBinderResultA = new FlowBinderBuilder().setContent(flowBinderContentA).build();
        final FlowBinder expectedFlowBinderResultB = new FlowBinderBuilder().setContent(flowBinderContentB).build();
        List<FlowBinder> expectedFlowBinderResultList = Arrays.asList(expectedFlowBinderResultA, expectedFlowBinderResultB);
        final List<FlowBinder> flowBinderResultList = findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowBinderResultList);

        assertThat(flowBinderResultList, not(nullValue()));
        assertFalse(flowBinderResultList.isEmpty());
        assertThat(flowBinderResultList.size(), is(2));
        assertThat(flowBinderResultList.get(0), is(notNullValue()));
        assertThat(flowBinderResultList.get(1), is(notNullValue()));
    }

    @Test
    public void findAllFlowBinders_noResults() throws FlowStoreServiceConnectorException {
        List<FlowBinder> flowBinderResultList = findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<FlowBinder>());
        assertThat(flowBinderResultList, is(notNullValue()));
        assertThat(flowBinderResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowBinders_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlowBinders_noListReturned() throws FlowStoreServiceConnectorException {
        findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlowBinders_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<FlowBinder> findAllFlowBinders_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, FlowStoreServiceConstants.FLOW_BINDERS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllFlowBinders();
    }

    // *************************************** get flow binder tests **************************************
    @Test
    public void getFlowBinder_flowBinderExist_flowBinderReturned() throws FlowStoreServiceConnectorException {
        final String FLOW_BINDER_NAME = "This one is the correct one";
        final FlowBinder expectedFlowBinder = createFlowBinder(FLOW_BINDER_NAME);
        final long expectedFlowBinderId = expectedFlowBinder.getId();
        setupHttpClientForGetFlowBinder(expectedFlowBinderId, Response.Status.OK.getStatusCode(), expectedFlowBinder);

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        final FlowBinder resultingFlowBinder = instance.getFlowBinder(expectedFlowBinderId);

        assertThat(resultingFlowBinder, not(nullValue()));
        assertThat(resultingFlowBinder.getId(), is(expectedFlowBinderId));
        assertThat(resultingFlowBinder.getContent().getName(), is(FLOW_BINDER_NAME));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowBinder_nullFlowBinder_throws() throws FlowStoreServiceConnectorException {
        final String FLOW_BINDER_NAME = "This one is the correct one";
        final FlowBinder expectedFlowBinder = createFlowBinder(FLOW_BINDER_NAME);
        final long expectedFlowBinderId = expectedFlowBinder.getId();
        setupHttpClientForGetFlowBinder(expectedFlowBinderId, Response.Status.OK.getStatusCode(), null);

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlowBinder(expectedFlowBinderId);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowBinder_flowBinderDoesNotExist_throws() throws FlowStoreServiceConnectorException {
        final long flowBinderId = 73L;
        setupHttpClientForGetFlowBinder(flowBinderId, Response.Status.NOT_FOUND.getStatusCode(), null);

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlowBinder(flowBinderId);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowBinder_internalServerError_throws() throws FlowStoreServiceConnectorException {
        final long flowBinderId = 73L;
        setupHttpClientForGetFlowBinder(flowBinderId, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null);

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.getFlowBinder(flowBinderId);
    }

    // **************************************** update flow binder tests ****************************************
    @Test
    public void updateFlowBinder_flowBinderIsUpdated_returnsFlowBinder() throws FlowStoreServiceConnectorException, JsonException {
        final FlowBinder flowBinderToUpdate = new FlowBinderBuilder().build();

        FlowBinder updatedFlowBinder = updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(),
                flowBinderToUpdate,
                flowBinderToUpdate.getId(),
                flowBinderToUpdate.getVersion());

        assertThat(updatedFlowBinder, not(nullValue()));
        assertThat(updatedFlowBinder.getContent(), is(notNullValue()));
        assertThat(updatedFlowBinder.getId(), is(flowBinderToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowBinder_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException{
        updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowBinder_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException{
        updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlowBinder_responseWithReferencedObjectNotFound_throws() throws FlowStoreServiceConnectorException{
        updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.PRECONDITION_FAILED.getStatusCode(), "", ID, VERSION);
    }

    // **************************************** delete flow binder tests ****************************************
    @Test
    public void deleteFlowBinder_flowBinderIsDeleted() throws FlowStoreServiceConnectorException, JsonException {
        final FlowBinder flowBinderToDelete = new FlowBinderBuilder().build();
        deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), flowBinderToDelete.getId(), flowBinderToDelete.getVersion());
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void deleteFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void deleteFlowBinder_responseWithVersionConflict_throws() throws FlowStoreServiceConnectorException{
        deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void deleteFlowBinder_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), ID, VERSION);
    }

    // Helper method
    private void deleteFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id, long version) throws FlowStoreServiceConnectorException {
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER)
                .bind(FlowStoreServiceConstants.FLOW_BINDER_ID_VARIABLE, Long.toString(id));

        when(HttpClient.doDelete(CLIENT, headers, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.deleteFlowBinder(id, version);
    }



    // **************************************** get flow binder by search index tests ****************************************
    @Test
    public void getFlowBinder_flowBinderRetrieved_returnsFlowBinder() throws FlowStoreServiceConnectorException {
        final FlowBinder expectedFlowBinderResult = new FlowBinderBuilder().build();
        final FlowBinder flowBinderResult = getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowBinderResult);
        assertThat(flowBinderResult, is(notNullValue()));
        assertThat(flowBinderResult.getId(), is(expectedFlowBinderResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowBinder_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlowBinder_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getFlowBinder_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    @SuppressWarnings("unchecked")
    private FlowBinder getFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), anyMap(), eq(FLOW_STORE_URL), eq(FlowStoreServiceConstants.FLOW_BINDER_RESOLVE)))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getFlowBinder("packaging", "format", "charset", ID, "destination");
    }


    // Helper method
    private FlowBinder updateFlowBinder_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER_CONTENT)
                .bind(FlowStoreServiceConstants.FLOW_BINDER_ID_VARIABLE, Long.toString(id));
        when(HttpClient.doPostWithJson(CLIENT, headers, flowBinderContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateFlowBinder(flowBinderContent, id, version);
    }

    // Helper method
    private FlowBinder createFlowBinder(String name) {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().setName(name).build();
        return new FlowBinderBuilder().setContent(flowBinderContent).build();
    }

    private void setupHttpClientForGetFlowBinder(long flowBinderId, int expectedErrorCode, FlowBinder expectedResult) {
        final String[] url = {FlowStoreServiceConstants.FLOW_BINDER.replaceFirst("/\\{id\\}", ""), String.valueOf(flowBinderId)}; // Eg.: {"binder", "62"}
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, url)).thenReturn(new MockedResponse<>(expectedErrorCode, expectedResult));
    }

}
