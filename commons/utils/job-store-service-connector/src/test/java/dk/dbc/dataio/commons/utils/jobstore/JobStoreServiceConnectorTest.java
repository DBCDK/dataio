package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
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
    private static final long JOB_ID = 42;
    private static final long CHUNK_ID = 1;

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

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getSink_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getSink(JOB_ID);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getSink_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getSink(JOB_ID);
    }

    @Test
    public void getSink_sinkRetrieved_returnsSink() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_SINK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        final Sink expectedSink = new SinkBuilder().build();
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedSink));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        final Sink sink = instance.getSink(JOB_ID);
        assertThat(sink, is(notNullValue()));
        assertThat(sink, is(expectedSink));
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getState_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_STATE), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getState(JOB_ID);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getState_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_STATE), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getState(JOB_ID);
    }

    @Test
    public void getState_stateRetrieved_returnsState() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_STATE), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        final JobState expectedJobState = new JobState();
        expectedJobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.DONE);
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedJobState));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        final JobState jobState = instance.getState(JOB_ID);
        assertThat(jobState, is(notNullValue()));
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getChunk_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_CHUNK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getChunk(JOB_ID, CHUNK_ID);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getChunk_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_CHUNK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getChunk(JOB_ID, CHUNK_ID);
    }

    @Test
    public void getChunk_chunkRetrieved_returnsChunk() throws JobStoreServiceConnectorException {
        final Chunk expectedChunk = new ChunkBuilder().build();
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_CHUNK), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedChunk));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        final Chunk chunk = instance.getChunk(JOB_ID, CHUNK_ID);
        assertThat(chunk, is(notNullValue()));
        assertThat(chunk.getJobId(), is(expectedChunk.getJobId()));
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getSinkChunkResult_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_DELIVERED), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getSinkChunkResult(JOB_ID, CHUNK_ID);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void getSinkChunkResult_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_DELIVERED), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        instance.getSinkChunkResult(JOB_ID, CHUNK_ID);
    }

    @Test
    public void getSinkChunkResult_sinkChunkResultRetrieved_returnsSinkChunkResult() throws JobStoreServiceConnectorException {
        final SinkChunkResult expectedSinkChunkResult = new SinkChunkResultBuilder().build();
        when(HttpClient.interpolatePathVariables(eq(JobStoreServiceConstants.JOB_DELIVERED), Matchers.<Map<String, String>>any()))
                .thenReturn("path");
        when(HttpClient.doGet(eq(CLIENT), eq(JOB_STORE_URL), (String) anyVararg()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), expectedSinkChunkResult));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        final SinkChunkResult sinkChunkResult = instance.getSinkChunkResult(JOB_ID, CHUNK_ID);
        assertThat(sinkChunkResult, is(notNullValue()));
        assertThat(sinkChunkResult.getJobId(), is(expectedSinkChunkResult.getJobId()));
    }

    private static JobStoreServiceConnector newJobStoreServiceConnector() {
        return new JobStoreServiceConnector(CLIENT, JOB_STORE_URL);
    }
}
