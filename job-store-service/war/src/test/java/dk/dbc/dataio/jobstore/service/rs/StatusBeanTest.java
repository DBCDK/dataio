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

package dk.dbc.dataio.jobstore.service.rs;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusBeanTest {
    private StatusBean statusBean;
    private JSONBContext jsonbContext;
    private Query query;
    private FlowStoreServiceConnector flowStoreServiceConnector;


    @Before
    public void setup() throws URISyntaxException {
        jsonbContext = new JSONBContext();
        query = mock(Query.class);
        initializeStatusBean();
        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        when(statusBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    // ************************************* getSinkStatusList() tests **********************************************************

    @Test
    public void getSinkStatusList_noSinksFound_returnsStatusOkResponseWithEmptyList() throws JSONBException, FlowStoreServiceConnectorException {
        final Response response = statusBean.getSinkStatusList();
        assertOkResponse(response);
        List<SinkStatusSnapshot> sinkStatusSnapshots = jsonbContext.unmarshall((String) response.getEntity(), getSinkStatusSnapshotListType());
        assertThat("ItemInfoSnapshots", sinkStatusSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots is empty", sinkStatusSnapshots.isEmpty(), is(true));
    }

    @Test
    public void getSinkStatusList_sinksFound_returnsStatusOkResponseWithSinkStatusSnapshotList() throws JSONBException, FlowStoreServiceConnectorException {
        final Sink sink = new SinkBuilder().setId(1).build();
        final SinkStatusSnapshot expectedSinkStatusSnapshot = new SinkStatusSnapshot()
                .withName(sink.getContent().getName())
                .withSinkType(sink.getContent().getSinkType()).withNumberOfJobs(1).withNumberOfChunks(2);

        when(flowStoreServiceConnector.getSink(1)).thenReturn(sink);
        when(flowStoreServiceConnector.findAllSinks()).thenReturn(Collections.singletonList(sink));
        when(statusBean.entityManager.createNamedQuery(DependencyTrackingEntity.JOB_COUNT_CHUNK_COUNT_QUERY)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new Object[]{1L, 2L});

        final Response response = statusBean.getSinkStatusList();
        assertOkResponse(response);

        final List<SinkStatusSnapshot> sinkStatusSnapshots = jsonbContext.unmarshall((String) response.getEntity(), getSinkStatusSnapshotListType());
        assertThat("SinkStatusSnapshots", sinkStatusSnapshots, is(notNullValue()));
        assertThat("SinkStatusSnapshots size", sinkStatusSnapshots.size(), is(1));
        assertThat("SinkStatusSnapshots element", sinkStatusSnapshots.get(0), is(expectedSinkStatusSnapshot));
    }

    // ***************************************** getSinkStatus() tests ***********************************************************

    @Test
    public void getSinkStatus_dependencyTrackingEntityFound_returnsStatusOkResponseWithSinkStatusSnapshot() throws JSONBException, FlowStoreServiceConnectorException {
        final Sink sink = new SinkBuilder().setId(1).build();
        final SinkStatusSnapshot expectedSinkStatusSnapshot = new SinkStatusSnapshot()
                .withName(sink.getContent().getName())
                .withSinkType(sink.getContent().getSinkType()).withNumberOfJobs(1).withNumberOfChunks(2);

        when(flowStoreServiceConnector.getSink(1)).thenReturn(sink);
        when(statusBean.entityManager.createNamedQuery(DependencyTrackingEntity.JOB_COUNT_CHUNK_COUNT_QUERY)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new Object[]{1L, 2L});

        Response response = statusBean.getSinkStatus(1);
        assertOkResponse(response);

        final SinkStatusSnapshot sinkStatusSnapshot = jsonbContext.unmarshall((String) response.getEntity(), SinkStatusSnapshot.class);
        assertThat("SinkStatusSnapshots", sinkStatusSnapshot, is(notNullValue()));
        assertThat("SinkStatusSnapshots element", sinkStatusSnapshot, is(expectedSinkStatusSnapshot));
    }

    @Test
    public void getSinkStatus_sinkNotFound_returnsStatusNotFoundResponse() throws FlowStoreServiceConnectorException, JSONBException {
        when(flowStoreServiceConnector.getSink(1))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("not found", Response.Status.NOT_FOUND.getStatusCode()));

        final Response response = statusBean.getSinkStatus(1);
        assertNotFoundResponse(response);
    }

    /*
     * Private methods
     */

    private void initializeStatusBean() {
        statusBean = new StatusBean();
        statusBean.jsonbContext = new JSONBContext();
        statusBean.entityManager = mock(EntityManager.class);
        statusBean.flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    }

    private void assertOkResponse(Response response) {
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
    }

    private void assertNotFoundResponse(Response response) {
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(false));
    }

    private CollectionType getSinkStatusSnapshotListType() {
        return jsonbContext.getTypeFactory().constructCollectionType(List.class, SinkStatusSnapshot.class);
    }
}
