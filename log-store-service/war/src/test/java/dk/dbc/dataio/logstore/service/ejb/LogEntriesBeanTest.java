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

import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogEntriesBeanTest {
    private final LogStoreBean logStoreBean = mock(LogStoreBean.class);
    private final String jobId = "jobId";
    private final Long chunkId = 42L;
    private final Long itemId = 1L;

    @Test
    public void getItemLog_noEntriesFoundInLogStore_returnsStatusNotFoundResponse() {
        when(logStoreBean.getItemLog(jobId, chunkId, itemId)).thenReturn("");

        final LogEntriesBean logEntriesBean = newLogEntriesBean();
        final Response response = logEntriesBean.getItemLog(jobId, chunkId, itemId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getItemLog_entriesFoundInLogStore_returnsStatusOkResponse() {
        final String logEntries = "entries";
        when(logStoreBean.getItemLog(jobId, chunkId, itemId)).thenReturn(logEntries);

        final LogEntriesBean logEntriesBean = newLogEntriesBean();
        final Response response = logEntriesBean.getItemLog(jobId, chunkId, itemId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(logEntries));
    }

    @Test
    public void deleteJobLog_returnsStatusNoContent() {
        final LogEntriesBean logEntriesBean = newLogEntriesBean();
        final Response response = logEntriesBean.deleteJobLog(jobId);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    private LogEntriesBean newLogEntriesBean() {
        final LogEntriesBean logEntriesBean = new LogEntriesBean();
        logEntriesBean.logStoreBean = logStoreBean;
        return logEntriesBean;
    }
}
