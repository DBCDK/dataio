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
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskQueueTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);
    private final TypedQuery<HarvestTask> query = mock(TypedQuery.class);
    private final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()
                        .withConsumerId("consumerId"));

    @Before
    public void setupMocks() {
        when(entityManager.createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class)).thenReturn(query);
        when(query.setParameter("configId", config.getId())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
    }

    @Test
    public void poll_noWaitingTasks_returnsNull() throws HarvesterException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        final TaskQueue queue = createQueue();
        assertThat(queue.poll(), is(nullValue()));
        assertThat("queue.estimatedSize()", queue.estimatedSize(), is(0));
    }

    @Test
    public void peek_noWaitingTasks_returnsNull() throws HarvesterException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        final TaskQueue queue = createQueue();
        assertThat(queue.peek(), is(nullValue()));
        assertThat("queue.estimatedSize()", queue.estimatedSize(), is(0));
    }

    @Test
    public void poll_removesHead() throws HarvesterException {
        final int submitterNumber = 123456;
        final RawRepoRecordHarvestTask expectedRecordHarvestTask1 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id1", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id1")
                        .withSubmitterNumber(submitterNumber));
        final RawRepoRecordHarvestTask expectedRecordHarvestTask2 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id2", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id2")
                        .withSubmitterNumber(submitterNumber));
        final HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Arrays.asList(
                expectedRecordHarvestTask1.getAddiMetaData(),
                expectedRecordHarvestTask2.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        final TaskQueue queue = createQueue();
        assertThat("queue is empty before first poll", queue.isEmpty(), is(false));
        final RawRepoRecordHarvestTask recordHarvestTask1 = queue.poll();
        assertThat("queue is empty after first poll", queue.isEmpty(), is(false));
        final RawRepoRecordHarvestTask recordHarvestTask2 = queue.poll();
        assertThat("queue is empty after second poll", queue.isEmpty(), is(true));
        assertThat("recordHarvestTask1", recordHarvestTask1, is(expectedRecordHarvestTask1));
        assertThat("recordHarvestTask2", recordHarvestTask2, is(expectedRecordHarvestTask2));
    }

    @Test
    public void peek_headRemains() throws HarvesterException {
        final int submitterNumber = 123456;
        final RawRepoRecordHarvestTask expectedRecordHarvestTask = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                    .withBibliographicRecordId("id")
                    .withSubmitterNumber(submitterNumber));
        final HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Collections.singletonList(expectedRecordHarvestTask.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        final TaskQueue queue = createQueue();
        assertThat("queue is empty before first peek", queue.isEmpty(), is(false));
        final RawRepoRecordHarvestTask recordHarvestTask1 = queue.peek();
        assertThat("queue is empty after first peek", queue.isEmpty(), is(false));
        final RawRepoRecordHarvestTask recordHarvestTask2 = queue.peek();
        assertThat("queue is empty after second peek", queue.isEmpty(), is(false));
        assertThat("recordHarvestTask1", recordHarvestTask1, is(expectedRecordHarvestTask));
        assertThat("recordHarvestTask2", recordHarvestTask2, is(expectedRecordHarvestTask));
    }

    @Test
    public void poll_skipsWhereRecordIdIsNull() throws HarvesterException {
        final RawRepoRecordHarvestTask expectedRecordHarvestTask = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", 123456))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(123456));
        final HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Arrays.asList(
                new AddiMetaData().withBibliographicRecordId("missingAgencyId"),
                expectedRecordHarvestTask.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        final TaskQueue queue = createQueue();
        assertThat("queue is empty before first poll", queue.isEmpty(), is(false));
        final RawRepoRecordHarvestTask recordHarvestTask = queue.poll();
        assertThat("queue is empty after first poll", queue.isEmpty(), is(true));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask));
    }

    @Test
    public void peek_skipsWhereRecordIdIsNull() throws HarvesterException {
        final RawRepoRecordHarvestTask expectedRecordHarvestTask = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", 123456))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(123456));
        final HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Arrays.asList(
                new AddiMetaData().withBibliographicRecordId("missingAgencyId"),
                expectedRecordHarvestTask.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        final TaskQueue queue = createQueue();
        assertThat("queue is empty before first peek", queue.isEmpty(), is(false));
        final RawRepoRecordHarvestTask recordHarvestTask = queue.peek();
        assertThat("queue is empty after first peek", queue.isEmpty(), is(false));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask));
    }

    @Test
    public void interpolates870970TasksForDbc() throws HarvesterException {
        interpolatesTasksForDbcWithSubmitter(870970);
    }

    @Test
    public void interpolates190004TasksForDbc() throws HarvesterException {
        interpolatesTasksForDbcWithSubmitter(190004);
    }

    public void interpolatesTasksForDbcWithSubmitter(int submitter) throws HarvesterException {
        final RawRepoRecordHarvestTask expectedRecordHarvestTask1 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", submitter))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(submitter));
        final RawRepoRecordHarvestTask expectedRecordHarvestTask2 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", 191919))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(submitter));
        final HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Collections.singletonList(expectedRecordHarvestTask1.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        final TaskQueue queue = createQueue();
        assertThat("queue is empty before first poll", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask = queue.poll();
        assertThat("queue is empty after first poll", queue.isEmpty(), is(false));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask1));
        recordHarvestTask = queue.poll();
        assertThat("queue is empty after second poll", queue.isEmpty(), is(true));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask2));
    }

    private TaskQueue createQueue() {
        return new TaskQueue(config, taskRepo);
    }
}