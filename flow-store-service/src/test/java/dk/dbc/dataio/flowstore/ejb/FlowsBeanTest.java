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

package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowsBeanTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;

    private JSONBContext jsonbContext;
    private UriInfo mockedUriInfo;

    @Before
    public void setup() throws URISyntaxException {
        jsonbContext = new JSONBContext();

        mockedUriInfo = mock(UriInfo.class);
        final UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));
    }

    @Test
    public void flowsBean_validConstructor_newInstance() {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        assertThat(flowsBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createFlow_nullFlowContent_throws() throws JSONBException {
        newFlowsBeanWithMockedEntityManager().createFlow(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFlow_emptyFlowContent_throws() throws JSONBException {
        newFlowsBeanWithMockedEntityManager().createFlow(null, "");
    }

    @Test(expected = JSONBException.class)
    public void createFlow_invalidJSON_throwsJsonException() throws JSONBException {
        newFlowsBeanWithMockedEntityManager().createFlow(mockedUriInfo, "invalid Json");
    }

    @Test
    public void getFlow_noFlowFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);
        Response response = flowsBean.getFlow(1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getFlow_flowFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        final Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName("testFlow").build());
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);

        Response response = flowsBean.getFlow(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String)response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is("testFlow"));
    }

    @Test
    public void findFlows_findFlowByNameFlowFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        final Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName("testFlow").build());
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        final TypedQuery<Flow> query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_BY_NAME, Flow.class)).thenReturn(query);
        when(query.setParameter(1, "testFlow")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(flow));

        Response response = flowsBean.findFlows("testFlow");
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(1));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is("testFlow"));
    }

    @Test
    public void findFlows_findFlowByNameFlowNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName("testFlow").build());
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        final TypedQuery<Flow> query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_BY_NAME, Flow.class)).thenReturn(query);
        when(query.setParameter(1, "testFlow")).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        Response response = flowsBean.findFlows("testFlow");
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findFlows_findAllFlowsFlowsNotFound_returnsResponseWithHttpStatusOk() throws JSONBException {
        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_ALL, String.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        final Response response = flowsBean.findFlows(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findFlows_findAllFlowsFlowsFound_returnsResponseWithHttpStatusOk() throws JSONBException {

        final String nameFlowA = "A";
        final FlowView flowA = new FlowView()
                .withId(1)
                .withName(nameFlowA)
                .withDescription("Flow A description");
        final String nameFlowB = "B";
        final FlowView flowB = new FlowView()
                .withId(2)
                .withName(nameFlowB)
                .withDescription("Flow B description");

        final TypedQuery query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Flow.QUERY_FIND_ALL, String.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(jsonbContext.marshall(flowA), jsonbContext.marshall(flowB)));

        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        final Response response = flowsBean.findFlows(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("name").textValue(), is(nameFlowA));
        assertThat(entityNode.get(1).get("name").textValue(), is(nameFlowB));
    }

    @Test
    public void CreateFlow_flowCreated_returnsResponseWithHttpStatusOk_returnsFlow() throws JSONBException {

        final String flowContentString = new FlowContentJsonBuilder().setName("CreateContentName").build();
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        final Response response = flowsBean.createFlow(mockedUriInfo, flowContentString);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    public void refreshFlowComponents_flowComponentNotFound_throwsException() throws JSONBException, ReferencedEntityNotFoundException {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        flowsBean.jsonbContext = mock(JSONBContext.class);
        final Flow flow = mock(Flow.class);

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(flow.getContent()).thenReturn("{}");

        final dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
        final FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Collections.singletonList(flowComponent))
                .build();

        when(flowsBean.jsonbContext.unmarshall(anyString(), eq(FlowContent.class))).thenReturn(flowContent);
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        String flowContentJSON = new JSONBContext().marshall( flowContent);
        flowsBean.updateFlow(flowContentJSON, null, 123L, 4321L, true);
    }

    @Test
    public void refreshFlowComponents_flowNotFound_throwsException() throws JSONBException, ReferencedEntityNotFoundException {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        final Response response = flowsBean.updateFlow(createEmptyFlowContentJSON(), null, 123L, 4321L, true);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void refreshFlowComponents_flowFound_returnsResponseWithHttpStatusOk_returnsFlow() throws JSONBException, ReferencedEntityNotFoundException {
        final Flow flow = mock(Flow.class);
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        flowsBean.jsonbContext = mock(JSONBContext.class);

        when(flowsBean.jsonbContext.marshall(flow)).thenReturn("test");

        final dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
        final FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Collections.singletonList(flowComponent))
                .build();

        when(flowsBean.jsonbContext.unmarshall(anyString(), eq(FlowContent.class))).thenReturn(flowContent);
        when(flowsBean.jsonbContext.unmarshall(anyString(), eq(dk.dbc.dataio.commons.types.FlowComponent.class))).thenReturn(flowComponent);
        when(flowsBean.jsonbContext.marshall(eq(flowComponent))).thenReturn("test");

        final FlowComponent persistedFlowComponent = mock(FlowComponent.class);
        when(ENTITY_MANAGER.find(eq(dk.dbc.dataio.flowstore.entity.FlowComponent.class), any())).thenReturn(persistedFlowComponent);
        when(persistedFlowComponent.getVersion()).thenReturn(flowComponent.getVersion() +1);
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(flow.getContent()).thenReturn("{}");

        final Response response = flowsBean.updateFlow(createEmptyFlowContentJSON(), mockedUriInfo, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, true);

        verify(flow).setContent(flowsBean.jsonbContext.marshall(flowContent));
        verify(flow).setVersion(DEFAULT_TEST_VERSION);

        // Verifying that the private method invoked is: updateFlowComponentsInFlowToLatestVersion.
        // The other method: updateFlowContent does not invoke flow.getContent().
        verify(flow, times(1)).getContent();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void updateFlow_nullFlowContent_throws() throws JSONBException, ReferencedEntityNotFoundException {
        newFlowsBeanWithMockedEntityManager().updateFlow(null, null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateFlow_emptyFlowContent_throws() throws JSONBException, ReferencedEntityNotFoundException {
        newFlowsBeanWithMockedEntityManager().updateFlow("", null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
    }

    @Test
    public void updateFlow_flowNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException, ReferencedEntityNotFoundException {
        final String flowContent = new FlowContentJsonBuilder().setName("UpdateContentName").build();
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        final Response response = flowsBean.updateFlow(flowContent, null, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION, false);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlow_flowNotFound_returnsResponseWithHttpStatusNotFound() {
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(null);

        final Response response = flowsBean.deleteFlow(12L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlow_flowFound_returnsNoContentHttpResponse() {
        final Flow flow = mock(Flow.class);
        final FlowsBean flowsBean = newFlowsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Flow.class), any())).thenReturn(flow);
        when(ENTITY_MANAGER.merge(any(Flow.class))).thenReturn(flow);

        final Response response = flowsBean.deleteFlow(12L, 1L);

        verify(flow).setVersion(1L);
        verify(ENTITY_MANAGER).remove(flow);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }


    private String createEmptyFlowContentJSON( ) {
        try {
            final dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponentBuilder().build();
            final FlowContent flowContent = new FlowContentBuilder()
                    .setComponents(Collections.singletonList(flowComponent))
                    .build();
            return jsonbContext.marshall(flowContent);
        } catch (JSONBException e) {
            fail("Internal Error Shut not happen");
        }
        return "";
    }

    public static FlowsBean newFlowsBeanWithMockedEntityManager() {
        final FlowsBean flowsBean = new FlowsBean();
        flowsBean.entityManager = ENTITY_MANAGER;
        return flowsBean;
    }
}
