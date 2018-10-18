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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorldCatHarvestOperationTest extends HarvestOperationTest {
    private final OcnRepo ocnRepo = mock(OcnRepo.class);
    private final static String OPENAGENCY_ENDPOINT = "openagency.endpoint";

    private final WorldCatEntity worldCatEntity = new WorldCatEntity()
            .withAgencyId(870970)
            .withBibliographicRecordId("recId")
            .withPid("897970-basis-recId")
            .withOcn("ocn123456");

    private final RawRepoRecordHarvestTask task = new RawRepoRecordHarvestTask()
            .withRecordId(HarvestOperationTest.DBC_RECORD_ID)
            .withAddiMetaData(new AddiMetaData()
                    .withSubmitterNumber(worldCatEntity.getAgencyId())
                    .withBibliographicRecordId(worldCatEntity.getBibliographicRecordId()));

    @Before
    public void setupMocks() {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class)))
                .thenReturn(Collections.singletonList(worldCatEntity));
    }

    @Test
    public void noHitsInOcnRepo_preprocessingReturnsEmptyList() throws QueueException, ConfigurationException, SQLException {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Collections.emptyList());
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        assertThat("number of tasks", harvestOperation.preprocessRecordHarvestTask(task).size(), is(0));
    }

    @Test
    public void singleHitInOcnRepo_preprocessingReturnsList() throws QueueException, ConfigurationException, SQLException {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Collections.singletonList(worldCatEntity));
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        final List<RawRepoRecordHarvestTask> tasks = harvestOperation.preprocessRecordHarvestTask(task);
        assertThat("number of tasks", tasks.size(), is(1));
        final RawRepoRecordHarvestTask recordHarvestTask = tasks.get(0);
        assertThat("record ID", recordHarvestTask.getRecordId(), is(HarvestOperationTest.DBC_RECORD_ID));
        compareAddiMetaDataWithWorldCatEntity(recordHarvestTask.getAddiMetaData(), worldCatEntity);
    }

    @Test
    public void multipleHitsInOcnRepo_preprocessingReturnsList() throws QueueException, ConfigurationException, SQLException {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Arrays.asList(worldCatEntity, worldCatEntity));
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        final List<RawRepoRecordHarvestTask> tasks = harvestOperation.preprocessRecordHarvestTask(task);
        assertThat("number of tasks", tasks.size(), is(2));
    }

    @Test
    public void multipleHitsInOcnRepo_causesMultipleItemsToBeAddedToJob() throws HarvesterException, QueueException, ConfigurationException, SQLException {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Arrays.asList(worldCatEntity, worldCatEntity));
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(harvesterJobBuilder, times(2)).addRecord(any(AddiRecord.class));
    }

    @Override
    public WorldCatHarvestOperation newHarvestOperation() throws QueueException, ConfigurationException, SQLException {
        return newHarvestOperation(HarvesterTestUtil.getRRHarvesterConfig());
    }

    @Override
    public WorldCatHarvestOperation newHarvestOperation(RRHarvesterConfig config) throws QueueException, ConfigurationException, SQLException {
        return new WorldCatHarvestOperation(config,
            harvesterJobBuilderFactory, taskRepo,
            new AgencyConnection(OPENAGENCY_ENDPOINT), rawRepoConnector,
            ocnRepo);
    }

    private void compareAddiMetaDataWithWorldCatEntity(AddiMetaData addiMetaData, WorldCatEntity worldCatEntity) {
        assertThat("addi submitterNumber", addiMetaData.submitterNumber(), is(worldCatEntity.getAgencyId()));
        assertThat("addi bibliographicRecordId", addiMetaData.bibliographicRecordId(), is(worldCatEntity.getBibliographicRecordId()));
        assertThat("addi pid", addiMetaData.pid(), is(worldCatEntity.getPid()));
        assertThat("addi ocn", addiMetaData.ocn(), is(worldCatEntity.getOcn()));
    }
}
