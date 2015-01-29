package dk.dbc.dataio.commons.utils.newjobstore;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SupplementaryProcessDataBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
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
    private static final int JOB_ID = 3;
    private static final int CHUNK_ID = 44;

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

    @Test
    public void addJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            addJob_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.BAD_REQUEST.getStatusCode(), jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void addJob_jobIsCreated() throws JobStoreServiceConnectorException {
        addJob_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), new JobInfoSnapshotBuilder().build());
    }

    // ******************************************* add chunk tests *******************************************

    @Test(expected = NullPointerException.class)
    public void addChunk_externalChunkArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(null, JOB_ID, CHUNK_ID);
    }

    @Test(expected = NullPointerException.class)
    public void addChunk_chunkTypeIsNull_throws() throws JobStoreServiceConnectorException {
        final ExternalChunk chunk = new ExternalChunkBuilder(null).build();
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChunk_chunkTypePartitioned_throws() throws JobStoreServiceConnectorException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).build();
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID);
    }

    @Test
    public void addChunk_chunkTypeProcessed_chunkIsAdded() throws JobStoreServiceConnectorException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();
        final JobInfoSnapshot jobInfoSnapshot = addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                chunk,
                chunk.getJobId(),
                chunk.getChunkId(),
                JobStoreServiceConstants.JOB_CHUNK_PROCESSED,
                Response.Status.CREATED.getStatusCode(),
                new JobInfoSnapshotBuilder().setJobId(JOB_ID).build());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat((long) jobInfoSnapshot.getJobId(), is(chunk.getJobId()));
    }

    @Test
    public void addChunk_chunkTypeDelivering_chunkIsAdded() throws JobStoreServiceConnectorException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build();
        final JobInfoSnapshot jobInfoSnapshot = addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                chunk,
                chunk.getJobId(),
                chunk.getChunkId(),
                JobStoreServiceConstants.JOB_CHUNK_DELIVERED,
                Response.Status.CREATED.getStatusCode(),
                new JobInfoSnapshotBuilder().setJobId(JOB_ID).build());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat((long) jobInfoSnapshot.getJobId(), is(chunk.getJobId()));
    }

    @Test
    public void addChunk_badRequestResponse_throws() throws JobStoreServiceConnectorException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();
        final JobError jobError = new JobError(JobError.Code.INVALID_CHUNK_IDENTIFIER, "description", null);
        try {
            addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                    chunk,
                    chunk.getJobId(),
                    chunk.getChunkId(),
                    JobStoreServiceConstants.JOB_CHUNK_PROCESSED,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addChunk_responseWithNullValuedEntity_throws() throws JobStoreServiceConnectorException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build();
        addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                chunk,
                chunk.getJobId(),
                chunk.getChunkId(),
                JobStoreServiceConstants.JOB_CHUNK_DELIVERED,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                null);
    }

    // ******************************************* listJobs() tests *******************************************

    @Test
    public void listJobs_criteriaArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.listJobs(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listJobs_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        try {
            listJobsWithMockedHttpResponse(new JobListCriteria(), 500, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(500));
        }
    }

    @Test
    public void listJobs_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        try {
            listJobsWithMockedHttpResponse(new JobListCriteria(), 200, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void listJobs_serviceReturnsEmptyListEntity_returnsEmptyList() throws JobStoreServiceConnectorException {
        final List<JobInfoSnapshot> expectedSnapshots = Collections.emptyList();
        final List<JobInfoSnapshot> returnedSnapshots = listJobsWithMockedHttpResponse(new JobListCriteria(), 200, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    @Test
    public void listJobs_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final List<JobInfoSnapshot> expectedSnapshots = Arrays.asList(new JobInfoSnapshotBuilder().build());
        final List<JobInfoSnapshot> returnedSnapshots = listJobsWithMockedHttpResponse(new JobListCriteria(), 200, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    // ******************************************* getResourceBundle() tests *******************************************

    @Test(expected = IllegalArgumentException.class)
    public void getResourceBundle_jobIdArgIsLessThanBound_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.getResourceBundle(-1);
        fail("No exception thrown");
    }

    @Test
    public void getResourceBundle_badRequestResponse_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "description", null);
        try {
            getResourceBundle_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.BAD_REQUEST.getStatusCode(), jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getResourceBundle_responseWithNullValuedEntity_throws() throws JobStoreServiceConnectorException {
        getResourceBundle_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null);
    }

    @Test
    public void getResourceBundle_bundleCreated_returnsResourceBundle() throws JobStoreServiceConnectorException {
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();
        SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessDataBuilder().build();
        final ResourceBundle expectedResourceBundle = new ResourceBundle(flow, sink, supplementaryProcessData);

        final ResourceBundle resourceBundle = getResourceBundle_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedResourceBundle);

        assertThat("ResourceBundle", resourceBundle, not(nullValue()));
        assertThat("ResourceBundle.flow", resourceBundle.getFlow(), is(expectedResourceBundle.getFlow()));
        assertThat("ResourceBundle.sink", resourceBundle.getSink(), is(expectedResourceBundle.getSink()));
        assertThat("ResourceBundle.supplementaryProcessData", resourceBundle.getSupplementaryProcessData(), is(expectedResourceBundle.getSupplementaryProcessData()));
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

    private JobInfoSnapshot addChunk_mockedHttpWithSpecifiedReturnErrorCode(ExternalChunk chunk, long jobId, long chunkId, String pathString, int statusCode, Object returnValue) throws JobStoreServiceConnectorException {
        when(HttpClient.doPostWithJson(CLIENT, chunk, JOB_STORE_URL, buildAddChunkPath(jobId, chunkId, pathString)))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.addChunk(chunk, jobId, chunkId);
    }

    private List<JobInfoSnapshot> listJobsWithMockedHttpResponse(
            JobListCriteria criteria, int statusCode, List<JobInfoSnapshot> responseEntity) throws JobStoreServiceConnectorException {

        when(HttpClient.doPostWithJson(CLIENT, criteria, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES))
                .thenReturn(new MockedResponse<>(statusCode, responseEntity));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.listJobs(criteria);
    }

    private ResourceBundle getResourceBundle_mockedHttpWithSpecifiedReturnErrorCode(
            int statusCode, Object returnValue) throws JobStoreServiceConnectorException {

        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_RESOURCEBUNDLE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, JOB_ID);
        when(HttpClient.doGet(CLIENT, JOB_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.getResourceBundle(JOB_ID);

    }

    private String[] buildAddChunkPath(long jobId, long chunkId, String pathString) {
        return new PathBuilder(pathString)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId).build();
    }

    private static JobInputStream getNewJobInputStream() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        return new JobInputStream(jobSpecification, false, PART_NUMBER);
    }

    private static JobStoreServiceConnector newJobStoreServiceConnector() {
        return new JobStoreServiceConnector(CLIENT, JOB_STORE_URL);
    }
}
