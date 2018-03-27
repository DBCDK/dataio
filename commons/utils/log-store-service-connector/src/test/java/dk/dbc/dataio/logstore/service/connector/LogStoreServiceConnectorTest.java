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

package dk.dbc.dataio.logstore.service.connector;

import dk.dbc.dataio.commons.types.rest.LogStoreServiceConstants;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,
})
public class LogStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String LOG_STORE_URL = "http://dataio/log-store";
    private static final String JOB_ID = "jobId";
    private static final long CHUNK_ID = 42;
    private static final long ITEM_ID = 1;

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new LogStoreServiceConnector(null, LOG_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new LogStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new LogStoreServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        assertThat(logStoreServiceConnector, is(notNullValue()));
        assertThat(logStoreServiceConnector.getHttpClient(), is(CLIENT));
        assertThat(logStoreServiceConnector.getBaseUrl(), is(LOG_STORE_URL));
    }

    @Test
    public void getItemLog_jobIdArgIsNull_throws() throws LogStoreServiceConnectorException {
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        try {
            logStoreServiceConnector.getItemLog(null, CHUNK_ID, ITEM_ID);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getItemLog_jobIdArgIsEmpty_throws() throws LogStoreServiceConnectorException {
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        try {
            logStoreServiceConnector.getItemLog("", CHUNK_ID, ITEM_ID);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test(expected = LogStoreServiceConnectorException.class)
    public void getItemLog_responseWithInternalServerErrorStatusCode_throws() throws LogStoreServiceConnectorException {
        getItemLog_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = LogStoreServiceConnectorException.class)
    public void getItemLog_responseWithNotFoundStatusCode_throws() throws LogStoreServiceConnectorException {
        getItemLog_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), "");
    }

    @Test
    public void getItemLog_logFound_returnsLog() throws LogStoreServiceConnectorException {
        final String expectedLog = "log";
        final String log = getItemLog_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedLog);
        assertThat(log, is(expectedLog));
    }

    @Test
    public void deleteItemLogs_LogsFound() throws LogStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(LogStoreServiceConstants.JOB_LOG_ENTRY_COLLECTION)
                .bind(LogStoreServiceConstants.JOB_ID_VARIABLE, JOB_ID);
        when(HttpClient.doDelete(CLIENT, LOG_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.NO_CONTENT.getStatusCode(), null));
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        logStoreServiceConnector.deleteJobLogs(JOB_ID);
    }

    private String getItemLog_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws LogStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(LogStoreServiceConstants.ITEM_LOG_ENTRY_COLLECTION)
                .bind(LogStoreServiceConstants.JOB_ID_VARIABLE, JOB_ID)
                .bind(LogStoreServiceConstants.CHUNK_ID_VARIABLE, CHUNK_ID)
                .bind(LogStoreServiceConstants.ITEM_ID_VARIABLE, ITEM_ID);
        when(HttpClient.doGet(CLIENT, LOG_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final LogStoreServiceConnector logStoreServiceConnector = newLogStoreServiceConnector();
        return logStoreServiceConnector.getItemLog(JOB_ID, CHUNK_ID, ITEM_ID);
    }

    private LogStoreServiceConnector newLogStoreServiceConnector() {
        return new LogStoreServiceConnector(CLIENT, LOG_STORE_URL);
    }
}