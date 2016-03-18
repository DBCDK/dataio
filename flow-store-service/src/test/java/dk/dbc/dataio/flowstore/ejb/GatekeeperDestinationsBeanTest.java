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

import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.test.json.GatekeeperDestinationJsonBuilder;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class GatekeeperDestinationsBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private UriInfo mockedUriInfo;

    @Before
    public void setup() throws URISyntaxException {
        mockedUriInfo = mock(UriInfo.class);
        final UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));
    }

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
    public void createGatekeeperDestination_gatekeeperDestinationCreated_returnsResponseWithHttpStatusOk() throws JSONBException, ReferencedEntityNotFoundException {
        final String gatekeeperDestination = new GatekeeperDestinationJsonBuilder().build();
        final GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();

        // Subject under test
        final Response response = gatekeeperDestinationsBean.createGatekeeperDestination(mockedUriInfo, gatekeeperDestination);

        // Verification
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
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
