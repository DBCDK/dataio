package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.json.ChunkJsonBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
        ServiceUtil.class
})
public class JobStoreServiceConnectorBeanTest {
    private final long jobId = 42;
    private final long chunkId = 1;
    final Map<String, String> pathVariables = new HashMap<>();
    {
        pathVariables.put(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
        pathVariables.put(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId));
    }
    private final String jobStoreUrl = "http://dataio/job-store";
    private final Client client = mock(Client.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getJobStoreServiceEndpoint())
                .thenReturn(jobStoreUrl);
        when(HttpClient.newClient()).thenReturn(client);
        when(HttpClient.interpolatePathVariables(JobStoreServiceConstants.JOB_CHUNK, pathVariables))
                .thenReturn("1/2/3/4");
    }

    @Test(expected = JobProcessorException.class)
    public void getChunk_endpointLookupThrowsNamingException_throws() throws JobProcessorException, NamingException {
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenThrow(new NamingException());
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        jobStoreServiceConnectorBean.getChunk(jobId, chunkId);
    }

    @Test(expected = JobProcessorException.class)
    public void getChunk_jobStoreReturnsNotFoundResponse_throws() throws JobProcessorException {
        when(HttpClient.doGet(any(Client.class), eq(jobStoreUrl), any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), ""));
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        jobStoreServiceConnectorBean.getChunk(jobId, chunkId);
    }

    @Test(expected = JobProcessorException.class)
    public void getChunk_jobStoreReturnsInternalServerErrorResponse_throws() throws JobProcessorException {
        when(HttpClient.doGet(any(Client.class), eq(jobStoreUrl), any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        jobStoreServiceConnectorBean.getChunk(jobId, chunkId);
    }

    @Test(expected = JobProcessorException.class)
    public void getChunk_jobStoreReturnsInvalidChunkEntity_throws() throws JobProcessorException {
        when(HttpClient.doGet(any(Client.class), eq(jobStoreUrl), any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), "invalid"));
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        jobStoreServiceConnectorBean.getChunk(jobId, chunkId);
    }

    @Test
    public void getChunk_jobStoreReturnsChunkEntity_returnsChunkInstance() throws JobProcessorException {
        final long expectedChunkId = 42;
        final String expectedChunk = new ChunkJsonBuilder()
                .setId(expectedChunkId)
                .build();
        when(HttpClient.doGet(any(Client.class), eq(jobStoreUrl), any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedChunk));
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        final Chunk chunk = jobStoreServiceConnectorBean.getChunk(jobId, chunkId);
        assertThat(chunk.getId(), is(expectedChunkId));
    }

    private JobStoreServiceConnectorBean getInitializedBean() {
        return new JobStoreServiceConnectorBean();
    }
}
