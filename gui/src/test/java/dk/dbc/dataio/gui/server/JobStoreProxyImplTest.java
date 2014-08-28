package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
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
public class JobStoreProxyImplTest {
    private final String jobStoreServiceUrl = "http://dataio/job-service";
    private final String jobStoreFilesystemUrl = "http://dataio/job-store/filesystem";
    private final Client client = mock(Client.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenReturn(jobStoreServiceUrl);
        when(ServiceUtil.getJobStoreFilesystemUrl()).thenReturn(jobStoreFilesystemUrl);
        when(HttpClient.newClient()).thenReturn(client);
    }

    @Test(expected = ProxyException.class)
    public void getJobStoreFilesystemUrl_jobStoreServiceEndpointCanNotBeLookedUp_throws() throws NamingException, ProxyException {
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenThrow(new NamingException());

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        try {
            jobStoreProxy.getJobStoreFilesystemUrl();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.SERVICE_NOT_FOUND));
            throw e;
        }
    }

    @Test
    public void getJobStoreFilesystemUrl_success_jobStoreFilesystemUrlReturned() throws NamingException, ProxyException {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        String jobStoreFilesystemUrl = jobStoreProxy.getJobStoreFilesystemUrl();
        assertThat(jobStoreFilesystemUrl, is(jobStoreFilesystemUrl));
    }

    @Test(expected = ProxyException.class)
    public void findAllSinks_jobStoreServiceEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenThrow(new NamingException());

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        try {
            jobStoreProxy.findAllJobs();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.SERVICE_NOT_FOUND));
            throw e;
        }
    }

    @Test(expected = ProxyException.class)
    public void findAllSinks_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doGet(any(Client.class), eq(jobStoreServiceUrl), eq(JobStoreServiceConstants.JOB_COLLECTION)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        try {
            jobStoreProxy.findAllJobs();
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
            throw e;
        }
    }

    @Test
    public void findAllSinks_remoteServiceReturnsHttpStatusOk_returnsListOfSinkEntity() throws Exception {
        final JobInfo job = new JobInfoBuilder().setJobId(666L).build();
        when(HttpClient.doGet(any(Client.class), eq(jobStoreServiceUrl), eq(JobStoreServiceConstants.JOB_COLLECTION)))
                .thenReturn(new MockedHttpClientResponse<List<JobInfo>>(Response.Status.OK.getStatusCode(), Arrays.asList(job)));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl();
        final List<JobInfo> allJobs = jobStoreProxy.findAllJobs();
        assertThat(allJobs.size(), is(1));
        assertThat(allJobs.get(0).getJobId(), is(job.getJobId()));
    }
}
