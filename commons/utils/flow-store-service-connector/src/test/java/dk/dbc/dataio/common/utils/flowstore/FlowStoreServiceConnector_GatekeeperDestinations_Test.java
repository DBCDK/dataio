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

import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class FlowStoreServiceConnector_GatekeeperDestinations_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // **************************************************** create gatekeeper destination tests *****************************************************************

    @Test
    public void createGatekeeperDestination_gatekeeperDestinationArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        try {
            instance.createGatekeeperDestination(null);
            fail("Exception not thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void createGatekeeperDestination_gatekeeperDestinationCreated_returnsGatekeeperDestination() throws FlowStoreServiceConnectorException, JSONBException {
        final GatekeeperDestination gatekeeperDestination =
                createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(
                        Response.Status.CREATED.getStatusCode(),
                        new GatekeeperDestinationBuilder().build());

        assertThat(gatekeeperDestination, is(notNullValue()));
    }

    @Test
    public void createGatekeeperDestination_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        try {
            createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException e) { }
    }

    @Test
    public void createGatekeeperDestination_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        try {
            createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException e) { }
    }

    @Test
    public void createGatekeeperDestination_responseWithUniqueConstraintViolation_throws() throws FlowStoreServiceConnectorException {
        try {
            createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) { }
    }

    // ************************************************** find all gatekeeper destinations tests ****************************************************************

    @Test
    public void findAllGatekeeperDestinations_gatekeeperDestinationsRetrieved_returnsListOfGatekeeperDestinations() throws FlowStoreServiceConnectorException {
        GatekeeperDestination gatekeeperDestinationA = new GatekeeperDestinationBuilder().setSubmitterNumber("1234").build();
        GatekeeperDestination gatekeeperDestinationB = new GatekeeperDestinationBuilder().setSubmitterNumber("2345").build();

        // Subject under test
        final List<GatekeeperDestination> result = findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(
                Response.Status.OK.getStatusCode(),
                Arrays.asList(gatekeeperDestinationA, gatekeeperDestinationB));

        // Verification
        assertThat(result.size(), is(2));
        assertThat(result.get(0), is(gatekeeperDestinationA));
        assertThat(result.get(1), is(gatekeeperDestinationB));
    }

    @Test
    public void findAllGatekeeperDestinations_noResults_returnsEmptyList() throws FlowStoreServiceConnectorException {
        // Subject under test
        List<GatekeeperDestination> result = findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(
                Response.Status.OK.getStatusCode(), Collections.emptyList());

        // Verification
        assertThat(result.size(), is(0));
    }

    @Test
    public void findAllGatekeeperDestinations_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        try {
            findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException e) { }
    }

    @Test
    public void findAllGatekeeperDestinations_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        try {
            findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(Response.Status.OK.getStatusCode(), null);
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException e) { }
    }

    @Test
    public void findAllGatekeeperDestinations_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        try {
            findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(Response.Status.NOT_FOUND.getStatusCode(), null);
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException e) { }
    }

    // ************************************************** delete gatekeeper destination tests *****************************************************************

    @Test
    public void deleteGatekeeperDestination_gatekeeperDestinationIsDeleted() throws FlowStoreServiceConnectorException, JSONBException {
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().build();
        deleteGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), gatekeeperDestination.getId());
    }

    @Test
    public void deleteGatekeeperDestination_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        try {
            deleteGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ID);
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException e) { }
    }

    @Test
    public void deleteGatekeeperDestination_responseWithNotFound_throws() throws FlowStoreServiceConnectorException{
        try {
            deleteGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), ID);
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) { }
    }

    /*
     * Private methods
     */

    /*
     * Helper method for createGatekeeperDestination tests
     */
    private GatekeeperDestination createGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, gatekeeperDestination, FLOW_STORE_URL, FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createGatekeeperDestination(gatekeeperDestination);
    }

    /*
     * Helper method for findAllGatekeeperDestination tests
     */
    private List<GatekeeperDestination> findAllGatekeeperDestinations_mockedHttpWithSpecifiedStatusCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllGatekeeperDestinations();
    }

    /*
     * Helper method for deleteGatekeeperDestination tests
     */
    private void deleteGatekeeperDestination_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.GATEKEEPER_DESTINATION)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        when(HttpClient.doDelete(CLIENT, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, null));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.deleteGatekeeperDestination(id);
    }
}
