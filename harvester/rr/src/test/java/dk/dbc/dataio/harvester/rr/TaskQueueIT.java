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

import dk.dbc.dataio.harvester.rr.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.RecordId;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TaskQueueIT extends IntegrationTest {
    /*
     * Given: a task queue containing a ready task
     * When : queue is polled until empty and subsequently committed
     * Then : the records contained in the task are returned in order
     * And  : the task gets its status and timeOfCompletion updated
     */
    @Test
    public void readyTasksExists() {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content());
        final int submitterNumber = 123456;
        final List<String> recordIds = new ArrayList<>(Arrays.asList("id1", "id2"));

        final HarvestTask task = new HarvestTask();
        task.setConfigId(config.getId());
        task.setSubmitterNumber((long) submitterNumber);
        task.setRecordIds(recordIds);
        task.setStatus(HarvestTask.Status.READY);
        persist(task);

        final TaskQueue taskQueue = new TaskQueue(config, entityManager);
        persistenceContext.run(() -> {
            assertThat("Number of records in task queue", taskQueue.size(), is(2));
            assertThat("1st record", taskQueue.poll(), is(new RecordId(recordIds.get(0), submitterNumber)));
            assertThat("2nd record", taskQueue.poll(), is(new RecordId(recordIds.get(1), submitterNumber)));
            taskQueue.commit();
        });

        final HarvestTask updatedTask = entityManager.merge(task);
        assertThat("Task status updated", updatedTask.getStatus(), is(HarvestTask.Status.COMPLETED));
        assertThat("Task timeOfCompletion updated", updatedTask.getTimeOfCompletion(), is(notNullValue()));
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
        task.setSubmitterNumber(123456L);
        task.setRecordIds(Collections.emptyList());
        task.setStatus(HarvestTask.Status.COMPLETED);
        persist(task);

        final TaskQueue taskQueue = new TaskQueue(config, entityManager);
        persistenceContext.run(() -> {
            assertThat("Number of records in task queue", taskQueue.size(), is(0));
            taskQueue.commit();
        });
    }
}
