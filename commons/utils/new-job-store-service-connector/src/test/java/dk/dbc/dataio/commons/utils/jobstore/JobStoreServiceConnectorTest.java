package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
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

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        addJob_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test
    public void addJob_jobIsCreated() throws JobStoreServiceConnectorException {
        addJob_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), getJobInfoSnapshot());
    }

    // ******************************************* add chunk tests *******************************************

    @Test(expected = NullPointerException.class)
    public void addChunk_externalChunkArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(null, JOB_ID, CHUNK_ID);
    }

    @Test (expected = NullPointerException.class)
    public void addChunk_chunkTypeIsNull_trows() throws JobStoreServiceConnectorException {
        ExternalChunk chunk = getExternalChunk(null);
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addChunk_chunkTypePartitioned_trows() throws JobStoreServiceConnectorException {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PARTITIONED);
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID);
    }

    @Test
    public void addChunk_chunkTypeProcessed_chunkIsAdded() throws JobStoreServiceConnectorException {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PROCESSED);
        JobInfoSnapshot jobInfoSnapshot = addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                chunk,
                chunk.getJobId(),
                chunk.getChunkId(),
                JobStoreServiceConstants.JOB_CHUNK_PROCESSED,
                Response.Status.CREATED.getStatusCode(),
                getJobInfoSnapshot());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat(Long.valueOf(jobInfoSnapshot.getJobId()).longValue(), is(chunk.getJobId()));
    }

    @Test
    public void addChunk_chunkTypeDelivering_chunkIsAdded() throws JobStoreServiceConnectorException {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.DELIVERED);
        JobInfoSnapshot jobInfoSnapshot = addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                chunk,
                chunk.getJobId(),
                chunk.getChunkId(),
                JobStoreServiceConstants.JOB_CHUNK_DELIVERED,
                Response.Status.CREATED.getStatusCode(),
                getJobInfoSnapshot());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat(Long.valueOf(jobInfoSnapshot.getJobId()).longValue(), is(chunk.getJobId()));
    }

    @Test(expected = JobStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void addChunk_badRequestResponse_throws() throws JobStoreServiceConnectorException {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PROCESSED);
        addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                chunk,
                chunk.getJobId(),
                chunk.getChunkId(),
                JobStoreServiceConstants.JOB_CHUNK_PROCESSED,
                Response.Status.BAD_REQUEST.getStatusCode(),
                null);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addChunk_responseWithNullValuedEntity_throws() throws JobStoreServiceConnectorException {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.DELIVERED);
        addChunk_mockedHttpWithSpecifiedReturnErrorCode(
                chunk,
                chunk.getJobId(),
                chunk.getChunkId(),
                JobStoreServiceConstants.JOB_CHUNK_DELIVERED,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                null);
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

    private String[] buildAddChunkPath(long jobId, long chunkId, String pathString) {
        return new PathBuilder(pathString)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId).build();
    }

    private static JobInputStream getNewJobInputStream() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        return new JobInputStream(jobSpecification, false, PART_NUMBER);
    }

    private static JobInfoSnapshot getJobInfoSnapshot() {
        return new JobInfoSnapshot(
                JOB_ID,
                false,
                2344,
                10,
                10,
                new Date(),
                new Date(),
                null,
                new JobSpecificationBuilder().build(),
                new State(),
                "FlowName",
                "SinkName");
    }

    private static JobStoreServiceConnector newJobStoreServiceConnector() {
        return new JobStoreServiceConnector(CLIENT, JOB_STORE_URL);
    }

    private static ExternalChunk getExternalChunk(ExternalChunk.Type type) {
        return new ExternalChunk(JOB_ID, CHUNK_ID, type);
    }

}
