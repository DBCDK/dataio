package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;

public class GatekeeperDestinationsIT extends AbstractFlowStoreServiceContainerTest {

    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the gatekeeper/destinations path without an identifier
     * Then : a gatekeeper destination it created and returned
     * And  : assert that the gatekeeper destination created has an id that differs from the id set before creation (0)
     * and contains the same information as the gatekeeper destination given as input
     */
    @Test
    public void createGatekeeperDestination_ok() throws Exception {
        // When...
        final GatekeeperDestination gatekeeperDestinationPrePersist = new GatekeeperDestinationBuilder()
                .setDestination("GatekeeperDestinationsIT.createGatekeeperDestination_ok")
                .setId(0)
                .build();

        // Then...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector
                .createGatekeeperDestination(gatekeeperDestinationPrePersist);

        // And...
        assertNotNull(gatekeeperDestination);
        assertThat(gatekeeperDestination, is(gatekeeperDestinationPrePersist));
        assertThat(gatekeeperDestination.getId(), not(gatekeeperDestinationPrePersist.getId()));
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
                flowStoreServiceConnector.getClient(), "<invalid json />",
                flowStoreServiceBaseUrl, FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing a gatekeeper destination resource
     * When : adding a gatekeeper destination containing the same values as the persisted resource
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createGatekeeperDestination_duplicateValues_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final GatekeeperDestination gatekeeperDestinationPrePersist = new GatekeeperDestinationBuilder()
                .setDestination("GatekeeperDestinationsIT.createGatekeeperDestination_duplicateValues_NotAcceptable")
                .setId(0)
                .build();
        try {
            flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist);

            // When...
            flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestinationPrePersist);
            fail("Unique constraint was not detected as input to createGatekeeperDestination().");

            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /*
     * Given: a deployed flow-store service containing three gatekeeper destinations
     * When: GETing gatekeeper destinations collection
     * Then: request returns with 3 gatekeeper destinations
     * And: the gatekeeper destinations are sorted by submitter number
     */
    @Test
    public void findAllGatekeeperDestinations_ok() throws Exception {
        // Given...
        final GatekeeperDestination gatekeeperDestinationPrePersist1 = new GatekeeperDestinationBuilder()
                .setDestination("GatekeeperDestinationsIT.findAllGatekeeperDestinations_ok")
                .setSubmitterNumber("1")
                .setId(0)
                .build();
        final GatekeeperDestination gatekeeperDestinationPrePersist2 = new GatekeeperDestinationBuilder()
                .setDestination("GatekeeperDestinationsIT.findAllGatekeeperDestinations_ok")
                .setSubmitterNumber("2")
                .setId(0)
                .build();
        final GatekeeperDestination gatekeeperDestinationPrePersist3 = new GatekeeperDestinationBuilder()
                .setDestination("GatekeeperDestinationsIT.findAllGatekeeperDestinations_ok")
                .setSubmitterNumber("3")
                .setId(0)
                .build();

        final GatekeeperDestination sortsSecond = flowStoreServiceConnector
                .createGatekeeperDestination(gatekeeperDestinationPrePersist2);
        final GatekeeperDestination sortsThird = flowStoreServiceConnector
                .createGatekeeperDestination(gatekeeperDestinationPrePersist3);
        final GatekeeperDestination sortsFirst = flowStoreServiceConnector
                .createGatekeeperDestination(gatekeeperDestinationPrePersist1);

        // When...
        final List<GatekeeperDestination> gatekeeperDestinations =
                flowStoreServiceConnector.findAllGatekeeperDestinations();

        // Then...
        assertNotNull(gatekeeperDestinations);
        assertThat(gatekeeperDestinations.size() >= 3, is(true));

        // And...
        assertThat(gatekeeperDestinations.get(0), is(sortsFirst));
        assertThat(gatekeeperDestinations.get(1), is(sortsSecond));
        assertThat(gatekeeperDestinations.get(2), is(sortsThird));
    }

    /**
     * Given: a deployed flow-store service and a gatekeeper destination is stored
     * When : attempting to delete the gatekeeper destination
     * Then : the gatekeeper destination is deleted
     */
    @Test
    public void deleteGatekeeperDestination_ok() throws FlowStoreServiceConnectorException {
        // Given...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder()
                        .setDestination("GatekeeperDestinationsIT.deleteGatekeeperDestination_ok")
                        .setId(0L)
                        .build());

