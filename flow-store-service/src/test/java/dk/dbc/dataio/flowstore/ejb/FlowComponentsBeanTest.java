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
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowComponentsBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

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
    public void flowComponentsBean_validConstructor_newInstance() {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        assertThat(flowComponentsBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createFlowComponent_nullFlowComponentContent_throws() throws JSONBException {
        newFlowComponentsBeanWithMockedEntityManager().createComponent(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFlowComponent_emptyFlowComponentContent_throws() throws JSONBException {
        newFlowComponentsBeanWithMockedEntityManager().createComponent(null, "");
    }

    @Test(expected = JSONBException.class)
    public void createFlowComponent_invalidJSON_throwsJsonException() throws JSONBException {
        newFlowComponentsBeanWithMockedEntityManager().createComponent(null, "invalid Json");
    }

    @Test
    public void getFlowComponent_noFlowComponentFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);
        Response response = flowComponentsBean.getFlowComponent(1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getFlowComponent_flowComponentFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent(new FlowComponentContentJsonBuilder().setName("testFlowComponent").build());
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        Response response = flowComponentsBean.getFlowComponent(1L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is("testFlowComponent"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllFlowComponents_noFlowComponentsFound_returnsResponseWithHttpStatusOk() throws JSONBException {
        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(FlowComponent.QUERY_FIND_ALL, FlowComponent.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        final Response response = flowComponentsBean.findAllComponents();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllFlowComponents_FlowComponentsFound_returnsResponseWithHttpStatusOk() throws JSONBException {

        final String nameFlowComponentA = "A";
        final FlowComponent flowComponentA = new FlowComponent();
        flowComponentA.setContent(new FlowComponentContentJsonBuilder()
                .setInvocationJavascriptName("invocationJavascriptName")
                .setInvocationMethod("invocationMethod")
                .setJavascripts(new ArrayList<>())
                .setName(nameFlowComponentA)
                .setSvnProjectForInvocationJavascript("svnProjectForInvocationJavascript")
                .setSvnRevision(1L)
                .build());
        final String nameFlowComponentB = "B";
        final FlowComponent flowComponentB = new FlowComponent();
        flowComponentB.setContent(new FlowComponentContentJsonBuilder()
                .setInvocationJavascriptName("invocationJavascriptName")
                .setInvocationMethod("invocationMethod")
                .setJavascripts(new ArrayList<>())
                .setName(nameFlowComponentB)
                .setSvnProjectForInvocationJavascript("svnProjectForInvocationJavascript")
                .setSvnRevision(1L)
                .build());

        final TypedQuery query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(FlowComponent.QUERY_FIND_ALL, FlowComponent.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(flowComponentA, flowComponentB));

        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        final Response response = flowComponentsBean.findAllComponents();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameFlowComponentA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameFlowComponentB));
    }

    @Test
    public void CreateFlowComponent_flowComponentCreated_returnsResponseWithHttpStatusOk_returnsSink() throws JSONBException {
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final FlowComponentContent flowComponentContent = new FlowComponentContent("CreateContentName", "svnProjectForInvocationJavascript", 1L, "invocationJavascriptName", new ArrayList<JavaScript>(), "invocationMethod", "RequireCach");
        final String flowComponentContentString = new FlowComponentContentJsonBuilder().setName("CreateContentName").build();
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        JSONBContext mockedJSONBContext = mock(JSONBContext.class);
        when(mockedJSONBContext.unmarshall(flowComponentContentString, FlowComponentContent.class)).thenReturn(flowComponentContent);
        when(mockedJSONBContext.marshall(any(FlowComponent.class))).thenReturn("flowComponent");

        final Response response = flowComponentsBean.createComponent(uriInfo, flowComponentContentString);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void updateFlowComponent_nullFlowComponentContent_throws() throws JSONBException {
        newFlowComponentsBeanWithMockedEntityManager().updateFlowComponent(null, null, 0L, 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateFlowComponent_emptyFlowComponentContent_throws() throws JSONBException {
        newFlowComponentsBeanWithMockedEntityManager().updateFlowComponent(null, "", 0L, 0L);
    }

    @Test
    public void updateFlowComponent_flowComponentNotFound_throwsException() throws JSONBException {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = flowComponentsBean.updateFlowComponent(null, flowComponentContent, 123L, 4321L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateFlowComponent_flowComponentFound_returnsResponseWithHttpStatusOk_returnsFlowComponent() throws JSONBException {
        final FlowComponent flowComponent = mock(FlowComponent.class);
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        flowComponentsBean.jsonbContext = mock(JSONBContext.class);
        when(flowComponentsBean.jsonbContext.marshall(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = flowComponentsBean.updateFlowComponent(mockedUriInfo, flowComponentContent, 123L, 4321L);

        verify(flowComponent).setContent(flowComponentContent);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateNext_flowComponentNotFound_throwsException() throws JSONBException {
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(null);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().setName("UpdateContentName").build();
        final Response response = flowComponentsBean.updateNext(null, flowComponentContent, 123L, 4321L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }


    @Test
    public void updateNext_nextIsNull_returnsResponseWithHttpStatusOk() throws JSONBException {
        final FlowComponent flowComponent = mock(FlowComponent.class);
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        flowComponentsBean.jsonbContext = mock(JSONBContext.class);
        when(flowComponentsBean.jsonbContext.marshall(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        final Response response = flowComponentsBean.updateNext(mockedUriInfo, null, 123L, 4321L);

        verify(flowComponent).setNext(null);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void updateNext_nextIsNotNull_returnsResponseWithHttpStatusOk_returnsFlowComponent() throws JSONBException {
        final FlowComponent flowComponent = mock(FlowComponent.class);
        final FlowComponentsBean flowComponentsBean = newFlowComponentsBeanWithMockedEntityManager();

        flowComponentsBean.jsonbContext = mock(JSONBContext.class);
        when(flowComponentsBean.jsonbContext.marshall(flowComponent)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(FlowComponent.class), any())).thenReturn(flowComponent);

        final String flowComponentContent = new FlowComponentContentJsonBuilder().build();
        final Response response = flowComponentsBean.updateNext(mockedUriInfo, flowComponentContent, 123L, 4321L);

        verify(flowComponent).setNext(flowComponentContent);
        verify(flowComponent).setVersion(4321L);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    public static FlowComponentsBean newFlowComponentsBeanWithMockedEntityManager() {
        final FlowComponentsBean flowComponentsBean = new FlowComponentsBean();
        flowComponentsBean.entityManager = ENTITY_MANAGER;
        return flowComponentsBean;
    }

}
