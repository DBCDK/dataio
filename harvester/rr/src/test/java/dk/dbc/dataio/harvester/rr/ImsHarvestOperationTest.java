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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImsHarvestOperationTest extends HarvestOperationTest {
    private final static Set<Integer> IMS_LIBRARIES = Stream.of(710100, 737000, 775100, 785100)
            .collect(Collectors.toSet());

    private final VipCoreConnection vipCoreConnection = mock(VipCoreConnection.class);
    private final HoldingsItemsConnector holdingsItemsConnector = mock(HoldingsItemsConnector.class);

    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Timer timer = mock(Timer.class);
    private final Counter counter = mock(Counter.class);

    @Before
    public void setupImsHarvestOperationTestMocks() throws HarvesterException {
        when(vipCoreConnection.getFbsImsLibraries()).thenReturn(IMS_LIBRARIES);
        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        doNothing().when(timer).update(any(Duration.class));
        doNothing().when(counter).inc();
    }

    @Test
    public void execute_harvestedRecordHasNonDbcAndNonImsAgencyId_recordIsSkipped()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        final QueueItem queueItem = getQueueItem(
                new RecordIdDTO("bibliographicRecordId", 123456));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoRecordServiceConnector, times(0)).recordFetch(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasImsAgencyId_recordIsProcessed()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        final QueueItem queueItem = getQueueItem(
                new RecordIdDTO("bibliographicRecordId", 710100));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoRecordServiceConnector, times(2)).recordFetch(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyId_recordIsProcessed()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        final QueueItem queueItem = getQueueItem(
                new RecordIdDTO("bibliographicRecordId", 710100));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        final RecordDTO record = new RecordDTO();
        record.setRecordId(DBC_RECORD_ID);
        record.setCreated(Instant.now().toString());
        record.setContent(getDeleteRecordContent(DBC_RECORD_ID).getBytes(StandardCharsets.UTF_8));
        record.setDeleted(true);

        when(rawRepoRecordServiceConnector.getRecordDataCollectionDataIO(any(RecordIdDTO.class)))
                .thenReturn(new HashMap<String, RecordDTO>() {{
                    put(DBC_RECORD_ID.getBibliographicRecordId(), record);
                }});

        when(rawRepoRecordServiceConnector.recordFetch(any(RecordIdDTO.class))).thenReturn(record);

        Set<Integer> set = new HashSet<>();
        set.add(1);

        when(holdingsItemsConnector.hasHoldings(anyString(), anySet())).thenReturn(set);
        when(rawRepoRecordServiceConnector.recordExists(anyInt(), anyString())).thenReturn(true);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(rawRepoRecordServiceConnector, times(2)).recordFetch(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyIdAndHoldingsItemsLookupReturnsImsAgencyIds_recordIsProcessed()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        final QueueItem queueItem = getQueueItem(DBC_RECORD_ID);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(holdingsItemsConnector.hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES))
                .thenReturn(IMS_LIBRARIES);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(holdingsItemsConnector, times(1)).hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES);
        verify(rawRepoRecordServiceConnector, times(8)).recordFetch(any(RecordIdDTO.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyIdAndHoldingsItemsLookupReturnsNonImsAgencyIds_recordIsSkipped()
            throws HarvesterException, RecordServiceConnectorException, SQLException, QueueException {
        final QueueItem queueItem = getQueueItem(DBC_RECORD_ID);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueItem)
                .thenReturn(null);

        when(holdingsItemsConnector.hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES))
                .thenReturn(new HashSet<>(Collections.singletonList(123456)));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(holdingsItemsConnector, times(1)).hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES);
        verify(rawRepoRecordServiceConnector, times(0)).recordFetch(any(RecordIdDTO.class));
    }

    @Override
    public void execute_rawRepoDeleteRecordHasAgencyIdContainedInExcludedSet_recordIsProcessed() {
        // Irrelevant test from super class
    }

    @Override
    public void execute_rawRepoDeleteRecordHasDbcId_recordIsSkipped() {
        // Irrelevant test from super class
    }

    @Override
    public HarvestOperation newHarvestOperation() {
        return newHarvestOperation(HarvesterTestUtil.getRRHarvesterConfig());
    }

    @Override
    public HarvestOperation newHarvestOperation(RRHarvesterConfig config) {
        try {
            return new ImsHarvestOperation(config, harvesterJobBuilderFactory, taskRepo,
                    vipCoreConnection, rawRepoConnector, holdingsItemsConnector,
                    rawRepoRecordServiceConnector, metricRegistry);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
