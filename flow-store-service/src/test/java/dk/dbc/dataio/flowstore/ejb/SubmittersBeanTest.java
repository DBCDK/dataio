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
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.flowstore.entity.Submitter;
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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SubmittersBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Long DEFAULT_TEST_ID = 23L;
    private static final Long DEFAULT_TEST_VERSION = 4L;
    private static final String DEFAULT_TEST_ETAG_VALUE = Long.toString(DEFAULT_TEST_VERSION);

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
    public void submittersBean_validConstructor_newInstance() {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        assertThat(submittersBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createSubmitter_nullSubmitterContent_throws() throws JSONBException {
        newSubmittersBeanWithMockedEntityManager().createSubmitter(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createSubmitter_emptySubmitterContent_throws() throws JSONBException {
        newSubmittersBeanWithMockedEntityManager().createSubmitter(null, "");
    }

    @Test(expected = JSONBException.class)
    public void createSubmitter_invalidJsonInSubmitterContent_throws() throws JSONBException {
        newSubmittersBeanWithMockedEntityManager().createSubmitter(null, "Invalid JSON");
    }

    @Test
    public void getSubmitter_submitterFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final Submitter submitter = new Submitter();
        final String submitterName = "testSubmitter";
        final Long submitterNumber = 555555L;
        submitter.setContent(new SubmitterContentJsonBuilder().setName(submitterName).setNumber(submitterNumber).build());
        submitter.setVersion(DEFAULT_TEST_VERSION);

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(submitter);

        Response response = submittersBean.getSubmitter(DEFAULT_TEST_ID);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("name").textValue(), is(submitterName));
        assertThat(entityNode.get("content").get("number").longValue(), is(submitterNumber));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    public void getSubmitter_noSubmitterFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(null);

        Response response = submittersBean.getSubmitter(DEFAULT_TEST_ID);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntityTag(), is(nullValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSubmitterBySubmitterNumber_submitterFound_returnsResponseWithHttpStatusOK() throws JSONBException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final Submitter submitter = new Submitter();
        final Long submitterNumber = 463725L;
        submitter.setContent(new SubmitterContentJsonBuilder().setNumber(submitterNumber).build());
        submitter.setVersion(DEFAULT_TEST_VERSION);

        TypedQuery<Submitter> query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(eq(Submitter.QUERY_FIND_BY_NUMBER), eq(Submitter.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(submitter));
        when(query.getSingleResult()).thenReturn(submitter);

        Response response = submittersBean.getSubmitterBySubmitterNumber(submitterNumber);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        JsonNode entityNode = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.get("content").get("number").longValue(), is(submitterNumber));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSubmitterBySubmitterNumber_noSubmitterFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final Long submitterNumber = 463725L;
        TypedQuery<Submitter> query = mock(TypedQuery.class);
        when(ENTITY_MANAGER.createNamedQuery(eq(Submitter.QUERY_FIND_BY_NUMBER), eq(Submitter.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(new ArrayList<>());

        Response response = submittersBean.getSubmitterBySubmitterNumber(submitterNumber);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntityTag(), is(nullValue()));
    }


    @Test(expected = NullPointerException.class)
    public void updateSubmitter_nullSubmitterContent_throws() throws JSONBException {
        newSubmittersBeanWithMockedEntityManager().updateSubmitter(null, 1L, 1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateSubmitter_emptySubmitterContent_throws() throws JSONBException {
        newSubmittersBeanWithMockedEntityManager().updateSubmitter("", 1L, 1L);
    }

    @Test
    public void updateSubmitter_submitterNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final String submitterContent = new SubmitterContentJsonBuilder().setName("UpdateContentName").build();
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(null);

        final Response response = submittersBean.updateSubmitter(submitterContent, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateSubmitter_submitterFound_returnsResponseWithHttpStatusOk_returnsSubmitter() throws JSONBException {
        final Submitter submitter = mock(Submitter.class);
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final String submitterContent = new SubmitterContentJsonBuilder().build();
        submittersBean.jsonbContext = mock(JSONBContext.class);

        when(submittersBean.jsonbContext.marshall(submitter)).thenReturn("test");
        when(ENTITY_MANAGER.find(eq(Submitter.class), any())).thenReturn(submitter);
        when(submitter.getVersion()).thenReturn(DEFAULT_TEST_VERSION);

        final Response response = submittersBean.updateSubmitter(submitterContent, DEFAULT_TEST_ID, DEFAULT_TEST_VERSION);
        verify(submitter).setContent(submitterContent);
        verify(submitter).setVersion(DEFAULT_TEST_VERSION);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getEntityTag().getValue(), is(DEFAULT_TEST_ETAG_VALUE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSubmitters_noSubmittersFound_returnsResponseWithHttpStatusOkAndSubmitterEntity() throws JSONBException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final TypedQuery query = mock(TypedQuery.class);

        when(ENTITY_MANAGER.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final Response response = submittersBean.findAllSubmitters();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllSubmitters_submittersFound_returnsResponseWithHttpStatusOkAndSubmitterEntities() throws JSONBException {
        final SubmittersBean submittersBean = newSubmittersBeanWithMockedEntityManager();
        final TypedQuery query = mock(TypedQuery.class);
        final String nameSubmitterA = "A";
        final Submitter submitterA = new Submitter();
        submitterA.setContent(new SubmitterContentJsonBuilder()
                .setName(nameSubmitterA)
                .build());
        final String nameSubmitterB = "B";
        final Submitter submitterB = new Submitter();
        submitterB.setContent(new SubmitterContentJsonBuilder()
                .setName(nameSubmitterB)
                .build());

        when(ENTITY_MANAGER.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(submitterA, submitterB));

        final Response response = submittersBean.findAllSubmitters();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("content").get("name").textValue(), is(nameSubmitterA));
        assertThat(entityNode.get(1).get("content").get("name").textValue(), is(nameSubmitterB));
    }

    public static SubmittersBean newSubmittersBeanWithMockedEntityManager() {
        final SubmittersBean submittersBean = new SubmittersBean();
        submittersBean.entityManager = ENTITY_MANAGER;
        return submittersBean;
    }
}
