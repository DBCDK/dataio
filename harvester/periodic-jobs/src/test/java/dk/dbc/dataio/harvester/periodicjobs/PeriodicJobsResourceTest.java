package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.harvester.periodicjobs.rest.PeriodicJobsResource;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.server.Authentication;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;


import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsResourceTest {
    private static PeriodicJobsResource periodicJobsResource = new PeriodicJobsResource();
    private static HarvesterBean harvesterBean = mock(HarvesterBean.class);
    private static PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig1 = new PeriodicJobsHarvesterConfig();


    @Before
    public void setupMocks() throws HarvesterException, FlowStoreServiceConnectorException {
        periodicJobsResource.harvesterConfigurationBean = mock(HarvesterConfigurationBean.class);
        periodicJobsResource.harvesterBean = mock(HarvesterBean.class);
        when(periodicJobsResource.harvesterConfigurationBean.getConfig(1)).thenReturn(Optional.of(periodicJobsHarvesterConfig1));
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
