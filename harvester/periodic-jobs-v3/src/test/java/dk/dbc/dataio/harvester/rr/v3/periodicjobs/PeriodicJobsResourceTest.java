package dk.dbc.dataio.harvester.rr.v3.periodicjobs;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import dk.dbc.dataio.harvester.rr.v3.periodicjobs.rest.PeriodicJobsResource;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsResourceTest {
    private static final PeriodicJobsResource periodicJobsResource = new PeriodicJobsResource();
    private static final PeriodicJobsV3HarvesterConfig periodicJobsV3HarvesterConfig = new PeriodicJobsV3HarvesterConfig();


    @BeforeEach
    public void setupMocks() throws HarvesterException {
        periodicJobsResource.harvesterConfigurationBean = mock(HarvesterConfigurationBean.class);
        periodicJobsResource.harvesterBean = mock(HarvesterBean.class);
        when(periodicJobsResource.harvesterConfigurationBean.getConfig(1)).thenReturn(Optional.of(periodicJobsV3HarvesterConfig));
        when(periodicJobsResource.harvesterConfigurationBean.getConfig(0)).thenReturn(Optional.empty());
    }

    @Test
    public void testCreatePeriodicJob() throws HarvesterException {
        assertThat("Create periodicJob returns 200-ok",
                periodicJobsResource.createPeriodicJob(1L).getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Create periodicJob returns 401-not found",
                periodicJobsResource.createPeriodicJob(0L).getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }


}
