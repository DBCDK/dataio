package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.opensearch.commons.repository.RepositoryException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private final CORepoConnector coRepoConnector = mock(CORepoConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector = mock(VipCoreLibraryRulesConnector.class);
    private final HarvesterTaskServiceConnector rrHarvesterServiceConnector = mock(HarvesterTaskServiceConnector.class);

    CoRepoHarvesterConfig config;

    @Before
    public void setupMocks() throws FlowStoreServiceConnectorException, RepositoryException {
        config = new CoRepoHarvesterConfig(1, 1, new CoRepoHarvesterConfig.Content()
                .withEnabled(true)
                .withTimeOfLastHarvest(Date.from(Instant.now().minus(645, ChronoUnit.SECONDS))));

        when(flowStoreServiceConnector.getHarvesterConfig(1, CoRepoHarvesterConfig.class)).thenReturn(config);
        when(flowStoreServiceConnector.updateHarvesterConfig(config)).thenReturn(config);

        when(coRepoConnector.getChangesInRepository(any(Instant.class), any(Instant.class), any()))
                .thenReturn(Arrays.asList(Pid.of("870970-basis:1"), Pid.of("870970-basis:2")))
                .thenReturn(Collections.singletonList(Pid.of("870970-basis:3")));
    }

    @Test
    public void harvestsAllTimeIntervals() throws HarvesterException, RepositoryException {
        newHarvestOperation().execute();

        verify(coRepoConnector, times(2)).getChangesInRepository(any(Instant.class), any(Instant.class), any());
    }

    @Test
    public void noPIDsHarvested() throws HarvesterException, HarvesterTaskServiceConnectorException, FlowStoreServiceConnectorException, RepositoryException {
        when(coRepoConnector.getChangesInRepository(any(Instant.class), any(Instant.class), any()))
                .thenReturn(Collections.emptyList());

        final Instant initialTimeOfLastHarvest = config.getContent().getTimeOfLastHarvest().toInstant();
        assertThat("number of PIDs harvested", newHarvestOperation().execute(), is(0));

        verify(rrHarvesterServiceConnector, times(0)).createHarvestTask(any(Long.class), any(HarvestRecordsRequest.class));
        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        assertThat("timeOfLastHarvest updated", config.getContent().getTimeOfLastHarvest().toInstant().isAfter(initialTimeOfLastHarvest), is(true));
    }

    @Test
    public void pidsHarvested() throws HarvesterException, HarvesterTaskServiceConnectorException, FlowStoreServiceConnectorException {
        final Instant initialTimeOfLastHarvest = config.getContent().getTimeOfLastHarvest().toInstant();
        assertThat("number of PIDs harvested", newHarvestOperation().execute(), is(3));

        verify(rrHarvesterServiceConnector, times(1)).createHarvestTask(any(Long.class), any(HarvestRecordsRequest.class));
        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        assertThat("timeOfLastHarvest updated", config.getContent().getTimeOfLastHarvest().toInstant().isAfter(initialTimeOfLastHarvest), is(true));
    }

    @Test
    public void numberOfPidsHarvestedExceedsMaxBatchSize() throws HarvesterException, HarvesterTaskServiceConnectorException, FlowStoreServiceConnectorException {
        final int harvestMaxBatchSize = HarvestOperation.HARVEST_MAX_BATCH_SIZE;
        try {
            HarvestOperation.HARVEST_MAX_BATCH_SIZE = 2;

            assertThat("number of PIDs harvested", newHarvestOperation().execute(), is(3));

            verify(rrHarvesterServiceConnector, times(2)).createHarvestTask(any(Long.class), any(HarvestRecordsRequest.class));
            verify(flowStoreServiceConnector, times(2)).updateHarvesterConfig(config);
        } finally {
            HarvestOperation.HARVEST_MAX_BATCH_SIZE = harvestMaxBatchSize;
        }
    }

    @Test
    public void HarvesterAbortsIntervalsWhenNotEnabled() throws HarvesterException, HarvesterTaskServiceConnectorException, FlowStoreServiceConnectorException {
        final int harvestMaxBatchSize = HarvestOperation.HARVEST_MAX_BATCH_SIZE;
        try {
            HarvestOperation.HARVEST_MAX_BATCH_SIZE = 2;
            config.getContent().withEnabled(false);

            assertThat("number of PIDs harvested", newHarvestOperation().execute(), is(0));

            verify(rrHarvesterServiceConnector, times(0)).createHarvestTask(any(Long.class), any(HarvestRecordsRequest.class));
            verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(config);
        } finally {
            HarvestOperation.HARVEST_MAX_BATCH_SIZE = harvestMaxBatchSize;
        }
    }

    private HarvestOperation newHarvestOperation() throws HarvesterException {
        return new HarvestOperation(config, coRepoConnector,
                flowStoreServiceConnector, vipCoreLibraryRulesConnector,
                rrHarvesterServiceConnector);
    }
}
