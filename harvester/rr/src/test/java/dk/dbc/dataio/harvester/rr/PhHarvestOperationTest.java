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
import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.dto.PhLogEntry;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PhHarvestOperationTest extends HarvestOperationTest {

    private final AgencyConnection agencyConnection = mock(AgencyConnection.class);
    private final PhLog phLog = new PhLog(entityManager);

    @Test
    public void execute_phLogHasRecordMarkedAsDelete_recordIsMarkedAsDelte() throws HarvesterException {
        final HarvestOperation harvestOperation = newHarvestOperation();
        final PhLogEntry phLogEntry = new PhLogEntry().withDeleted(true);
        when(entityManager.find(eq(PhLogEntry.class), any(PhLogEntry.Key.class))).thenReturn(phLogEntry);
        harvestOperation.execute();
        verify(entityManager).find(eq(PhLogEntry.class), any(PhLogEntry.Key.class));
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
        return new PhHarvestOperation(config, harvesterJobBuilderFactory, entityManager, agencyConnection, rawRepoConnector, phLog);
    }
}