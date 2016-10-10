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

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,})
public class FlowStoreServiceConnector_Flows_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // **************************************** create flow tests ****************************************
    @Test(expected = NullPointerException.class)
    public void createFlow_flowContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createFlow(null);
    }

    @Test
    public void createFlow_flowIsCreated_returnsFlow() throws FlowStoreServiceConnectorException, JSONBException {
        final Flow expectedFlow = new FlowBuilder().build();
        final Flow flow = createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedFlow);
        assertThat(flow, is(notNullValue()));
        assertThat(flow.getId(), is(expectedFlow.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createFlow_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    // Helper method
    private Flow createFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, flowContent, FLOW_STORE_URL, FlowStoreServiceConstants.FLOWS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createFlow(flowContent);
    }

    // **************************************** get flow tests ****************************************
    @Test
    public void getFlow_flowRetrieved_returnsFlow() throws FlowStoreServiceConnectorException {
        final Flow expectedFlowResult = new FlowBuilder().build();
        final Flow flowResult
                = getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowResult, ID);
        assertThat(flowResult, is(notNullValue()));
        assertThat(flowResult.getId(), is(expectedFlowResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getFlow_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, ID);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getFlow_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null, ID);
    }

    // Helper method
    private Flow getFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getFlow(id);
    }

    // ***************************************** update flow tests *****************************************
    @Test
    public void refreshFlowComponents_componentsInFlowAreUpdated_returnsFlow() throws FlowStoreServiceConnectorException, JSONBException {
        final Flow flowToUpdate = new FlowBuilder().build();
        Flow updatedFlow = refreshFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(),
                flowToUpdate,
                flowToUpdate.getId(),
                flowToUpdate.getVersion());
        assertThat(updatedFlow, is(notNullValue()));
        assertThat(updatedFlow.getContent(), is(notNullValue()));
        assertThat(updatedFlow.getId(), is(flowToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void refreshFlowComponents_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        refreshFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void refreshFlowComponents_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        refreshFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    // Helper method
    private Flow refreshFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final Map<String, Object> queryParameters = new HashMap<>(1);
        queryParameters.put(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH, true);

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        when(HttpClient.doPostWithJson(CLIENT, queryParameters, headers, "", FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.refreshFlowComponents(id, version);
    }

    @Test
    public void updateFlow_flowIsUpdated_returnsFlow() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowContent flowContent = new FlowContentBuilder().build();
        final Flow flowToUpdate = new FlowBuilder().build();

        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final Map<String, Object> queryParameters = new HashMap<>(1);
        queryParameters.put(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH, false);

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, flowToUpdate.getId());

        when(HttpClient.doPostWithJson(CLIENT, queryParameters, headers, flowContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowToUpdate));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        Flow updatedFlow = instance.updateFlow(flowContent, flowToUpdate.getId(), flowToUpdate.getVersion());

        assertThat(updatedFlow, is(notNullValue()));
        assertThat(updatedFlow.getContent(), is(notNullValue()));
        assertThat(updatedFlow.getId(), is(flowToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlow_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlow_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "", ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateFlow_responseWithFlowIDNotFound_throws() throws FlowStoreServiceConnectorException {
        updateFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), "", ID, VERSION);
    }

    // Helper method
    private Flow updateFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = new FlowContentBuilder().build();

        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final Map<String, Object> queryParameters = new HashMap<>(1);
        queryParameters.put(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH, false);

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        when(HttpClient.doPostWithJson(CLIENT, queryParameters, headers, flowContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateFlow(flowContent, id, version);
    }

    // ************************************* find all flows tests *************************************
    @Test
    public void findAllFlows_flowsRetrieved_returnsFlows() throws FlowStoreServiceConnectorException {
        final FlowContent flowContentA = new FlowContentBuilder().setName("a").build();
        final FlowContent flowContentB = new FlowContentBuilder().setName("b").build();
        final Flow expectedFlowResultA = new FlowBuilder().setContent(flowContentA).build();
        final Flow expectedFlowResultB = new FlowBuilder().setContent(flowContentB).build();
        List<Flow> expectedFlowResultList = Arrays.asList(expectedFlowResultA, expectedFlowResultB);
        final List<Flow> flowResultList = findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedFlowResultList);

        assertNotNull(flowResultList);
        assertFalse(flowResultList.isEmpty());
        assertThat(flowResultList.size(), is(2));
        assertThat(flowResultList.get(0), is(notNullValue()));
        assertThat(flowResultList.get(1), is(notNullValue()));
    }

    @Test
    public void findAllFlows_noResults() throws FlowStoreServiceConnectorException {
        List<Flow> flowResultList = findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<Flow>());
        assertThat(flowResultList, is(notNullValue()));
        assertThat(flowResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlows_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllFlows_noListReturned() throws FlowStoreServiceConnectorException {
        findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllFlows_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<Flow> findAllFlows_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), anyMap(), eq(FLOW_STORE_URL), eq(FlowStoreServiceConstants.FLOWS)))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllFlows();
    }

    // ************************************* find flow by name tests *************************************
    @Test
    public void findFlowByName_flowRetrieved_returnsFlow() throws FlowStoreServiceConnectorException {
        final String flowName = "testFlow";
        final Flow expectedFlowResult = new FlowBuilder().setContent(new FlowContentBuilder().setName(flowName).build()).build();
        final Flow flow = findFlowByName_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), Collections.singletonList(expectedFlowResult), flowName);
        assertThat(flow, is(expectedFlowResult));
    }

    @Test
    public void findFlowByName_noResults() throws FlowStoreServiceConnectorException {
        assertThat(() -> findFlowByName_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null, null),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findFlowByName_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findFlowByName_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    // Helper method
    private Flow findFlowByName_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, String queryParamName) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(eq(CLIENT), anyMap(), eq(FLOW_STORE_URL), eq(FlowStoreServiceConstants.FLOWS)))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findFlowByName(queryParamName);
    }

    // **************************************** delete flow tests ****************************************
    @Test
    public void deleteFlow_flowIsDeleted() throws FlowStoreServiceConnectorException, JSONBException {
        final Flow flowToDelete = new FlowBuilder().build();
        deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), flowToDelete.getId(), flowToDelete.getVersion());
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void deleteFlow_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void deleteFlow_responseWithVersionConflict_throws() throws FlowStoreServiceConnectorException{
        deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), ID, VERSION);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void deleteFlow_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), ID, VERSION);
    }

    // Helper method
    private void deleteFlow_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id, long version) throws FlowStoreServiceConnectorException {
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        when(HttpClient.doDelete(CLIENT, headers, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.deleteFlow(id, version);
    }
}
