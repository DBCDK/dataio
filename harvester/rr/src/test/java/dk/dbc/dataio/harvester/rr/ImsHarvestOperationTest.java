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
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.RecordId;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImsHarvestOperationTest extends HarvestOperationTest {
    private final static Set<Integer> IMS_LIBRARIES = Stream.of(710100, 737000, 775100, 785100)
            .collect(Collectors.toSet());

    private final HoldingsItemsConnector holdingsItemsConnector = mock(HoldingsItemsConnector.class);

    @Test
    public void execute_harvestedRecordHasNonDbcAndNonImsAgencyId_recordIsSkipped() throws HarvesterException, RawRepoException, SQLException {
        final QueueJob queueJob = getQueueJob(new RecordId("bibliographicRecordId", 123456));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute(entityManager);

        verify(rawRepoConnector, times(0)).fetchRecord(any(RecordId.class));
    }

    @Test
    public void execute_harvestedRecordHasImsAgencyId_recordIsProcessed() throws HarvesterException, RawRepoException, SQLException {
        final QueueJob queueJob = getQueueJob(new RecordId("bibliographicRecordId", 710100));

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute(entityManager);

        verify(rawRepoConnector, times(1)).fetchRecord(any(RecordId.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyIdAndHoldingsItemsLookupReturnsImsAgencyIds_recordIsProcessed()
            throws HarvesterException, RawRepoException, SQLException {
        final QueueJob queueJob = getQueueJob(DBC_RECORD_ID);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(holdingsItemsConnector.hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES))
                .thenReturn(IMS_LIBRARIES);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute(entityManager);

        verify(holdingsItemsConnector, times(1)).hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES);
        verify(rawRepoConnector, times(4)).fetchRecord(any(RecordId.class));
    }

    @Test
    public void execute_harvestedRecordHasDbcAgencyIdAndHoldingsItemsLookupReturnsNonImsAgencyIds_recordIsSkipped()
            throws HarvesterException, RawRepoException, SQLException {
        final QueueJob queueJob = getQueueJob(DBC_RECORD_ID);

        when(rawRepoConnector.dequeue(anyString()))
                .thenReturn(queueJob)
                .thenReturn(null);

        when(holdingsItemsConnector.hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES))
                .thenReturn(new HashSet<>(Collections.singletonList(123456)));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute(entityManager);

        verify(holdingsItemsConnector, times(1)).hasHoldings(DBC_RECORD_ID.getBibliographicRecordId(), IMS_LIBRARIES);
        verify(rawRepoConnector, times(0)).fetchRecord(any(RecordId.class));
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
        return new ImsHarvestOperation(config, harvesterJobBuilderFactory, null, rawRepoConnector, holdingsItemsConnector);
    }
}