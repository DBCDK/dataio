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
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.flowstore.entity.SinkEntity;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.flowstore.model.FlowBinderContentMatch;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowBindersBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;

    private JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void test() {
        final FlowBinderContentMatch flowBinderContentMatch = new FlowBinderContentMatch()
                .withSubmitterIds(Collections.singletonList(42L));

        System.out.println(flowBinderContentMatch.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getAllFlowBinders_validParametersAndMatchingFlow_returnsResponseWithFlowAndHTTP200() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        TypedQuery<dk.dbc.dataio.commons.types.FlowBinder> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(FlowBinder.FIND_ALL_QUERY_NAME, dk.dbc.dataio.commons.types.FlowBinder.class)).thenReturn(query);

        String flowBinderStr = new FlowBinderJsonBuilder().build();
        dk.dbc.dataio.commons.types.FlowBinder flowBinder = jsonbContext.unmarshall(flowBinderStr, dk.dbc.dataio.commons.types.FlowBinder.class);

        when(query.getResultList()).thenReturn(Collections.singletonList(flowBinder));
        Response response = fbb.findAllFlowBinders();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void getFlowBinderById_flowBinderNotFound_returnsResponse404() throws JSONBException {
        final long FLOW_BINDER_ID = 12L;
        FlowBindersBean bean = new FlowBindersBean();
        EntityManager mockedEntityManager = mock(EntityManager.class);
        bean.entityManager = mockedEntityManager;
        when(mockedEntityManager.find(FlowBinder.class, FLOW_BINDER_ID)).thenReturn(null);  // null => Not found

        Response response = bean.getFlowBinderById(FLOW_BINDER_ID);

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getFlowBinderById_flowBinderFound_returnsResponseWithFlowAndHTTP200() throws JSONBException {
        final long FLOW_BINDER_ID = 12L;
        FlowBindersBean bean = new FlowBindersBean();
        EntityManager mockedEntityManager = mock(EntityManager.class);
        bean.entityManager = mockedEntityManager;
        FlowBinder flowBinder = testFlowBinder();
        when(mockedEntityManager.find(FlowBinder.class, FLOW_BINDER_ID)).thenReturn(flowBinder);

        Response response = bean.getFlowBinderById(FLOW_BINDER_ID);

        assertThat(response.getStatus(), is(200));
        assertThat(response.hasEntity(), is(true));
        JsonNode entity = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entity.get("version").asLong(), is(FLOW_BINDER_VERSION));
        assertThat(entity.get("content").get("flowId").asLong(), is(flowBinder.getFlowId()));
        assertTrue(flowBinder.getSubmitterIds().contains(entity.get("content").get("submitterIds").elements().next().asLong()));
        assertThat(entity.get("content").get("sinkId").asLong(), is((flowBinder.getSinkId())));
    }

    @Test(expected = NullPointerException.class)
    public void updateFlowBinder_nullFlowBinderContent_throws() throws JSONBException, ReferencedEntityNotFoundException {
        newFlowBindersBeanWithMockedEntityManager().updateFlowBinder(null, 1L, 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateFlowBinder_emptyFlowBinderContent_throws() throws JSONBException, ReferencedEntityNotFoundException {
        newFlowBindersBeanWithMockedEntityManager().updateFlowBinder("", 1L, 1L);
    }

    @Test
    public void updateFlowBinder_flowBinderNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException, ReferencedEntityNotFoundException {
        final String flowBinderContent = new FlowBinderContentJsonBuilder().setName("UpdateContentName").build();
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(null);

        final Response response = flowBindersBean.updateFlowBinder(flowBinderContent, 1L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateFlowBinder_errorWhileSettingParametersForQuery_returnsResponseWithHttpStatusNotFound() throws JSONBException, ReferencedEntityNotFoundException {
        final String flowBinderContent = new FlowBinderContentJsonBuilder().setName("UpdateContentName").build();
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        final FlowBinder flowBinder = mock(FlowBinder.class);

        when(ENTITY_MANAGER.find(FlowBinder.class, 0)).thenReturn(flowBinder);

        final Response response = flowBindersBean.updateFlowBinder(flowBinderContent, 1L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    public void updateFlowBinder_referencedSinkNotFound_throws() throws JSONBException, ReferencedEntityNotFoundException {
        final String flowBinderContentJson = new FlowBinderContentJsonBuilder().build();
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any(Long.class))).thenReturn(new FlowBinder());
        when(ENTITY_MANAGER.find(eq(Flow.class), anyLong())).thenReturn(new Flow());
        when(ENTITY_MANAGER.find(eq(SinkEntity.class), anyLong())).thenReturn(null);

        flowBindersBean.updateFlowBinder(flowBinderContentJson, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    public void updateFlowBinder_referencedFlowNotFound_throws() throws JSONBException, ReferencedEntityNotFoundException {
        final String flowBinderContentJson = new FlowBinderContentJsonBuilder().build();
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any(Long.class))).thenReturn(new FlowBinder());
        when(ENTITY_MANAGER.find(eq(Flow.class), anyLong())).thenReturn(null);

        flowBindersBean.updateFlowBinder(flowBinderContentJson, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    public void updateFlowBinder_referencedSubmittersNotFound_throws() throws JSONBException, ReferencedEntityNotFoundException {

        final String flowBinderContentJson = new FlowBinderContentJsonBuilder().build();
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any(Long.class))).thenReturn(new FlowBinder());
        when(ENTITY_MANAGER.find(eq(Flow.class), anyLong())).thenReturn(new Flow());
        when(ENTITY_MANAGER.find(eq(SinkEntity.class), anyLong())).thenReturn(new SinkEntity());
        when(ENTITY_MANAGER.find(eq(Submitter.class), anyLong())).thenReturn(null);

        flowBindersBean.updateFlowBinder(flowBinderContentJson, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
    }

    @Test
    public void deleteFlowBinder_flowBinderNotFound_returnsResponseWithHttpStatusNotFound() {
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(null);

        final Response response = flowBindersBean.deleteFlowBinder(12L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlowBinder_flowBinderFound_returnsNoContentHttpResponse() {
        final FlowBinder flowBinder = mock(FlowBinder.class);
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(flowBinder);
        when(ENTITY_MANAGER.merge(any(FlowBinder.class))).thenReturn(flowBinder);

        final Response response = flowBindersBean.deleteFlowBinder(12L, 1L);

        verify(flowBinder).setVersion(1L);
        verify(ENTITY_MANAGER).remove(flowBinder);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test
    public void deleteFlowBinder_errorWhileSettingParametersForQuery_returnsResponseWithHttpStatusNotFound() {
        final FlowBinder flowBinder = mock(FlowBinder.class);
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(FlowBinder.class, 0)).thenReturn(flowBinder);

        final Response response = flowBindersBean.deleteFlowBinder(1L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    private static FlowBindersBean newFlowBindersBeanWithMockedEntityManager() {
        final FlowBindersBean flowBindersBean = new FlowBindersBean();
        flowBindersBean.entityManager = ENTITY_MANAGER;
        return flowBindersBean;
    }

    final long FLOW_BINDER_VERSION = 1245L;
    private FlowBinder testFlowBinder() throws JSONBException {
        FlowBinder flowBinder = new FlowBinder();
        flowBinder.setVersion(FLOW_BINDER_VERSION);
        flowBinder.setFlow(testFlow());
        flowBinder.setSinkEntity(testSink());
        flowBinder.setContent(new FlowBinderContentJsonBuilder().build());
        flowBinder.setSubmitters(testSubmitters());
        return flowBinder;
    }

    final String TEST_FLOW_NAME = "Test flow name";
    private Flow testFlow() throws JSONBException {
        final Flow flow = new Flow();
        flow.setContent(new FlowContentJsonBuilder().setName(TEST_FLOW_NAME).build());
        return flow;
    }

    private SinkEntity testSink() throws JSONBException {
        final SinkEntity sinkEntity = new SinkEntity();
        sinkEntity.setContent(new SinkContentJsonBuilder().build());
        return sinkEntity;
    }

    private Set<Submitter> testSubmitters() throws JSONBException {
        Submitter submitter = new Submitter();
        submitter.setContent(new SubmitterContentJsonBuilder().build());
        return new HashSet<>(Collections.singletonList(submitter));
    }
}