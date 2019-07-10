/*
 * DataIO - Data IO
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.phholdingsitems;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PhHoldingsItemsHarvesterConfig;
import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.dto.PhLogEntry;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private final PhLog phLog = mock(PhLog.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final HarvesterTaskServiceConnector rrHarvesterServiceConnector = mock(HarvesterTaskServiceConnector.class);
    PhHoldingsItemsHarvesterConfig config;

    private static PhLog.ResultSet<PhLogEntry> results = mock(PhLog.ResultSet.class);

    @Before
    public void setupMocks() throws FlowStoreServiceConnectorException {
        ArrayList<Long> rrHarvesters = new ArrayList<>();
        rrHarvesters.add(5252L);
        config = new PhHoldingsItemsHarvesterConfig(1, 1, new PhHoldingsItemsHarvesterConfig.Content()
            .withEnabled(true)
            .withTimeOfLastHarvest(Date.from(Instant.now().minus(645, ChronoUnit.SECONDS)))
            .withRrHarvesters(rrHarvesters));

        when(flowStoreServiceConnector.getHarvesterConfig(1, PhHoldingsItemsHarvesterConfig.class))
            .thenReturn(config);
        when(flowStoreServiceConnector.updateHarvesterConfig(config)).thenReturn(config);
    }


    @Test
    public void harvest() throws HarvesterException,
            HarvesterTaskServiceConnectorException, FlowStoreServiceConnectorException {
        HarvestOperation harvestOperation = getHarvestOperation();
        setUpEntries();
        int harvested = harvestOperation.execute();
        assertThat("number of records harvested", harvested, is(3));
        verify(rrHarvesterServiceConnector, times(1)).createHarvestTask(
            any(Long.class), any(HarvestRecordsRequest.class));
        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
    }

    @Test
    public void noRecordsHarvested() throws HarvesterException,
            HarvesterTaskServiceConnectorException, FlowStoreServiceConnectorException {
        HarvestOperation harvestOperation = getHarvestOperation();
        setUpNoEntries();
        int harvested = harvestOperation.execute();
        assertThat("number of records harvested", harvested, is(0));
        verify(rrHarvesterServiceConnector, times(0)).createHarvestTask(
            any(Long.class), any(HarvestRecordsRequest.class));
        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
    }

    @Test
    public void recordsExceedingMaxBatchSize() throws HarvesterException,
            HarvesterTaskServiceConnectorException, FlowStoreServiceConnectorException {
        HarvestOperation harvestOperation = getHarvestOperation();
        final int oldHarvestMaxBatchSize = HarvestOperation.HARVEST_MAX_BATCH_SIZE;
        try {
            HarvestOperation.HARVEST_MAX_BATCH_SIZE = 2;
            setUpEntries();
            int harvested = harvestOperation.execute();
            assertThat(harvested, is(3));
            verify(rrHarvesterServiceConnector, times(1)).createHarvestTask(
                any(Long.class), any(HarvestRecordsRequest.class));
            verify(flowStoreServiceConnector, times(2)).updateHarvesterConfig(config);
        } finally {
            HarvestOperation.HARVEST_MAX_BATCH_SIZE = oldHarvestMaxBatchSize;
        }
    }

    private void setUpEntries() {
        List<PhLogEntry> list = Stream.of(830190, 830370, 830380).map(e ->
            new PhLogEntry()
                .withKey(new PhLogEntry.Key()
                    .withBibliographicRecordId("123123123")
                    .withAgencyId(e))
                .withDeleted(true)
        ).collect(Collectors.toList());
        when(results.iterator()).thenReturn(list.iterator());
        when(phLog.getEntriesModifiedBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(results);
    }

    private void setUpNoEntries() {
        when(results.iterator()).thenReturn(Collections.emptyIterator());
        when(phLog.getEntriesModifiedBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(results);
    }

    private HarvestOperation getHarvestOperation() throws HarvesterException {
       return new HarvestOperation(config,
           phLog, flowStoreServiceConnector, rrHarvesterServiceConnector);
    }
}
