package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.ChunkCompletionState;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkCompletionStateBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobCompletionStateBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.Format;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,
    ServiceUtil.class,
    Format.class
})
public class JobStoreProxyImplTest {
    private final String jobStoreServiceUrl = "http://dataio/job-service";
    private final String jobStoreFilesystemUrl = "http://dataio/job-store/filesystem";
    private final Client client = mock(Client.class);

    private final long ITEM_ID = 737L;
    private final JobCompletionState defaultJobCompletionState = new JobCompletionStateBuilder()
            .addChunk(new ChunkCompletionStateBuilder()
                    .addItem(new ItemCompletionState(ITEM_ID, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS))
                    .build())
            .build();


    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        mockStatic(Format.class);
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenReturn(jobStoreServiceUrl);
        when(ServiceUtil.getJobStoreFilesystemUrl()).thenReturn(jobStoreFilesystemUrl);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }

    @Test(expected = NamingException.class)
    public void constructor_jobStoreServiceEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenThrow(new NamingException());

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
    }

    @Test
    public void getJobStoreFilesystemUrl_jobStoreServiceEndpointCanNotBeLookedUp_throws() throws NamingException, ProxyException {
        when(ServiceUtil.getJobStoreFilesystemUrl()).thenThrow(new NamingException());

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        try {
            jobStoreProxy.getJobStoreFilesystemUrl();
            fail("JobStoreFileSystemUrl was successfully looked up, where a ProxyException was expected");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.SERVICE_NOT_FOUND));
        }
    }

    @Test
    public void getJobStoreFilesystemUrl_success_jobStoreFilesystemUrlReturned() throws NamingException, ProxyException {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        String jobStoreFilesystemUrl = jobStoreProxy.getJobStoreFilesystemUrl();
        assertThat(jobStoreFilesystemUrl, is(jobStoreFilesystemUrl));
    }

    @Test
    public void findAllJobsOld_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(jobStoreServiceUrl), eq(JobStoreServiceConstants.JOB_COLLECTION)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        try {
            jobStoreProxy.findAllJobs();
            fail("The call to jobStoreProxy.findAllJobs() succeeded, where a ProxyException(INTERNAL_SERVER_ERROR) was expected");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllJobsOld_remoteServiceReturnsHttpStatusOk_returnsListOfJobEntity() throws Exception {
        final JobInfo job = new JobInfoBuilder().setJobId(666L).build();
        when(HttpClient.doGet(any(Client.class), eq(jobStoreServiceUrl), eq(JobStoreServiceConstants.JOB_COLLECTION)))
                .thenReturn(new MockedHttpClientResponse<List<JobInfo>>(Response.Status.OK.getStatusCode(), Arrays.asList(job)));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        final List<JobInfo> allJobs = jobStoreProxy.findAllJobs();
        assertThat(allJobs.size(), is(1));
        assertThat(allJobs.get(0).getJobId(), is(job.getJobId()));
    }

    @Test
    public void findAllJobs_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(jobStoreServiceUrl), eq(JobStoreServiceConstants.JOB_COLLECTION)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        try {
            jobStoreProxy.findAllJobsNew();
            fail("The call to jobStoreProxy.findAllJobs() succeeded, where a ProxyException(INTERNAL_SERVER_ERROR) was expected");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
        }
    }

    @Test
    public void findAllJobs_remoteServiceReturnsHttpStatusOk_returnsListOfJobEntity() throws Exception {
        final JobInfo job = new JobInfoBuilder().setJobId(666L).build();
        when(HttpClient.doGet(any(Client.class), eq(jobStoreServiceUrl), eq(JobStoreServiceConstants.JOB_COLLECTION)))
                .thenReturn(new MockedHttpClientResponse<List<JobInfo>>(Response.Status.OK.getStatusCode(), Arrays.asList(job)));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        final List<JobModel> allJobs = jobStoreProxy.findAllJobsNew();
        assertThat(allJobs.size(), is(1));
        assertThat(allJobs.get(0).getJobId(), is(String.valueOf(job.getJobId())));
    }

    @Test(expected = ProxyException.class)
    public void getJobCompletionState_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException, NamingException {
        final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

        when(jobStoreServiceConnector.getJobCompletionState(JobCompletionStateBuilder.DEFAULT_JOB_ID)).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);

        JobCompletionState jobCompletionState = jobStoreProxy.getJobCompletionState(JobCompletionStateBuilder.DEFAULT_JOB_ID);
    }

    @Test
    public void getJobCompletionState_correct_success() throws ProxyException, JobStoreServiceConnectorException, NamingException {
        final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

        when(jobStoreServiceConnector.getJobCompletionState(JobCompletionStateBuilder.DEFAULT_JOB_ID)).thenReturn(defaultJobCompletionState);
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);

        JobCompletionState jobCompletionState = jobStoreProxy.getJobCompletionState(JobCompletionStateBuilder.DEFAULT_JOB_ID);

        assertThat(jobCompletionState.getJobId(), is(JobCompletionStateBuilder.DEFAULT_JOB_ID));
        List<ChunkCompletionState> chunks = jobCompletionState.getChunks();
        assertThat(chunks.size(), is(1));
        assertThat(chunks.get(0).getChunkId(), is(ChunkCompletionStateBuilder.DEFAULT_CHUNK_ID));
        List<ItemCompletionState> items = chunks.get(0).getItems();
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getItemId(), is(ITEM_ID));
        assertThat(items.get(0).getProcessingState(), is(ItemCompletionState.State.SUCCESS));
    }

}
