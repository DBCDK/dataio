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

import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.json.GatekeeperDestinationJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.flowstore.entity.GatekeeperDestinationEntity;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GatekeeperDestinationsBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private UriInfo mockedUriInfo;
    private JSONBContext jsonbContext;

    @Before
    public void setup() throws URISyntaxException {
        mockedUriInfo = mock(UriInfo.class);
        final UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));

        jsonbContext = new JSONBContext();
    }

    // ***************************************************** create gatekeeper destination ******************************************************

    @Test
    public void gatekeeperDestinationsBean_validConstructor_newInstance() {
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        assertThat(gatekeeperDestinationsBean, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void createGatekeeperDestination_nullGatekeeperDestination_throws() throws JSONBException {
        newGatekeeperDestinationsBeanWithMockedEntityManager().createGatekeeperDestination(mockedUriInfo, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createGatekeeperDestination_emptyGatekeeperDestination_throws() throws JSONBException {
        newGatekeeperDestinationsBeanWithMockedEntityManager().createGatekeeperDestination(mockedUriInfo, "");
    }

    @Test(expected = JSONBException.class)
    public void createGatekeeperDestination_invalidJSON_throwsJsonException() throws JSONBException {
        newGatekeeperDestinationsBeanWithMockedEntityManager().createGatekeeperDestination(mockedUriInfo, "invalid Json");
    }

    @Test
    public void createGatekeeperDestination_gatekeeperDestinationCreated_returnsResponseWithHttpStatusOk() throws JSONBException {
        final String gatekeeperDestination = new GatekeeperDestinationJsonBuilder().build();
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();

        // Subject under test
        final Response response = gatekeeperDestinationsBean.createGatekeeperDestination(mockedUriInfo, gatekeeperDestination);

        // Verification
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    // **************************************************** find all gatekeeper destinations ****************************************************

    @Test
    public void findAllGatekeeperDestinations_noGatekeeperDestinationsFound_returnsResponseWithHttpStatusOkAndEmptyList() throws JSONBException {
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        final Query query = mock(Query.class);

        when(ENTITY_MANAGER.createNamedQuery(GatekeeperDestinationEntity.QUERY_FIND_ALL)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // Subject under test
        final Response response = gatekeeperDestinationsBean.findAllGatekeeperDestinations();

        // Verification
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    public void findAllGatekeeperDestinations_gatekeeperDestinationsFound_returnsResponseWithHttpStatusOkAndGatekeeperDestinationEntities() throws JSONBException {
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        final Query query = mock(Query.class);
        final GatekeeperDestinationEntity gatekeeperDestinationEntityA = jsonbContext.unmarshall(
                new GatekeeperDestinationJsonBuilder().setSubmitterNumber("123").build(), GatekeeperDestinationEntity.class);

        final GatekeeperDestinationEntity gatekeeperDestinationEntityB = jsonbContext.unmarshall(
                new GatekeeperDestinationJsonBuilder().setSubmitterNumber("234").build(), GatekeeperDestinationEntity.class);

        when(ENTITY_MANAGER.createNamedQuery(GatekeeperDestinationEntity.QUERY_FIND_ALL)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(gatekeeperDestinationEntityA, gatekeeperDestinationEntityB));

        // Subject under test
        final Response response = gatekeeperDestinationsBean.findAllGatekeeperDestinations();

        // Verification
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("submitterNumber").textValue(), is("123"));
        assertThat(entityNode.get(1).get("submitterNumber").textValue(), is("234"));
    }

    // ****************************************************** delete gatekeeper destination *****************************************************

    @Test
    public void deleteGatekeeperDestination_gatekeeperDestinationNotFound_returnsResponseWithHttpStatusNotFound() {
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(null);

        // Subject under test
        final Response response = gatekeeperDestinationsBean.deleteGatekeeperDestination(42L);

        // Verification
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteGatekeeperDestination_gatekeeperDestinationFound_returnsNoContentHttpResponse() {
        final GatekeeperDestinationEntity gatekeeperDestinationEntity = mock(GatekeeperDestinationEntity.class);
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(gatekeeperDestinationEntity);

        // Subject under test
        final Response response = gatekeeperDestinationsBean.deleteGatekeeperDestination(42L);

        // Verification
        verify(ENTITY_MANAGER).remove(gatekeeperDestinationEntity);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    // ***************************************************** update gatekeeper destination ******************************************************

    @Test(expected = NullPointerException.class)
    public void updateGatekeeperDestination_nullGatekeeperDestination_throws() throws JSONBException {
        newGatekeeperDestinationsBeanWithMockedEntityManager().updateGatekeeperDestination(null, 42L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateGatekeeperDestination_emptyGatekeeperDestination_throws() throws JSONBException {
        newGatekeeperDestinationsBeanWithMockedEntityManager().updateGatekeeperDestination("", 42L);
    }

    @Test
    public void updateGatekeeperDestination_gatekeeperDestinationNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(null);

        // Subject under test
        final Response response = gatekeeperDestinationsBean.updateGatekeeperDestination(new GatekeeperDestinationJsonBuilder().build(), 42L);

        // Verification
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateGatekeeperDestination_gatekeeperDestinationFound_returnsResponseWithHttpStatusOk() throws JSONBException {
        final GatekeeperDestinationEntity gatekeeperDestinationEntity = mock(GatekeeperDestinationEntity.class);
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().build();
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        gatekeeperDestinationsBean.jsonbContext = mock(JSONBContext.class);

        when(gatekeeperDestinationsBean.jsonbContext.unmarshall(anyString(), eq(GatekeeperDestination.class))).thenReturn(gatekeeperDestination);
        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(gatekeeperDestinationEntity);
        when(gatekeeperDestinationsBean.jsonbContext.marshall(gatekeeperDestinationEntity)).thenReturn("entity");

        // Subject under test
        final Response response = gatekeeperDestinationsBean.updateGatekeeperDestination(
                new JSONBContext().marshall(gatekeeperDestination), gatekeeperDestination.getId());

        // Verification
        verify(gatekeeperDestinationEntity).setSubmitterNumber(gatekeeperDestination.getSubmitterNumber());
        verify(gatekeeperDestinationEntity).setDestination(gatekeeperDestination.getDestination());
        verify(gatekeeperDestinationEntity).setPackaging(gatekeeperDestination.getPackaging());
        verify(gatekeeperDestinationEntity).setFormat(gatekeeperDestination.getFormat());
        verify(gatekeeperDestinationEntity).setCopyToPosthus(gatekeeperDestination.isCopyToPosthus());
        verify(gatekeeperDestinationEntity).setNotifyFromPosthus(gatekeeperDestination.isNotifyFromPosthus());

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    /*
     * Private methods
     */

    private static GatekeeperDestinationsBean newGatekeeperDestinationsBeanWithMockedEntityManager() {
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = new GatekeeperDestinationsBean();
        gatekeeperDestinationsBean.entityManager = ENTITY_MANAGER;
        return gatekeeperDestinationsBean;
    }
}
