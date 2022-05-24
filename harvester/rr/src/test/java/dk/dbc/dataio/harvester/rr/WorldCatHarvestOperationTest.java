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
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorldCatHarvestOperationTest extends HarvestOperationTest {
    private final OcnRepo ocnRepo = mock(OcnRepo.class);

    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Timer timer = mock(Timer.class);
    private final Counter counter = mock(Counter.class);
    private final VipCoreConnection vipCoreConnection = mock(VipCoreConnection.class);

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
        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        when(metricRegistry.counter(any(Metadata.class), any(Tag.class))).thenReturn(counter);
        doNothing().when(timer).update(any(Duration.class));
        doNothing().when(counter).inc();
    }

    @Test
    public void noHitsInOcnRepo_preprocessingReturnsEmptyList() {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Collections.emptyList());
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        assertThat("number of tasks", harvestOperation.preprocessRecordHarvestTask(task).size(), is(0));
    }

    @Test
    public void singleHitInOcnRepo_preprocessingReturnsList() {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Collections.singletonList(worldCatEntity));
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        final List<RawRepoRecordHarvestTask> tasks = harvestOperation.preprocessRecordHarvestTask(task);
        assertThat("number of tasks", tasks.size(), is(1));
        final RawRepoRecordHarvestTask recordHarvestTask = tasks.get(0);
        assertThat("record ID", recordHarvestTask.getRecordId(), is(HarvestOperationTest.DBC_RECORD_ID));
        compareAddiMetaDataWithWorldCatEntity(recordHarvestTask.getAddiMetaData(), worldCatEntity);
    }

    @Test
    public void multipleHitsInOcnRepo_preprocessingReturnsList() {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Arrays.asList(worldCatEntity, worldCatEntity));
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        final List<RawRepoRecordHarvestTask> tasks = harvestOperation.preprocessRecordHarvestTask(task);
        assertThat("number of tasks", tasks.size(), is(2));
    }

    @Test
    public void multipleHitsInOcnRepo_causesMultipleItemsToBeAddedToJob() throws HarvesterException {
        when(ocnRepo.lookupWorldCatEntity(any(WorldCatEntity.class))).thenReturn(Arrays.asList(worldCatEntity, worldCatEntity));
        final WorldCatHarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verify(harvesterJobBuilder, times(2)).addRecord(any(AddiRecord.class));
    }

    @Override
    public WorldCatHarvestOperation newHarvestOperation() {
        return newHarvestOperation(HarvesterTestUtil.getRRHarvesterConfig());
    }

    @Override
    public WorldCatHarvestOperation newHarvestOperation(RRHarvesterConfig config) {
        try {
            return new WorldCatHarvestOperation(config, harvesterJobBuilderFactory, taskRepo,
                    vipCoreConnection, rawRepoConnector, ocnRepo,
                    rawRepoRecordServiceConnector, metricRegistry);
        } catch (SQLException | QueueException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void compareAddiMetaDataWithWorldCatEntity(AddiMetaData addiMetaData, WorldCatEntity worldCatEntity) {
        assertThat("addi submitterNumber", addiMetaData.submitterNumber(), is(worldCatEntity.getAgencyId()));
        assertThat("addi bibliographicRecordId", addiMetaData.bibliographicRecordId(), is(worldCatEntity.getBibliographicRecordId()));
        assertThat("addi pid", addiMetaData.pid(), is(worldCatEntity.getPid()));
        assertThat("addi ocn", addiMetaData.ocn(), is(worldCatEntity.getOcn()));
    }
}
