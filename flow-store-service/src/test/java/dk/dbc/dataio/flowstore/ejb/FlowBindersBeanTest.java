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
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.flowstore.entity.FlowBinderSearchIndexEntry;
import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceUtil.class})
public class FlowBindersBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;
    private static final String DEFAULT_TEST_ETAG_VALUE = Long.toString(DEFAULT_TEST_VERSION);

    private JSONBContext jsonbContext = new JSONBContext();

    @Test(expected = NullPointerException.class)
    public void getFlow_nullPackagingParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder(null, "nmalbum", "utf8", 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyPackagingParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("", "nmalbum", "utf8", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullFormatParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", null, "utf8", 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyFormatParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "", "utf8", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullCharsetParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", null, 654321L, "someDestination");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyCharsetParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "", 654321L, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullSubmitterParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "utf8", null, "someDestination");
    }

    @Test(expected = NullPointerException.class)
    public void getFlow_nullDestinationParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFlow_emptyDestinationParameter_throws() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, "");
    }

    @Test
    public void getFlow_validParametersButNoMatchingFlow_returnsResponseWithFlowAndHTTP404() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        Query query = mock(Query.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOWBINDER)).thenReturn(query);

        when(query.getResultList()).thenReturn(Collections.emptyList());

        Query query1 = mock(Query.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_BY_SUBMITTER)).thenReturn(query1);
        when(query1.getResultList()).thenReturn(Collections.emptyList());
        Response response = fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, "someDestination");
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getFlow_validParametersAndMatchingFlow_returnsResponseWithFlowAndHTTP200() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        Query query = mock(Query.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOWBINDER)).thenReturn(query);

        String flowBinderStr = new FlowBinderJsonBuilder().build();
        dk.dbc.dataio.commons.types.FlowBinder flowBinder = jsonbContext.unmarshall(flowBinderStr, dk.dbc.dataio.commons.types.FlowBinder.class);

        when(query.getResultList()).thenReturn(Collections.singletonList(flowBinder));
        Response response = fbb.getFlowBinder("xml", "nmalbum", "utf8", 654321L, "someDestination");
        assertThat(response.getStatus(), is(200));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getAllFlowBinders_validParametersAndMatchingFlow_returnsResponseWithFlowAndHTTP200() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        TypedQuery<dk.dbc.dataio.commons.types.FlowBinder> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(FlowBinder.QUERY_FIND_ALL, dk.dbc.dataio.commons.types.FlowBinder.class)).thenReturn(query);

        String flowBinderStr = new FlowBinderJsonBuilder().build();
        dk.dbc.dataio.commons.types.FlowBinder flowBinder = jsonbContext.unmarshall(flowBinderStr, dk.dbc.dataio.commons.types.FlowBinder.class);

        when(query.getResultList()).thenReturn(Collections.singletonList(flowBinder));
        Response response = fbb.findAllFlowBinders();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void getFlowBinder_flowBinderNotFound_returnsResponse404() throws JSONBException {
        final long FLOW_BINDER_ID = 12L;
        FlowBindersBean bean = new FlowBindersBean();
        EntityManager mockedEntityManager = mock(EntityManager.class);
        bean.entityManager = mockedEntityManager;
        when(mockedEntityManager.find(FlowBinder.class, FLOW_BINDER_ID)).thenReturn(null);  // null => Not found

        Response response = bean.getFlowBinderById(FLOW_BINDER_ID);

        assertThat(response.getStatus(), is(404));
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
        final Query query = mock(Query.class);

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(flowBinder);
        when(ENTITY_MANAGER.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER)).thenReturn(query);
        when(query.setParameter(FlowBinder.DB_QUERY_PARAMETER_FLOWBINDER, flowBinder.getId())).thenThrow(new IllegalArgumentException());

        final Response response = flowBindersBean.updateFlowBinder(flowBinderContent, 1L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    @SuppressWarnings("unchecked")
    public void updateFlowBinder_referencedSinkNotFound_throws() throws JSONBException, ReferencedEntityNotFoundException {

        final String flowBinderContentJson = new FlowBinderContentJsonBuilder().build();

        final Query query = mock(Query.class);
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any(Long.class))).thenReturn(new FlowBinder());
        when(ENTITY_MANAGER.find(eq(Flow.class), anyLong())).thenReturn(new Flow());
        when(ENTITY_MANAGER.find(eq(Sink.class), anyLong())).thenReturn(null);

        when(ENTITY_MANAGER.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new FlowBinderSearchIndexEntry()));

        flowBindersBean.updateFlowBinder(flowBinderContentJson, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    @SuppressWarnings("unchecked")
    public void updateFlowBinder_referencedFlowNotFound_throws() throws JSONBException, ReferencedEntityNotFoundException {

        final String flowBinderContentJson = new FlowBinderContentJsonBuilder().build();

        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        final Query query = mock(Query.class);

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any(Long.class))).thenReturn(new FlowBinder());
        when(ENTITY_MANAGER.find(eq(Flow.class), anyLong())).thenReturn(null);

        when(ENTITY_MANAGER.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new FlowBinderSearchIndexEntry()));

        flowBindersBean.updateFlowBinder(flowBinderContentJson, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    @SuppressWarnings("unchecked")
    public void updateFlowBinder_referencedSubmittersNotFound_throws() throws JSONBException, ReferencedEntityNotFoundException {

        final String flowBinderContentJson = new FlowBinderContentJsonBuilder().build();
        final Query query = mock(Query.class);
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any(Long.class))).thenReturn(new FlowBinder());
        when(ENTITY_MANAGER.find(eq(Flow.class), anyLong())).thenReturn(new Flow());
        when(ENTITY_MANAGER.find(eq(Sink.class), anyLong())).thenReturn(new Sink());
        when(ENTITY_MANAGER.find(eq(Submitter.class), anyLong())).thenReturn(null);

        when(ENTITY_MANAGER.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new FlowBinderSearchIndexEntry()));

        flowBindersBean.updateFlowBinder(flowBinderContentJson, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateFlowBinder_flowBinderFound_returnsResponseWithHttpStatusOk_returnsFlowBinder() throws JSONBException, ReferencedEntityNotFoundException {

        final Submitter submitter = new Submitter();
        String submitterContentJson = new SubmitterContentJsonBuilder().build();
        submitter.setContent(submitterContentJson);

        final String flowBinderContentJson = new FlowBinderContentJsonBuilder().build();

        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        flowBindersBean.jsonbContext = mock(JSONBContext.class);
        final Query query = mock(Query.class);

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any(Long.class))).thenReturn(new FlowBinder());
        when(ENTITY_MANAGER.find(eq(Flow.class), anyLong())).thenReturn(new Flow());
        when(ENTITY_MANAGER.find(eq(Sink.class), anyLong())).thenReturn(new Sink());
        when(ENTITY_MANAGER.find(eq(Submitter.class), anyLong())).thenReturn(submitter);

        when(flowBindersBean.jsonbContext.unmarshall(eq(flowBinderContentJson), eq(FlowBinderContent.class))).thenReturn(new FlowBinderContentBuilder().build());
        when(flowBindersBean.jsonbContext.unmarshall(eq(submitterContentJson), eq(SubmitterContent.class))).thenReturn(new SubmitterContentBuilder().build());
        when(flowBindersBean.jsonbContext.marshall(anyString())).thenReturn(new FlowBinderJsonBuilder().build());

        when(ENTITY_MANAGER.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new FlowBinderSearchIndexEntry()));

        final Response response = flowBindersBean.updateFlowBinder(flowBinderContentJson, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    public void deleteFlowBinder_flowBinderNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException, ReferencedEntityNotFoundException {
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(null);

        final Response response = flowBindersBean.deleteFlowBinder(12L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlowBinder_flowBinderFound_returnsNoContentHttpResponse() throws JSONBException, ReferencedEntityNotFoundException {
        final FlowBinder flowBinder = mock(FlowBinder.class);
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        Query mockedQuery = mock(Query.class);

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(flowBinder);
        when(ENTITY_MANAGER.merge(any(FlowBinder.class))).thenReturn(flowBinder);
        when(ENTITY_MANAGER.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER)).thenReturn(mockedQuery);
        when(mockedQuery.getResultList()).thenReturn(Collections.singletonList(new FlowBinderSearchIndexEntry()));

        final Response response = flowBindersBean.deleteFlowBinder(12L, 1L);

        verify(flowBinder).setVersion(1L);
        verify(ENTITY_MANAGER).remove(flowBinder);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test
    public void deleteFlowBinder_errorWhileSettingParametersForQuery_returnsResponseWithHttpStatusNotFound() throws JSONBException, ReferencedEntityNotFoundException {
        final FlowBinder flowBinder = mock(FlowBinder.class);
        final FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        final Query mockedQuery = mock(Query.class);

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(flowBinder);
        when(ENTITY_MANAGER.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER)).thenReturn(mockedQuery);
        when(mockedQuery.setParameter(FlowBinder.DB_QUERY_PARAMETER_FLOWBINDER, flowBinder.getId())).thenThrow(new IllegalArgumentException());

        final Response response = flowBindersBean.deleteFlowBinder(1L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getFlowBinder_flowBinderFound_returnsResponseWithFlowAndHTTP200() throws JSONBException {
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
        flowBinder.setSink(testSink());
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

    private Sink testSink() throws JSONBException {
        final Sink sink = new Sink();
        sink.setContent(new SinkContentJsonBuilder().build());
        return sink;
    }

    private Set<Submitter> testSubmitters() throws JSONBException {
        Submitter submitter = new Submitter();
        submitter.setContent(new SubmitterContentJsonBuilder().build());
        return new HashSet<>(Collections.singletonList(submitter));
    }

}