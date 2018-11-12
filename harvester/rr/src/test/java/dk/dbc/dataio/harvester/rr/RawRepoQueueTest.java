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
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.MockedQueueItem;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.queue.QueueException;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RawRepoQueueTest {
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()
                        .withConsumerId("consumerId"));

    private RawRepoQueue queue;

    @Before
    public void createQueue() {
        queue = new RawRepoQueue(config, rawRepoConnector, 200, ChronoUnit.MILLIS);
    }

    @Test
    public void poll_rawRepoDequeueReturnsNull_returnsNull() throws HarvesterException {
        assertThat(queue.poll(), is(nullValue()));
    }

    @Test
    public void peek_rawRepoDequeueReturnsNull_returnsNull() throws HarvesterException {
        assertThat(queue.peek(), is(nullValue()));
    }

    @Test
    public void poll_removesHead() throws SQLException, HarvesterException, QueueException {
        final RawRepoRecordHarvestTask expectedRecordHarvestTask1 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordData.RecordId("id1", 123456))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id1")
                        .withSubmitterNumber(123456));
        final RawRepoRecordHarvestTask expectedRecordHarvestTask2 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordData.RecordId("id2", 123456))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id2")
                        .withSubmitterNumber(123456));
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId()))
                .thenReturn(new MockedQueueItem(
                        expectedRecordHarvestTask1.getRecordId().getBibliographicRecordId(),
                        expectedRecordHarvestTask1.getRecordId().getAgencyId(),
                        "worker", new Timestamp(new Date().getTime())))
                .thenReturn(new MockedQueueItem(
                        expectedRecordHarvestTask2.getRecordId().getBibliographicRecordId(),
                        expectedRecordHarvestTask2.getRecordId().getAgencyId(),
                        "worker", new Timestamp(new Date().getTime())));

        final RawRepoRecordHarvestTask task1 = queue.poll();
        final RawRepoRecordHarvestTask task2 = queue.poll();
        assertThat("task1", task1, is(expectedRecordHarvestTask1));
        assertThat("task2", task2, is(expectedRecordHarvestTask2));

        verify(rawRepoConnector, times(2)).dequeue(config.getContent().getConsumerId());
    }

    @Test
    public void poll_pileUpDuration() throws SQLException, HarvesterException, InterruptedException, QueueException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId()))
                .thenReturn(new MockedQueueItem("id", 123456 , "worker",
                        new Timestamp(new Date().getTime()), 1));

        // Keep polling high priority items
        queue.poll();
        assertThat("poll during pile up", queue.poll(), is(notNullValue()));

        // Ensure pile up duration is exceeded
        Thread.sleep(250);
        queue.poll();

        assertThat("poll after pile up duration exceeded", queue.poll(), is(nullValue()));
    }

    @Test
    public void poll_lowPriorityOnly() throws SQLException, HarvesterException, InterruptedException, QueueException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId()))
                .thenReturn(new MockedQueueItem("id", 123456 , "worker",
                        new Timestamp(new Date().getTime()), 1000));

        // Keep polling low priority items
        queue.poll();
        assertThat("poll", queue.poll(), is(notNullValue()));

        // Ensure pile up duration is exceeded
        Thread.sleep(250);
        queue.poll();

        // assert that low priority only queue is not affected by pileUpDuration
        assertThat("poll after pile up duration exceeded", queue.poll(), is(notNullValue()));
    }

    @Test
    public void poll_highPriorityFollowedByLowPriority() throws SQLException, HarvesterException, QueueException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId()))
                .thenReturn(new MockedQueueItem("id", 123456 , "worker",
                        new Timestamp(new Date().getTime()), 1))
                .thenReturn(new MockedQueueItem("id", 123456 , "worker",
                        new Timestamp(new Date().getTime()), 1000))
                .thenReturn(new MockedQueueItem("id", 123456 , "worker",
                    new Timestamp(new Date().getTime()), 1));

        assertThat("poll high", queue.poll(), is(notNullValue()));
        assertThat("poll low", queue.poll(), is(notNullValue()));
        assertThat("poll", queue.poll(), is(nullValue()));
    }

    @Test
    public void peek_headRemains() throws SQLException, HarvesterException, QueueException {
        final RawRepoRecordHarvestTask expectedRecordHarvestTask = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordData.RecordId("id", 123456))
                .withAddiMetaData(new AddiMetaData()
                    .withBibliographicRecordId("id")
                    .withSubmitterNumber(123456));

        when(rawRepoConnector.dequeue(config.getContent().getConsumerId()))
                .thenReturn(new MockedQueueItem(
                        expectedRecordHarvestTask.getRecordId().getBibliographicRecordId(),
                        expectedRecordHarvestTask.getRecordId().getAgencyId(),
                        "worker", new Timestamp(new Date().getTime())));

        final RawRepoRecordHarvestTask task1 = queue.peek();
        final RawRepoRecordHarvestTask task2 = queue.peek();
        assertThat("task1", task1, is(expectedRecordHarvestTask));
        assertThat("task2", task2, is(expectedRecordHarvestTask));

        verify(rawRepoConnector, times(1)).dequeue(config.getContent().getConsumerId());
    }
}