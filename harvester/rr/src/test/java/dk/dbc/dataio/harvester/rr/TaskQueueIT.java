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

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TaskQueueIT extends IntegrationTest {
    /*
     * Given: a task queue containing a ready task
     * When : queue is polled until empty and subsequently committed
     * Then : the records contained in the task are returned in order
     * And  : the task is removed
     */
    @Test
    public void readyTasksExists() {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content());
        final int submitterNumber = 123456;
        final RawRepoRecordHarvestTask expectedRecordHarvestTask1 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id1", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id1")
                        .withSubmitterNumber(submitterNumber)
                        .withLibraryRules(new AddiMetaData.LibraryRules()));
        final RawRepoRecordHarvestTask expectedRecordHarvestTask2 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id2", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id2")
                        .withSubmitterNumber(submitterNumber)
                        .withLibraryRules(new AddiMetaData.LibraryRules()));

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setRecords(Arrays.asList(
                expectedRecordHarvestTask1.getAddiMetaData(),
                expectedRecordHarvestTask2.getAddiMetaData()));
        persist(task);
        jpaTestEnvironment.getEntityManager().refresh(task);

        final TaskQueue taskQueue = new TaskQueue(config, new TaskRepo(jpaTestEnvironment.getEntityManager()));
        jpaTestEnvironment.getPersistenceContext().run(() -> {
            assertThat("task queue is empty", taskQueue.isEmpty(), is(false));
            assertThat("1st harvestRecordTask", taskQueue.poll(), is(expectedRecordHarvestTask1));
            assertThat("2nd harvestRecordTask", taskQueue.poll(), is(expectedRecordHarvestTask2));
            taskQueue.commit();
        });

        assertThat(jpaTestEnvironment.getEntityManager().find(HarvestTask.class, task.getId()), is(nullValue()));
    }

    /*
     * Given: a task store containing non-ready tasks
     * Then : an empty task queue is created
     */
    @Test
    public void noReadyTasksExists() {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content());

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setRecords(Collections.emptyList());
        persist(task);

        final TaskQueue taskQueue = new TaskQueue(config, new TaskRepo(jpaTestEnvironment.getEntityManager()));
        jpaTestEnvironment.getPersistenceContext().run(() -> {
            assertThat("task queue is empty", taskQueue.isEmpty(), is(true));
            taskQueue.commit();
        });
    }
}
