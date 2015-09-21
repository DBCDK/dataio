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
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        JsonUtil.class,
        ServiceUtil.class})
public class SinksBeanTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final long ID = 123;
    private static final long VERSION = 41L;
    private static final String ETAG_VALUE = Long.toString(VERSION);

    @Test
    public void sinksBean_validConstructor_newInstance() {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        assertThat(sinksBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createSink_nullSinkContent_throws() throws JsonException {
        newSinksBeanWithMockedEntityManager().createSink(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createSink_emptySinkContent_throws() throws JsonException {
        newSinksBeanWithMockedEntityManager().createSink(null, "");
    }

    @Test(expected = JsonException.class)
    public void createSink_invalidJSON_throwsJsonException() throws JsonException {
        newSinksBeanWithMockedEntityManager().createSink(null, "invalid Json");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSinks_noSinksFound_returnsResponseWithHttpStatusOkAndSinkEntity() throws JsonException {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final TypedQuery query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Sink.QUERY_FIND_ALL, Sink.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        final Response response = sinksBean.findAllSinks();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSinks_sinksFound_returnsResponseWithHttpStatusOkAndSinkEntities() throws JsonException {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final TypedQuery query = mock(TypedQuery.class);
        final String nameSinkA = "A";
        final Sink sinkA = new Sink();
        sinkA.setContent(new SinkContentJsonBuilder()
                .setName(nameSinkA)
                .build());
        final String nameSinkB = "B";
        final Sink sinkB = new Sink();
        sinkB.setContent(new SinkContentJsonBuilder()
                .setName(nameSinkB)
                .build());

        when(ENTITY_MANAGER.createNamedQuery(Sink.QUERY_FIND_ALL, Sink.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(sinkA, sinkB));

        final Response response = sinksBean.findAllSinks();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameSinkA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameSinkB));
    }

    @Test
    public void getSink_noSinkFound_returnsResponseWithHttpStatusNotFound() throws JsonException {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(null);

        Response response = sinksBean.getSink(ID);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntityTag(), is(nullValue()));
    }

    @Test
    public void getSink_sinkFound_returnsResponseWithHttpStatusOK() throws JsonException {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final Sink sink = new Sink();
        final String sinkName = "testSink";
        sink.setContent(new SinkContentJsonBuilder().setName(sinkName).build());
        sink.setVersion(VERSION);

        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(sink);

        Response response = sinksBean.getSink(ID);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = JsonUtil.getJsonRoot((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is(sinkName));
        assertThat(response.getEntityTag().getValue(), is(ETAG_VALUE));
    }

    @Test(expected = NullPointerException.class)
    public void updateSink_nullSinkContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newSinksBeanWithMockedEntityManager().updateSink(null, ID, VERSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateSink_emptySinkContent_throws() throws JsonException, ReferencedEntityNotFoundException {
        newSinksBeanWithMockedEntityManager().updateSink("", ID, VERSION);
    }

    @Test
    public void updateSink_sinkNotFound_returnsResponseWithHttpStatusNotFound() throws JsonException, ReferencedEntityNotFoundException {
        final String sinkContent = new SinkContentJsonBuilder().setName("UpdateContentName").build();
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(null);

        final Response response = sinksBean.updateSink(sinkContent, ID, VERSION);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateSink_sinkFound_returnsResponseWithHttpStatusOk_returnsSink() throws JsonException, ReferencedEntityNotFoundException {
        final Sink sink = mock(Sink.class);
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final String sinkContent = new SinkContentJsonBuilder().build();

        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(sink)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(sink);
        when(sink.getVersion()).thenReturn(VERSION);

        final Response response = sinksBean.updateSink(sinkContent, ID, VERSION);
        verify(sink).setContent(sinkContent);
        verify(sink).setVersion(VERSION);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(ETAG_VALUE));
    }


    @Test
    public void deleteSink_sinkNotFound_returnsResponseWithHttpStatusNotFound() throws JsonException, ReferencedEntityNotFoundException {
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(null);

        final Response response = sinksBean.deleteSink(ID, VERSION);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteSink_sinkFound_returnsNoContentHttpResponse() throws JsonException, ReferencedEntityNotFoundException {
        final Sink sink = mock(Sink.class);
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();

        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(sink)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Sink.class), any())).thenReturn(sink);
        when(ENTITY_MANAGER.merge(any(Sink.class))).thenReturn(sink);

        final Response response = sinksBean.deleteSink(ID, VERSION);

        verify(sink).setVersion(VERSION);
        verify(ENTITY_MANAGER).remove(sink);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test
    public void CreateSink_sinkCreated_returnsResponseWithHttpStatusOk_returnsSink() throws JsonException, ReferencedEntityNotFoundException {
        final UriInfo uriInfo = mock(UriInfo.class);
        final UriBuilder uriBuilder = mock(UriBuilder.class);
        final String sinkContent = new SinkContentJsonBuilder().build();
        final SinksBean sinksBean = newSinksBeanWithMockedEntityManager();
        final Sink sink = new Sink();
        sink.setVersion(VERSION);

        mockStatic(JsonUtil.class);
        mockStatic(ServiceUtil.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(JsonUtil.toJson(any(Sink.class))).thenReturn("sink");
        when(ServiceUtil.saveAsVersionedEntity(ENTITY_MANAGER, Sink.class, sinkContent)).thenReturn(sink);

        final Response response = sinksBean.createSink(uriInfo, sinkContent);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(ETAG_VALUE));
    }

    public static SinksBean newSinksBeanWithMockedEntityManager() {
        final SinksBean sinksBean = new SinksBean();
        sinksBean.entityManager = ENTITY_MANAGER;
        return sinksBean;
    }
}