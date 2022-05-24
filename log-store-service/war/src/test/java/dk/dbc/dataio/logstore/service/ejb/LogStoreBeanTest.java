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

package dk.dbc.dataio.logstore.service.ejb;

import dk.dbc.dataio.logstore.service.entity.LogEntryEntity;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogStoreBeanTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final String jobId = "jobId";
    private final long chunkId = 42L;
    private final long itemId = 1L;

    @Test
    public void getItemLog_noLogEntriesFound_returnsEmptyString() {
        when(entityManager.createNamedQuery(LogEntryEntity.QUERY_FIND_ITEM_ENTRIES)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final LogStoreBean logStoreBean = newLogStoreBean();
        final String itemLog = logStoreBean.getItemLog(jobId, chunkId, itemId);
        assertThat(itemLog.isEmpty(), is(true));
    }

    @Test
    public void getItemLog_multipleLogEntriesFound_returnsLog() {
        final LogEntryEntity logEntryEntity = new LogEntryEntity();
        logEntryEntity.setTimestamp(new Timestamp(new Date().getTime()));
        when(entityManager.createNamedQuery(LogEntryEntity.QUERY_FIND_ITEM_ENTRIES)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(logEntryEntity, logEntryEntity));

        final LogStoreBean logStoreBean = newLogStoreBean();
        final String itemLog = logStoreBean.getItemLog(jobId, chunkId, itemId);
        assertThat(itemLog.isEmpty(), is(false));
    }

    @Test
    public void getItemLog_singleLogEntriesFound_returnsLog() {
        final LogEntryEntity logEntryEntity = new LogEntryEntity();
        logEntryEntity.setTimestamp(new Timestamp(new Date().getTime()));
        logEntryEntity.setFormattedMessage("message");
        when(entityManager.createNamedQuery(LogEntryEntity.QUERY_FIND_ITEM_ENTRIES)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(logEntryEntity));

        final LogStoreBean logStoreBean = newLogStoreBean();
        final String itemLog = logStoreBean.getItemLog(jobId, chunkId, itemId);
        assertThat(itemLog.isEmpty(), is(false));
        assertThat(itemLog, is(logEntryEntity.getFormattedMessage() + LogStoreBean.ENTRY_SEPARATOR));
    }

    @Test
    public void deleteJobLog_ok() {
        when(entityManager.createNamedQuery(LogEntryEntity.QUERY_DELETE_ITEM_ENTRIES_FOR_JOB)).thenReturn(query);
        final LogStoreBean logStoreBean = newLogStoreBean();
        logStoreBean.deleteJobLog(jobId);
    }

    private LogStoreBean newLogStoreBean() {
        final LogStoreBean logStoreBean = new LogStoreBean();
        logStoreBean.entityManager = entityManager;
        return logStoreBean;
    }

}
