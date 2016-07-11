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
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.MockedQueueJob;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RawRepoQueueTest {
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()
                        .withConsumerId("consumerId"));
    private final RecordId recordId = new RecordId("bibliographicRecordId", 123456);
    private final Record record = new MockedRecord(recordId);
    private final QueueJob queueJob = new MockedQueueJob(
            recordId.getBibliographicRecordId(), recordId.getAgencyId(), "worker", new Timestamp(new Date().getTime()));

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
    public void poll_rawRepoFetchRecordReturnsNull_returnsRecordWrapperWithError() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenReturn(null);
        final RecordWrapper recordWrapper = queue.poll();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord(), is(Optional.empty()));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(instanceOf(HarvesterInvalidRecordException.class)));
    }

    @Test
    public void peek_rawRepoFetchRecordReturnsNull_returnsRecordWrapperWithError() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenReturn(null);
        final RecordWrapper recordWrapper = queue.peek();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord(), is(Optional.empty()));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(instanceOf(HarvesterInvalidRecordException.class)));
    }

    @Test
    public void poll_rawRepoFetchRecordThrows_returnsRecordWrapperWithError() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenThrow(new SQLException());
        final RecordWrapper recordWrapper = queue.poll();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord(), is(Optional.empty()));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(instanceOf(HarvesterSourceException.class)));
    }

    @Test
    public void peek_rawRepoFetchRecordThrows_returnsRecordWrapperWithError() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenThrow(new SQLException());
        final RecordWrapper recordWrapper = queue.peek();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord(), is(Optional.empty()));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(instanceOf(HarvesterSourceException.class)));
    }

    @Test
    public void poll_rawRepoFetchRecordReturnsRecord_returnsRecordWrapperWithRecord() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenReturn(record);
        final RecordWrapper recordWrapper = queue.poll();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord().orElse(null), is(record));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(nullValue()));
    }

    @Test
    public void peek_rawRepoFetchRecordReturnsRecord_returnsRecordWrapperWithRecord() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenReturn(record);
        final RecordWrapper recordWrapper = queue.peek();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord().orElse(null), is(record));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(nullValue()));
    }

    @Test
    public void poll_removesHead() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId))
                .thenReturn(record)
                .thenReturn(new MockedRecord(recordId));

        final RecordWrapper recordWrapper1 = queue.poll();
        final RecordWrapper recordWrapper2 = queue.poll();
        assertThat("recordWrapper1", recordWrapper1, is(notNullValue()));
        assertThat("recordWrapper2", recordWrapper2, is(notNullValue()));
        assertThat("recordWrapper1 == recordWrapper2", recordWrapper1, is(not(recordWrapper2)));

        verify(rawRepoConnector, times(2)).dequeue(config.getContent().getConsumerId());
        verify(rawRepoConnector, times(2)).fetchRecord(recordId);
    }

    @Test
    public void peek_headRemains() throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenReturn(record);

        final RecordWrapper recordWrapper1 = queue.peek();
        final RecordWrapper recordWrapper2 = queue.peek();
        final RecordWrapper recordWrapper3 = queue.peek();
        assertThat("recordWrapper1", recordWrapper1, is(notNullValue()));
        assertThat("recordWrapper1 == recordWrapper2", recordWrapper2, is(recordWrapper1));
        assertThat("recordWrapper1 == recordWrapper3", recordWrapper3, is(recordWrapper1));

        verify(rawRepoConnector, times(1)).dequeue(config.getContent().getConsumerId());
        verify(rawRepoConnector, times(1)).fetchRecord(recordId);
    }
}