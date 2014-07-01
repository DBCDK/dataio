package dk.dbc.dataio.jobstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
  * JobsBean unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Client.class,
        HttpClient.class,
        ServiceUtil.class,
        UriInfo.class,
        JsonUtil.class
})
public class JobsBeanTest {
    private final long jobId = 42;
    private final long chunkId = 1;
    private final String flowStoreUrl = "http://dataio/flow-store";
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final Client client = mock(Client.class);
    private final JobStoreBean jobStoreBean = mock(JobStoreBean.class);
    private final JobStore jobStore = mock(JobStore.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint())
                .thenReturn(flowStoreUrl);
        when(HttpClient.newClient()).thenReturn(client);
        when(jobStoreBean.getJobStore()).thenReturn(jobStore);
    }

    @Test(expected = NullPointerException.class)
    public void createJob_jobSpecDataIsNull_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createJob_jobSpecDataIsEmpty_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, "");
    }

    @Test(expected = JsonException.class)
    public void createJob_jobSpecDataIsInvalidJson_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, "{");
    }

    @Test(expected = JsonException.class)
    public void createJob_jobSpecDataIsInvalidJobSpecification_throws() throws Exception {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, "{\"name\": \"name\"}");
    }

    @Test(expected = EJBException.class)
    public void createJob_jndiLookupThrowsNamingException_throws() throws Exception {
        final String jobSpecData = getValidJobSpecificationString();
        when(ServiceUtil.getFlowStoreServiceEndpoint())
                .thenThrow(new NamingException());

        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, jobSpecData);
    }

    @Test(expected = ReferencedEntityNotFoundException.class)
    public void createJob_noFlowBinderCanBeFound_throws() throws Exception {
        final String jobSpecData = getValidJobSpecificationString();
        when(HttpClient.doGet(any(Client.class), Matchers.<Map<String, Object>>any(), eq(flowStoreUrl), eq(FlowStoreServiceConstants.FLOW_BINDERS), eq(JobsBean.REST_FLOWBINDER_QUERY_ENTRY_POINT)))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), ""));
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, jobSpecData);
    }

    @Ignore
    @Test
    public void createJob_jobIsCreated_returnsStatusCreatedResponse() throws Exception {
        long flowId = 42L;
        final String flowData = new FlowJsonBuilder().setId(flowId).build();
        long flowBinderId = 31L;
        final String flowBinderData = new FlowBinderJsonBuilder().setId(flowBinderId).build();
        final String jobSpecData = getValidJobSpecificationString();
        final String jobInfoData = new JobInfoJsonBuilder().build();
        final Job job = new Job(JsonUtil.fromJson(jobInfoData, JobInfo.class, MixIns.getMixIns()), new JobState(),
                JsonUtil.fromJson(flowData, Flow.class, MixIns.getMixIns()));

        when(HttpClient.doGet(any(Client.class), Matchers.<Map<String, Object>>any(), eq(flowStoreUrl), eq(FlowStoreServiceConstants.FLOW_BINDERS), eq(JobsBean.REST_FLOWBINDER_QUERY_ENTRY_POINT)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowBinderData));
        when(HttpClient.doGet(any(Client.class), eq(flowStoreUrl), eq(FlowStoreServiceConstants.FLOWS), eq(Long.toString(flowId))))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowData));

        final JobsBean jobsBean = new JobsBean();
        final Response response = jobsBean.createJob(uriInfo, jobSpecData);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
    }

    @Test
    public void getChunk_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        when(jobStore.getChunk(jobId, chunkId)).thenReturn(null);

        final Response response = getJobsBean().getChunk(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getChunk_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        when(jobStore.getChunk(jobId, chunkId)).thenThrow(new JobStoreException("JobStoreException"));

        getJobsBean().getChunk(jobId, chunkId);
    }

    @Test(expected = JobStoreException.class)
    public void getChunk_chunkCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(Chunk.class))).thenThrow(new JsonException("JsonException"));
        when(jobStore.getChunk(jobId, chunkId)).thenReturn(new ChunkBuilder().build());

        final Response response = getJobsBean().getChunk(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getChunk_jobStoreReturnsChunk_returnsStatusOkResponseWithChunkEntity() throws JobStoreException, JsonException {
        final Chunk chunk = new ChunkBuilder().build();
        when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunk);

        final Response response = getJobsBean().getChunk(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("chunkId").longValue(), is(chunk.getChunkId()));
    }

    @Test
    public void getSink_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        when(jobStore.getSink(jobId)).thenReturn(null);

        final Response response = getJobsBean().getSink(jobId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getSink_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        when(jobStore.getSink(jobId)).thenThrow(new JobStoreException("JobStoreException"));

        getJobsBean().getSink(jobId);
    }

    @Test(expected = JobStoreException.class)
    public void getSink_sinkCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(Sink.class))).thenThrow(new JsonException("JsonException"));
        when(jobStore.getSink(jobId)).thenReturn(new SinkBuilder().build());

        final Response response = getJobsBean().getSink(jobId);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getSink_jobStoreReturnsSink_returnsStatusOkResponseWithEntity() throws JobStoreException, JsonException {
        when(jobStore.getSink(jobId)).thenReturn(new SinkBuilder().build());

        final Response response = getJobsBean().getSink(jobId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void getState_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        when(jobStore.getJobState(jobId)).thenReturn(null);

        final Response response = getJobsBean().getState(jobId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getState_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        when(jobStore.getJobState(jobId)).thenThrow(new JobStoreException("JobStoreException"));

        getJobsBean().getState(jobId);
    }

    @Test(expected = JobStoreException.class)
    public void getState_stateCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(JobState.class))).thenThrow(new JsonException("JsonException"));
        when(jobStore.getJobState(jobId)).thenReturn(new JobState());

        final Response response = getJobsBean().getState(jobId);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getState_jobStoreReturnsState_returnsStatusOkResponseWithEntity() throws JobStoreException, JsonException {
        when(jobStore.getJobState(jobId)).thenReturn(new JobState());

        final Response response = getJobsBean().getState(jobId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    @Test
    public void getProcessorResult_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(null);

        final Response response = getJobsBean().getProcessorResult(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getProcessorResult_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        when(jobStore.getProcessorResult(jobId, chunkId)).thenThrow(new JobStoreException("JobStoreException"));

        getJobsBean().getProcessorResult(jobId, chunkId);
    }

    @Test(expected = JobStoreException.class)
    public void getProcessorResult_chunkCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(Chunk.class))).thenThrow(new JsonException("JsonException"));
        when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(new ChunkResultBuilder().build());

        final Response response = getJobsBean().getProcessorResult(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getProcessorResult_jobStoreReturnsResult_returnsStatusOkResponseWithEntity() throws JobStoreException, JsonException {
        final ChunkResult processorResult = new ChunkResultBuilder().build();
        when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(processorResult);

        final Response response = getJobsBean().getProcessorResult(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("jobId").longValue(), is(processorResult.getJobId()));
    }

    @Test
    public void getSinkResult_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(null);

        final Response response = getJobsBean().getSinkResult(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getSinkResult_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        when(jobStore.getSinkResult(jobId, chunkId)).thenThrow(new JobStoreException("JobStoreException"));

        getJobsBean().getSinkResult(jobId, chunkId);
    }

    @Test(expected = JobStoreException.class)
    public void getSinkResult_chunkCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(Chunk.class))).thenThrow(new JsonException("JsonException"));
        when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(new SinkChunkResultBuilder().build());

        final Response response = getJobsBean().getSinkResult(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getSinkResult_jobStoreReturnsResult_returnsStatusOkResponseWithEntity() throws JobStoreException, JsonException {
        final SinkChunkResult sinkResult = new SinkChunkResultBuilder().build();
        when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(sinkResult);

        final Response response = getJobsBean().getSinkResult(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("jobId").longValue(), is(sinkResult.getJobId()));
    }

    @Test
    public void getJobs_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        when(jobStore.getAllJobInfos()).thenReturn(null);

        final Response response = getJobsBean().getJobs();
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getJobs_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        when(jobStore.getAllJobInfos()).thenThrow(new JobStoreException("JobStoreException"));

        getJobsBean().getJobs();
    }

    @Test(expected = JobStoreException.class)
    public void getJobs_jobListCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(Chunk.class))).thenThrow(new JsonException("JsonException"));
        final List<JobInfo> JobInfoList = new ArrayList<>();
        JobInfoList.add(new JobInfoBuilder().build());
        when(jobStore.getAllJobInfos()).thenReturn(JobInfoList);

        final Response response = getJobsBean().getJobs();
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getJobs_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JobStoreException, JsonException {
        final List<JobInfo> jobInfoList = new ArrayList<>();
        when(jobStore.getAllJobInfos()).thenReturn(jobInfoList);

        final Response response = getJobsBean().getJobs();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.isArray(), is(true));
        assertThat(entityNode.elements().hasNext(), is(false));
    }

    @Test
    public void getJobs_jobStoreReturnsJobList_returnsStatusOkResponseWithJobList() throws JobStoreException, JsonException {
        final List<JobInfo> jobInfoList = new ArrayList<>();
        jobInfoList.add(new JobInfoBuilder().build());
        when(jobStore.getAllJobInfos()).thenReturn(jobInfoList);

        final Response response = getJobsBean().getJobs();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.isArray(), is(true));
        assertThat(entityNode.elements().hasNext(), is(true));
        assertThat(entityNode.elements().next().get("jobId").longValue(), is(jobInfoList.get(0).getJobId()));
    }

    private JobsBean getJobsBean() {
        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStoreBean = jobStoreBean;
        return jobsBean;
    }

    private String getValidJobSpecificationString() {
        final String packaging = "xml";
        final String format = "nmalbum";
        final String charset = "utf8";
        final String destination = "idontknow";
        final Long submitterNumber = 123456L;

        return new JobSpecificationJsonBuilder()
                .setPackaging(packaging)
                .setFormat(format)
                .setCharset(charset)
                .setSubmitterId(submitterNumber)
                .setDestination(destination)
                .build();
    }
}
