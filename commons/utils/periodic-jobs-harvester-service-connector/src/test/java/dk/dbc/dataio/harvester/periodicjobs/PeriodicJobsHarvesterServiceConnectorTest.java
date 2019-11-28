package dk.dbc.dataio.harvester.periodicjobs;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.periodicjobs.PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.periodicjobs.PeriodicJobsHarvesterServiceConnector;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import javax.ws.rs.core.Response;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static dk.dbc.commons.testutil.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsHarvesterServiceConnectorTest {
    private static String PERIODIC_JOBS_HARVESTER_URL = "http://periodicjobsharvester";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);
    private final PeriodicJobsHarvesterServiceConnector connector =
            new PeriodicJobsHarvesterServiceConnector(failSafeHttpClient, PERIODIC_JOBS_HARVESTER_URL);

    @Test
    public void testCreateJob() throws PeriodicJobsHarvesterServiceConnectorException {
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(Response.ok().build());
        connector.createPeriodicJob(1L);
    }

    @Test
    public void testCreateJobIdNotFound() throws PeriodicJobsHarvesterServiceConnectorException {
        final MockedResponse response = new MockedResponse(Response.Status.NOT_FOUND.getStatusCode(), null);
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(response);
        try {
            connector.createPeriodicJob(0L);
        }
        catch (PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException e) {
            assertThat("Statuscode is not found", e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    public void testCreateJobInternalServerError() throws PeriodicJobsHarvesterServiceConnectorException {
        final MockedResponse response = new MockedResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null);
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(response);
        try {
            connector.createPeriodicJob(0L);
        }
        catch (PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException e) {
            assertThat("Statuscode is internal server error", e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }
}
