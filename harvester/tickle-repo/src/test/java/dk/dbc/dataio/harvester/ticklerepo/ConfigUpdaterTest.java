package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.Batch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigUpdaterTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final ConfigUpdater configUpdater = ConfigUpdater.create(flowStoreServiceConnector);
    private final Batch batch = new Batch().withId(42);

    private TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());

    @BeforeEach
    public void setupMocks() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config)).thenReturn(config);
        when(flowStoreServiceConnector.getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class))).thenReturn(config);
    }

    @BeforeEach
    public void resetConfig() {
        config = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());
    }

    @Test
    public void updatesLastBatchHarvestedInConfig() throws HarvesterException {
        configUpdater.updateHarvesterConfig(config, batch);
        assertThat(config.getContent().getLastBatchHarvested(), is(batch.getId()));
    }

    @Test
    public void flowStoreReturnsNonOkNonConflict_throws() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("Internal error", 500));

        assertThat(() -> configUpdater.updateHarvesterConfig(config, batch), isThrowing(HarvesterException.class));

        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(0)).getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class));
    }

    @Test
    public void flowStoreReturnsConflict_refreshesConfigAndRetriesUpdate() throws FlowStoreServiceConnectorException, HarvesterException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("Conflict", 409))
                .thenReturn(config);

        configUpdater.updateHarvesterConfig(config, batch);
        assertThat(config.getContent().getLastBatchHarvested(), is(batch.getId()));

        verify(flowStoreServiceConnector, times(2)).updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(1)).getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class));
    }

    @Test
    public void flowStoreReturnsNonOkOnRefreshAfterConflict_throws() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("Conflict", 409));
        when(flowStoreServiceConnector.getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class)))
                .thenThrow(new FlowStoreServiceConnectorException("Died"));

        assertThat(() -> configUpdater.updateHarvesterConfig(config, batch), isThrowing(HarvesterException.class));

        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(1)).getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class));
    }
}
