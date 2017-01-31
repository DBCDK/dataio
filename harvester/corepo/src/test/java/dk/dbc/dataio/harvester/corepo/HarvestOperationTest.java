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

package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.openagency.OpenAgencyConnector;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnector;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnectorException;
import dk.dbc.opensearch.commons.repository.RepositoryException;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private final CORepoConnector coRepoConnector = mock(CORepoConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final OpenAgencyConnector openAgencyConnector = mock(OpenAgencyConnector.class);
    private final RRHarvesterServiceConnector rrHarvesterServiceConnector = mock(RRHarvesterServiceConnector.class);

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
    public void noPIDsHarvested() throws HarvesterException, RRHarvesterServiceConnectorException, FlowStoreServiceConnectorException, RepositoryException {
        when(coRepoConnector.getChangesInRepository(any(Instant.class), any(Instant.class), any()))
                .thenReturn(Collections.emptyList());

        final Instant initialTimeOfLastHarvest = config.getContent().getTimeOfLastHarvest().toInstant();
        assertThat("number of PIDs harvested", newHarvestOperation().execute(), is(0));

        verify(rrHarvesterServiceConnector, times(0)).createHarvestTask(any(Long.class), any(HarvestRecordsRequest.class));
        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        assertThat("timeOfLastHarvest updated", config.getContent().getTimeOfLastHarvest().toInstant().isAfter(initialTimeOfLastHarvest), is(true));
    }

    @Test
    public void pidsHarvested() throws HarvesterException, RRHarvesterServiceConnectorException, FlowStoreServiceConnectorException {
        final Instant initialTimeOfLastHarvest = config.getContent().getTimeOfLastHarvest().toInstant();
        assertThat("number of PIDs harvested", newHarvestOperation().execute(), is(3));

        verify(rrHarvesterServiceConnector, times(1)).createHarvestTask(any(Long.class), any(HarvestRecordsRequest.class));
        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        assertThat("timeOfLastHarvest updated", config.getContent().getTimeOfLastHarvest().toInstant().isAfter(initialTimeOfLastHarvest), is(true));
    }

    @Test
    public void numberOfPidsHarvestedExceedsMaxBatchSize() throws HarvesterException, RRHarvesterServiceConnectorException, FlowStoreServiceConnectorException {
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
    public void HarvesterAbortsIntervalsWhenNotEnabled() throws HarvesterException, RRHarvesterServiceConnectorException, FlowStoreServiceConnectorException {
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
        return new HarvestOperation(config, coRepoConnector, flowStoreServiceConnector, openAgencyConnector, rrHarvesterServiceConnector);
    }
}