package dk.dbc.dataio.logstore.service.ejb;

import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
