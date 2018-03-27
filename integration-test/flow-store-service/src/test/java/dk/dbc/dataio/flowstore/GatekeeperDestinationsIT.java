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

package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import net.jodah.failsafe.RetryPolicy;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newIntegrationTestConnection;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;

public class GatekeeperDestinationsIT {

    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;
    private static FlowStoreServiceConnector flowStoreServiceConnector;
    private static JSONBContext jsonbContext;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        dbConnection = newIntegrationTestConnection("flowstore");
        restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(
            FailSafeHttpClient.create(restClient, new RetryPolicy().withMaxRetries(0)),
            baseUrl);
        jsonbContext = new JSONBContext();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        JDBCUtil.closeConnection(dbConnection);
    }

    @After
    public void tearDown() throws SQLException {
        clearAllDbTables(dbConnection);
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the gatekeeper/destinations path without an identifier
     * Then : a gatekeeper destination it created and returned
     * And  : assert that the gatekeeper destination created has an id that differs from the id set before creation (0)
     * and contains the same information as the gatekeeper destination given as input
     * And  : assert that only one gatekeeper destination can be found in the underlying database
     */
    @Test
    public void createGatekeeperDestination_ok() throws Exception{

        // When...
        final GatekeeperDestination gatekeeperDestinationPrePersist = new GatekeeperDestinationBuilder().setId(0).build();

        // Then...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist);

        // And...
        assertNotNull(gatekeeperDestination);
        assertThat(gatekeeperDestination, is(gatekeeperDestinationPrePersist));
        assertThat(gatekeeperDestination.getId(), not(gatekeeperDestinationPrePersist.getId()));

        // And ...
        final List<GatekeeperDestination> gatekeeperDestinations = flowStoreServiceConnector.findAllGatekeeperDestinations();
        assertThat(gatekeeperDestinations.size(), is(1));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the gatekeeper/destinations path causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void createGatekeeperDestination_invalidJson_BadRequest() {
        // When...
        final Response response = HttpClient.doPostWithJson(
                restClient,
                "<invalid json />",
                baseUrl,
                FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing a gatekeeper destination resource
     * When : adding a gatekeeper destination containing the same values as the persisted resource
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that one gatekeeper destination exist in the underlying database
     */
    @Test
    public void createGatekeeperDestination_duplicateValues_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final GatekeeperDestination gatekeeperDestinationPrePersist = new GatekeeperDestinationBuilder().setId(0).build();
        try {
            flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist);

            // When...
            flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist);
            fail("Unique constraint was not detected as input to createGatekeeperDestination().");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
            // And...
            List<GatekeeperDestination> gatekeeperDestinations = flowStoreServiceConnector.findAllGatekeeperDestinations();
            assertThat(gatekeeperDestinations.size(), is(1));
        }
    }

    /*
     * Given: a deployed flow-store service containing no gatekeeper destinations
     * When: GETing gatekeeper destinations collection
     * Then: request returns with empty list
     */
    @Test
    public void findAllGatekeeperDestinations_emptyResult() throws Exception {
        // When...
        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final List<GatekeeperDestination> gatekeeperDestinations = flowStoreServiceConnector.findAllGatekeeperDestinations();

        // Then...
        assertThat(gatekeeperDestinations, is(notNullValue()));
        assertThat(gatekeeperDestinations.size(), is(0));
    }

    /*
     * Given: a deployed flow-store service containing three gatekeeper destinations
     * When: GETing gatekeeper destinations collection
     * Then: request returns with 3 gatekeeper destinations
     * And: the gatekeeper destinations are sorted by submitter number
     */
    @Test
    public void findAllGatekeeperDestinations_Ok() throws Exception {
        // Given...
        final GatekeeperDestination gatekeeperDestinationPrePersist1 = new GatekeeperDestinationBuilder().setId(0).setSubmitterNumber("1").build();
        final GatekeeperDestination gatekeeperDestinationPrePersist2 = new GatekeeperDestinationBuilder().setId(0).setSubmitterNumber("2").build();
        final GatekeeperDestination gatekeeperDestinationPrePersist3 = new GatekeeperDestinationBuilder().setId(0).setSubmitterNumber("3").build();

        final FlowStoreServiceConnector flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        final GatekeeperDestination sortsSecond = flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist2);
        final GatekeeperDestination sortsThird = flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist3);
        final GatekeeperDestination sortsFirst = flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist1);

        // When...
        final List<GatekeeperDestination> gatekeeperDestinations = flowStoreServiceConnector.findAllGatekeeperDestinations();

        // Then...
        assertNotNull(gatekeeperDestinations);
        assertThat(gatekeeperDestinations.size(), is (3));

        // And...
        assertThat(gatekeeperDestinations.get(0), is (sortsFirst));
        assertThat(gatekeeperDestinations.get(1), is (sortsSecond));
        assertThat(gatekeeperDestinations.get(2), is (sortsThird));
    }

    /**
     * Given: a deployed flow-store service and a gatekeeper destination is stored
     * When : attempting to delete the gatekeeper destination
     * Then : the gatekeeper destination is deleted
     */
    @Test
    public void deleteGatekeeperDestination_Ok() throws FlowStoreServiceConnectorException {

        // Given...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector.createGatekeeperDestination(new GatekeeperDestinationBuilder().setId(0L).build());

        // When...
        flowStoreServiceConnector.deleteGatekeeperDestination(gatekeeperDestination.getId());

        // Then...
        final List<GatekeeperDestination> gatekeeperDestinations = flowStoreServiceConnector.findAllGatekeeperDestinations();
        assertThat(gatekeeperDestinations.size(), is(0));
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to delete a gatekeeper destination that does not exist
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void deleteGatekeeperDestination_gateKeeperDestinationNotFound() throws ProcessingException {
        try {
            // When...
            flowStoreServiceConnector.deleteGatekeeperDestination(42L);
            fail("None existing Gatekeeper destination was not detected");

            // Then ...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(Response.Status.fromStatusCode(e.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service with a valid gatekeeper destination with given id is already stored
     * When : valid JSON is POSTed to the gatekeeper/destinations path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that updated data can be found in the underlying database and only one gatekeeper destination exists
     */
    @Test
    public void updateGatekeeperDestination_ok() throws Exception{

        // Given...
        GatekeeperDestination originalGatekeeperDestination = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder().setPackaging("lin").setId(0L).build());

        GatekeeperDestination modifiedGatekeeperDestination = new GatekeeperDestinationBuilder()
                .setId(originalGatekeeperDestination.getId()).setPackaging("iso").build();

        // When...
        GatekeeperDestination updatedGatekeeperDestination = flowStoreServiceConnector.updateGatekeeperDestination(modifiedGatekeeperDestination);

        // Then...
        assertThat(updatedGatekeeperDestination, is(modifiedGatekeeperDestination));

        // And...
        assertThat(flowStoreServiceConnector.findAllGatekeeperDestinations().size(), is(1));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the gatekeeper/destinations path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateGatekeeperDestination_invalidJson_BadRequest() throws FlowStoreServiceConnectorException {
        // Given...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector
                .createGatekeeperDestination(new GatekeeperDestinationBuilder().setId(0L).build());

        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl,
                FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS, String.valueOf(gatekeeperDestination.getId()));
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the gatekeeper/destinations path with an identifier (update), where the id of the gatekeeper destination object
     *        does not match the id given in path.
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateGatekeeperDestination_gatekeeperDestinationIdDoesNotMatchIdInPath_BadRequest() throws FlowStoreServiceConnectorException, JSONBException {
        // Given...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector
                .createGatekeeperDestination(new GatekeeperDestinationBuilder().setId(0L).build());

        // When...
        final Response response = HttpClient.doPostWithJson(restClient, jsonbContext.marshall(gatekeeperDestination), baseUrl,
                FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS, String.valueOf(gatekeeperDestination.getId() + 1));
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the gatekeeper/destinations path with an identifier (update) and a none existing id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no gatekeeper destination exist in the underlying database
     */
    @Test
    public void updateGatekeeperDestination_noneExistingIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try {
            // When...
            flowStoreServiceConnector.updateGatekeeperDestination(new GatekeeperDestinationBuilder().build());
            fail("None existing gatekeeper id was not detected as input to updateGatekeeperDestination().");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(404));

            // And...
            final List<GatekeeperDestination> gatekeeperDestinations = flowStoreServiceConnector.findAllGatekeeperDestinations();
            assertThat(gatekeeperDestinations.size(), is(0));
        }
    }

    /**
     * Given: a deployed flow-store service with two valid gatekeeper destinations stored
     * When : valid JSON is POSTed to the gatekeeper/destinations path with an identifier (update) but with a combination of values that are
     *        already in use by another existing gatekeeper destination
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     * And  : assert that two gatekeeper destinations exists in the underlying database
     * And  : updated data cannot be found in the underlying database
     */
    @Test
    public void updateGatekeeperDestination_noneUniqueValues_NotAcceptable() throws FlowStoreServiceConnectorException{
        // Given...
        GatekeeperDestination gatekeeperDestinationA = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder().setDestination("A").setId(0).build());

        GatekeeperDestination gatekeeperDestinationB = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder().setDestination("B").setId(0).build());

        try {
            // When... (Attempting to update the second gatekeeperDestination with the same value combination as the first gatekeeperDestination)
            flowStoreServiceConnector.updateGatekeeperDestination(new GatekeeperDestinationBuilder()
                    .setId(gatekeeperDestinationB.getId())
                    .setDestination(gatekeeperDestinationA.getDestination())
                    .build()
            );

            fail("Primary key violation was not detected as input to updateGatekeeperDestination().");
            // Then...
        }catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e){

            // And...
            assertThat(e.getStatusCode(), is(406));

            // And...
            final List<GatekeeperDestination> gatekeeperDestinations = flowStoreServiceConnector.findAllGatekeeperDestinations();
            assertThat(gatekeeperDestinations.size(), is(2));

            // And...
            assertThat(gatekeeperDestinations.get(0), is (gatekeeperDestinationA));
            assertThat(gatekeeperDestinations.get(1), is (gatekeeperDestinationB));
        }
    }

}
