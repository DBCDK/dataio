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
import dk.dbc.dataio.commons.utils.httpclient.FailSafeHttpClient;
import dk.dbc.dataio.commons.utils.httpclient.HttpDelete;
import dk.dbc.dataio.commons.utils.httpclient.HttpGet;
import dk.dbc.dataio.commons.utils.httpclient.HttpPost;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlowStoreServiceConnector_FlowComponents_Test {
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector =
            new FlowStoreServiceConnector(failSafeHttpClient, FLOW_STORE_URL);

    // ************************************* create flow component tests *************************************
    @Test
    public void createFlowComponent_flowComponentIsCreated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponent expectedFlowComponent = new FlowComponentBuilder().build();
        final FlowComponent flowComponent = createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.CREATED.getStatusCode(), expectedFlowComponent);
        assertThat(flowComponent, is(expectedFlowComponent));
    }

    @Test
    public void createFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void createFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    // Helper method
    private FlowComponent createFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();
        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(FLOW_STORE_URL)
                    .withPathElements(FlowStoreServiceConstants.FLOW_COMPONENTS)
                    .withJsonData(flowComponentContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
    }

    // *************************************** get flow component tests **************************************
    @Test
    public void getFlowComponent_flowComponentRetrieved_returnsFlowComponent() throws FlowStoreServiceConnectorException {
        final FlowComponent expectedFlowComponent = new FlowComponentBuilder().build();
        final FlowComponent flowComponent = getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), expectedFlowComponent, 1);
        assertThat(flowComponent, is(expectedFlowComponent));
    }

    @Test
    public void getFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, 1),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    // Helper method
    private FlowComponent getFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.getFlowComponent(id);
    }

    // ************************************** update flow component tests **************************************
    @Test
    public void updateFlowComponent_flowComponentIsUpdated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().build();
        final FlowComponent updatedFlowComponent = updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), flowComponentToUpdate, flowComponentToUpdate.getId(), flowComponentToUpdate.getVersion());
        assertThat(updatedFlowComponent, is(flowComponentToUpdate));
    }

    @Test
    public void updateFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void updateFlowComponent_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, 1, 1),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    // Helper method
    private FlowComponent updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder().build();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                .withJsonData(flowComponentContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateFlowComponent(flowComponentContent, id, version);
    }

    // ******************************************** update next tests ********************************************

    @Test
    public void updateNext_flowComponentIsUpdated_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponentContent next = new FlowComponentContentBuilder().setSvnRevision(34).build();
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().setNext(next).build();
        final FlowComponent updatedFlowComponent = updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), flowComponentToUpdate, flowComponentToUpdate.getId(), flowComponentToUpdate.getVersion());
        assertThat(updatedFlowComponent.getNext(), is(next));
    }

    @Test
    public void updateNext_flowComponentIsUpdatedWithNull_returnsFlowComponent() throws FlowStoreServiceConnectorException, JSONBException {
        final FlowComponent flowComponentToUpdate = new FlowComponentBuilder().setNext(null).build();
        final FlowComponent updatedFlowComponent = updateFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), flowComponentToUpdate, flowComponentToUpdate.getId(), flowComponentToUpdate.getVersion());
        assertThat(updatedFlowComponent.getNext(), is(nullValue()));
    }

    @Test
    public void updateNext_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateNext_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private FlowComponent updateNext_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final FlowComponentContent next = new FlowComponentContentBuilder().build();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT_NEXT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                .withJsonData(next);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateNext(next, id, version);
    }

    // *********************************** find all flow components tests ***********************************
    @Test
    public void findAllFlowComponents_flowComponentsRetrieved_returnsflowComponents() throws FlowStoreServiceConnectorException {
        final FlowComponentContent flowComponentContentA = new FlowComponentContentBuilder().setName("a").build();
        final FlowComponentContent flowComponentContentB = new FlowComponentContentBuilder().setName("b").build();
        final FlowComponent expectedFlowComponentResultA = new FlowComponentBuilder().setContent(flowComponentContentA).build();
        final FlowComponent expectedFlowComponentResultB = new FlowComponentBuilder().setContent(flowComponentContentB).build();

        final List<FlowComponent> expectedFlowComponentResultList = new ArrayList<>();
        expectedFlowComponentResultList.add(expectedFlowComponentResultA);
        expectedFlowComponentResultList.add(expectedFlowComponentResultB);

        final List<FlowComponent> flowComponentResultList = getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), expectedFlowComponentResultList);

        assertThat(flowComponentResultList, is(expectedFlowComponentResultList));
    }

    @Test
    public void findAllFlowComponents_noResults() throws FlowStoreServiceConnectorException {
        final List<FlowComponent> flowComponentResultList = getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), new ArrayList<FlowComponent>());
        assertThat(flowComponentResultList, is(Collections.emptyList()));
    }

    @Test
    public void findAllFlowComponents_responseWithNullEntity() throws FlowStoreServiceConnectorException {
        assertThat(() -> getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    @Test
    public void findAllFlowComponents_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private List<FlowComponent> getAllFlowComponents_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(FLOW_STORE_URL)
                    .withPathElements(FlowStoreServiceConstants.FLOW_COMPONENTS);

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.findAllFlowComponents();
    }

    // **************************************** delete flow component tests ****************************************
    @Test
    public void deleteFlowComponent_flowComponentIsDeleted() throws FlowStoreServiceConnectorException {
        deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), 1, 1);
    }

    @Test
    public void deleteFlowComponent_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // Helper method
    private void deleteFlowComponent_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id, long version) throws FlowStoreServiceConnectorException {
        final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        final HttpDelete httpDelete = new HttpDelete(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(pathBuilder.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version));

        when(failSafeHttpClient.execute(httpDelete))
                .thenReturn(new MockedResponse<>(statusCode, null));

        flowStoreServiceConnector.deleteFlowComponent(id, version);
    }
}
