package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class JobStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String JOB_STORE_URL = "http://dataio/job-store";

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new JobStoreServiceConnector(null, JOB_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new JobStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new JobStoreServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(JOB_STORE_URL));
    }

    @Test(expected = NullPointerException.class)
    public void createJob_jobSpecificationArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.createJob(null);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void createJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, jobSpecification, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.createJob(jobSpecification);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void createJob_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, jobSpecification, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), null));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.createJob(jobSpecification);
    }

    @Test(expected = JobStoreServiceConnectorJobCreationFailedException.class)
    public void createJob_responseWithJobInfoWithErrorCodeIndicatingJobCreationFailure_throws() throws JobStoreServiceConnectorException, JsonException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInfo jobInfo = new JobInfoBuilder()
                .setJobErrorCode(JobErrorCode.DATA_FILE_INVALID)
                .build();
        when(HttpClient.doPostWithJson(CLIENT, jobSpecification, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), jobInfo));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.createJob(jobSpecification);
    }

    @Test
    public void createJob_jobIsCreated_returnsJobInfo() throws JobStoreServiceConnectorException, JsonException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInfo expectedJobInfo = new JobInfoBuilder()
                .setJobErrorCode(JobErrorCode.NO_ERROR)
                .build();
        when(HttpClient.doPostWithJson(CLIENT, jobSpecification, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), expectedJobInfo));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        final JobInfo jobInfo = instance.createJob(jobSpecification);
        assertThat(jobInfo, is(notNullValue()));
        assertThat(jobInfo.getJobId(), is(expectedJobInfo.getJobId()));
    }

    private static JobStoreServiceConnector newJobStoreServiceConnector() {
        return new JobStoreServiceConnector(CLIENT, JOB_STORE_URL);
    }
}
