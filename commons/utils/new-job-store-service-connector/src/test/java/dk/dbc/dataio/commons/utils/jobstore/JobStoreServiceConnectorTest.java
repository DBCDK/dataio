package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class
})
public class JobStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String JOB_STORE_URL = "http://dataio/job-store";
    private static final int PART_NUMBER = 414243;

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

    // ******************************************* add job tests ********************************************

    @Test(expected = NullPointerException.class)
    public void addJob_jobInputStreamArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addJob(null);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addJob_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        addJob_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        addJob_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test
    public void addJob_jobIsCreated() throws JobStoreServiceConnectorException {
        addJob_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), getJobInfoSnapshot());
    }

    /*
     * Private methods
     */

    // Helper method
    private void addJob_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws JobStoreServiceConnectorException {
        final JobInputStream jobInputStream = getNewJobInputStream();
        when(HttpClient.doPostWithJson(CLIENT, jobInputStream, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.addJob(jobInputStream);
    }

    private static JobInputStream getNewJobInputStream() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        return new JobInputStream(jobSpecification, false, PART_NUMBER);
    }

    private static JobInfoSnapshot getJobInfoSnapshot() {
        return new JobInfoSnapshot(
                42,
                false,
                2344,
                10,
                10,
                new Date(System.currentTimeMillis()),
                new Date(System.currentTimeMillis()),
                null,
                new JobSpecificationBuilder().build(),
                new State(),
                "FlowName",
                "SinkName");
    }

    private static JobStoreServiceConnector newJobStoreServiceConnector() {
        return new JobStoreServiceConnector(CLIENT, JOB_STORE_URL);
    }

}
