package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigUpdaterTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector =
            mock(FlowStoreServiceConnector.class);

    private final ConfigUpdater configUpdater =
            ConfigUpdater.create(flowStoreServiceConnector);

    private PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(
            1, 2, new PeriodicJobsHarvesterConfig.Content());

    @BeforeEach
    public void setupMocks() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenReturn(config);
        when(flowStoreServiceConnector.getHarvesterConfig(
                anyLong(), eq(PeriodicJobsHarvesterConfig.class)))
                .thenReturn(config);
    }

    @BeforeEach
    public void resetConfig() {
        config = new PeriodicJobsHarvesterConfig(
                1, 2, new PeriodicJobsHarvesterConfig.Content());
    }

    @Test
    public void flowStoreReturnsNonOkNonConflict_throws()
            throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(
                        "Internal error", 500));

        assertThat(() -> configUpdater.push(config), isThrowing(HarvesterException.class));

        verify(flowStoreServiceConnector, times(1))
                .updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(0))
                .getHarvesterConfig(anyLong(), eq(PeriodicJobsHarvesterConfig.class));
    }

    @Test
    public void flowStoreReturnsConflict_refreshesConfigAndRetriesUpdate()
            throws FlowStoreServiceConnectorException, HarvesterException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(
                        "Conflict", 409))
                .thenReturn(config);

        configUpdater.push(config);

        verify(flowStoreServiceConnector, times(2))
                .updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(1))
                .getHarvesterConfig(anyLong(), eq(PeriodicJobsHarvesterConfig.class));
    }

    @Test
    public void flowStoreReturnsNonOkOnRefreshAfterConflict_throws()
            throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(
                        "Conflict", 409));
        when(flowStoreServiceConnector.getHarvesterConfig(
                anyLong(), eq(PeriodicJobsHarvesterConfig.class)))
                .thenThrow(new FlowStoreServiceConnectorException("Died"));

        assertThat(() -> configUpdater.push(config), isThrowing(HarvesterException.class));

        verify(flowStoreServiceConnector, times(1))
                .updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(1))
                .getHarvesterConfig(anyLong(), eq(PeriodicJobsHarvesterConfig.class));
    }
}