        // When...
        flowStoreServiceConnector.deleteGatekeeperDestination(gatekeeperDestination.getId());

        // Then...
        flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestination);
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
            flowStoreServiceConnector.deleteGatekeeperDestination(new Date().getTime());
            fail("None existing Gatekeeper destination was not detected");

            // Then ...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(Response.Status.fromStatusCode(e.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service with a valid gatekeeper destination with given id is already stored
     * When : valid JSON is POSTed to the gatekeeper/destinations path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     */
    @Test
    public void updateGatekeeperDestination_ok() throws Exception {

        // Given...
        GatekeeperDestination originalGatekeeperDestination = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder()
                        .setDestination("GatekeeperDestinationsIT.updateGatekeeperDestination_ok")
                        .setPackaging("lin")
                        .setId(0L)
                        .build());

        GatekeeperDestination modifiedGatekeeperDestination = new GatekeeperDestinationBuilder()
                .setDestination("GatekeeperDestinationsIT.updateGatekeeperDestination_ok")
                .setPackaging("iso")
                .setId(originalGatekeeperDestination.getId())
                .build();

        // When...
        GatekeeperDestination updatedGatekeeperDestination =
                flowStoreServiceConnector.updateGatekeeperDestination(modifiedGatekeeperDestination);

        // Then...
        assertThat(updatedGatekeeperDestination, is(modifiedGatekeeperDestination));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the gatekeeper/destinations path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateGatekeeperDestination_invalidJson_BadRequest() throws FlowStoreServiceConnectorException {
        // Given...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder()
                        .setDestination("GatekeeperDestinationsIT.updateGatekeeperDestination_invalidJson_BadRequest")
                        .setId(0L)
                        .build());

        // When...
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                "<invalid json />", flowStoreServiceBaseUrl,
                FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS,
                String.valueOf(gatekeeperDestination.getId()));
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the gatekeeper/destinations path with an identifier (update), where the id of the gatekeeper destination object
     * does not match the id given in path.
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateGatekeeperDestination_gatekeeperDestinationIdDoesNotMatchIdInPath_BadRequest() throws FlowStoreServiceConnectorException, JSONBException {
        // Given...
        GatekeeperDestination gatekeeperDestination = flowStoreServiceConnector
                .createGatekeeperDestination(new GatekeeperDestinationBuilder()
                        .setDestination("GatekeeperDestinationsIT.updateGatekeeperDestination_gatekeeperDestinationIdDoesNotMatchIdInPath_BadRequest")
                        .setId(0L)
                        .build());

        // When...
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                jsonbContext.marshall(gatekeeperDestination), flowStoreServiceBaseUrl,
                FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS,
                String.valueOf(gatekeeperDestination.getId() + 1));
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the gatekeeper/destinations path with an identifier (update) and a none existing id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void updateGatekeeperDestination_noneExistingIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try {
            // When...
            flowStoreServiceConnector.updateGatekeeperDestination(new GatekeeperDestinationBuilder()
                    .build());
            fail("None existing gatekeeper id was not detected as input to updateGatekeeperDestination().");

            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service with two valid gatekeeper destinations stored
     * When : valid JSON is POSTed to the gatekeeper/destinations path with an identifier (update) but with a combination of values that are
     * already in use by another existing gatekeeper destination
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void updateGatekeeperDestination_noneUniqueValues_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        GatekeeperDestination gatekeeperDestinationA = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder()
                        .setDestination("GatekeeperDestinationsIT.updateGatekeeperDestination_noneUniqueValues_NotAcceptable.A")
                        .setId(0)
                        .build());

        GatekeeperDestination gatekeeperDestinationB = flowStoreServiceConnector.createGatekeeperDestination(
                new GatekeeperDestinationBuilder()
                        .setDestination("GatekeeperDestinationsIT.updateGatekeeperDestination_noneUniqueValues_NotAcceptable.B")
                        .setId(0)
                        .build());
        try {
            // When... (Attempting to update the second gatekeeperDestination with the same value combination as the first gatekeeperDestination)
            flowStoreServiceConnector.updateGatekeeperDestination(new GatekeeperDestinationBuilder()
                    .setId(gatekeeperDestinationB.getId())
                    .setDestination(gatekeeperDestinationA.getDestination())
                    .build()
            );

            fail("Primary key violation was not detected as input to updateGatekeeperDestination().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

}
