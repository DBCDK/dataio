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
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
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
        queue = new RawRepoQueue(config, rawRepoConnector);
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
    public void poll_removesHead() throws RawRepoException, SQLException, HarvesterException {
        final RecordId expectedRecordId1 = new RecordId("id1", 123456);
        final RecordId expectedRecordId2 = new RecordId("id2", 123456);
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId()))
                .thenReturn(new MockedQueueJob(
                        expectedRecordId1.getBibliographicRecordId(), expectedRecordId1.getAgencyId(), "worker", new Timestamp(new Date().getTime())))
                .thenReturn(new MockedQueueJob(
                    expectedRecordId2.getBibliographicRecordId(), expectedRecordId2.getAgencyId(), "worker", new Timestamp(new Date().getTime())));

        final RecordId recordId1 = queue.poll();
        final RecordId recordId2 = queue.poll();
        assertThat("recordId1", recordId1, is(expectedRecordId1));
        assertThat("recordId2", recordId2, is(expectedRecordId2));

        verify(rawRepoConnector, times(2)).dequeue(config.getContent().getConsumerId());
    }

    @Test
    public void peek_headRemains() throws RawRepoException, SQLException, HarvesterException {
        final RecordId expectedRecordId = new RecordId("id", 123456);
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId()))
                .thenReturn(new MockedQueueJob(
                        expectedRecordId.getBibliographicRecordId(), expectedRecordId.getAgencyId(), "worker", new Timestamp(new Date().getTime())));

        final RecordId recordId1 = queue.peek();
        final RecordId recordId2 = queue.peek();
        assertThat("recordId1", recordId1, is(expectedRecordId));
        assertThat("recordId2", recordId2, is(expectedRecordId));

        verify(rawRepoConnector, times(1)).dequeue(config.getContent().getConsumerId());
    }
}