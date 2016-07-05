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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecordSourceTest {
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()
                        .withConsumerId("consumerId"));
    private final RecordId recordId = new RecordId("bibliographicRecordId", 123456);
    private final Record record = new MockedRecord(recordId);
    private final QueueJob queueJob = new MockedQueueJob(
            recordId.getBibliographicRecordId(), recordId.getAgencyId(), "worker", new Timestamp(new Date().getTime()));

    private RecordSource recordSource;

    @Before
    public void createRecordSource() {
        recordSource = new RecordSource(config, rawRepoConnector);
    }

    @Test
    public void getRecord_rawRepoDequeueReturnsNull_returnsNull() throws HarvesterException {
        assertThat(recordSource.getRecord(), is(nullValue()));
    }

    @Test
    public void getRecord_rawRepoFetchRecordReturnsNull_returnsRecordWrapperWithError()
            throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenReturn(null);
        final RecordSource.RecordWrapper recordWrapper = recordSource.getRecord();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord(), is(Optional.empty()));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(instanceOf(HarvesterInvalidRecordException.class)));
    }

    @Test
    public void getRecord_rawRepoFetchRecordThrows_returnsRecordWrapperWithError()
            throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenThrow(new SQLException());
        final RecordSource.RecordWrapper recordWrapper = recordSource.getRecord();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord(), is(Optional.empty()));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(instanceOf(HarvesterSourceException.class)));
    }

    @Test
    public void getRecord_rawRepoFetchRecordReturnsRecord_returnsRecordWrapperWithRecord()
            throws RawRepoException, SQLException, HarvesterException {
        when(rawRepoConnector.dequeue(config.getContent().getConsumerId())).thenReturn(queueJob);
        when(rawRepoConnector.fetchRecord(recordId)).thenReturn(record);
        final RecordSource.RecordWrapper recordWrapper = recordSource.getRecord();
        assertThat("recordWrapper", recordWrapper, is(notNullValue()));
        assertThat("recordWrapper.getRecordId()", recordWrapper.getRecordId(), is(recordId));
        assertThat("recordWrapper.getRecord()", recordWrapper.getRecord().orElse(null), is(record));
        assertThat("recordWrapper.getError()", recordWrapper.getError(), is(nullValue()));
    }
}