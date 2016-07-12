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
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskQueueTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()
                        .withConsumerId("consumerId"));

    @Before
    public void setupMocks() {
        when(entityManager.createNamedQuery(HarvestTask.QUERY_FIND_WAITING)).thenReturn(query);
        when(query.setParameter("configId", config.getId())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
    }

    @Test
    public void poll_noWaitingTasks_returnsNull() throws HarvesterException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        final TaskQueue queue = createQueue();
        assertThat(queue.poll(), is(nullValue()));
        assertThat("queue.size()", queue.size(), is(0));
    }

    @Test
    public void peek_noWaitingTasks_returnsNull() throws HarvesterException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        final TaskQueue queue = createQueue();
        assertThat(queue.peek(), is(nullValue()));
        assertThat("queue.size()", queue.size(), is(0));
    }

    @Test
    public void poll_removesHead() throws HarvesterException {
        final int submitterNumber = 123456;
        final RecordId expectedRecordId1 = new RecordId("id1", submitterNumber);
        final RecordId expectedRecordId2 = new RecordId("id2", submitterNumber);
        final HarvestTask task = new HarvestTask();
        task.setSubmitterNumber((long) submitterNumber);
        task.setRecordIds(Arrays.asList(
                expectedRecordId1.getBibliographicRecordId(), expectedRecordId2.getBibliographicRecordId()));
        when(query.getResultList()).thenReturn(Collections.singletonList(task));

        final TaskQueue queue = createQueue();
        assertThat("queue.size() before first poll", queue.size(), is(2));
        final RecordId recordId1 = queue.poll();
        assertThat("queue.size() after first poll", queue.size(), is(1));
        final RecordId recordId2 = queue.poll();
        assertThat("queue.size() after second poll", queue.size(), is(0));
        assertThat("recordId1", recordId1, is(expectedRecordId1));
        assertThat("recordId2", recordId2, is(expectedRecordId2));
        assertThat("task status", task.getStatus(), is(HarvestTask.Status.COMPLETED));
    }

    @Test
    public void peek_headRemains() throws HarvesterException {
        final int submitterNumber = 123456;
        final RecordId expectedRecordId = new RecordId("id1", submitterNumber);
        final HarvestTask task = new HarvestTask();
        task.setSubmitterNumber((long) submitterNumber);
        task.setRecordIds(Collections.singletonList(expectedRecordId.getBibliographicRecordId()));
        when(query.getResultList()).thenReturn(Collections.singletonList(task));

        final TaskQueue queue = createQueue();
        assertThat("queue.size() before first peek", queue.size(), is(1));
        final RecordId recordId1 = queue.peek();
        assertThat("queue.size() after first peek", queue.size(), is(1));
        final RecordId recordId2 = queue.peek();
        assertThat("queue.size() after second peek", queue.size(), is(1));
        assertThat("recordId1", recordId1, is(expectedRecordId));
        assertThat("recordId2", recordId2, is(expectedRecordId));
    }

    private TaskQueue createQueue() {
        return new TaskQueue(config, entityManager);
    }
}