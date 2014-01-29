package dk.dbc.dataio.jobstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobState;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private final String flowStoreUrl = "http://dataio/flow-store";
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final Client client = mock(Client.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getFlowStoreServiceEndpoint())
                .thenReturn(flowStoreUrl);
        when(HttpClient.newClient()).thenReturn(client);
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
        when(HttpClient.doGet(any(Client.class), any(Map.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOW_BINDERS), eq(JobsBean.REST_FLOWBINDER_QUERY_ENTRY_POINT)))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), ""));
        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, jobSpecData);
    }

    @Ignore
    @Test(expected = EJBException.class)
    public void createJob_jobStoreThrowsJobStoreException_throwsEJBException() throws Exception {
        final long flowId = 42L;
        final long flowBinderId = 31L;
        final String flowData = new FlowJsonBuilder().setId(flowId).build();
        final String flowBinderData = new FlowBinderJsonBuilder().setId(flowBinderId).build();
        final String jobSpecData = getValidJobSpecificationString();

        when(HttpClient.doGet(any(Client.class), any(Map.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOW_BINDERS), eq(JobsBean.REST_FLOWBINDER_QUERY_ENTRY_POINT)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowBinderData));
        when(HttpClient.doGet(any(Client.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOWS), eq(Long.toString(flowId))))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowData));

        final JobsBean jobsBean = new JobsBean();
        jobsBean.createJob(uriInfo, jobSpecData);
    }

    @Ignore
    @Test
    public void createJob_jobIsCreated_returnsStatusCreatedResponse() throws Exception {
        final long flowId = 42L;
        final long flowBinderId = 31L;
        final String flowData = new FlowJsonBuilder().setId(flowId).build();
        final String flowBinderData = new FlowBinderJsonBuilder().setId(flowBinderId).build();
        final String jobSpecData = getValidJobSpecificationString();
        final String jobInfoData = new JobInfoJsonBuilder().build();
        final Job job = new Job(JsonUtil.fromJson(jobInfoData, JobInfo.class, MixIns.getMixIns()), new JobState(),
                JsonUtil.fromJson(flowData, Flow.class, MixIns.getMixIns()));

        when(HttpClient.doGet(any(Client.class), any(Map.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOW_BINDERS), eq(JobsBean.REST_FLOWBINDER_QUERY_ENTRY_POINT)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowBinderData));
        when(HttpClient.doGet(any(Client.class), eq(flowStoreUrl), eq(FlowStoreServiceEntryPoint.FLOWS), eq(Long.toString(flowId))))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), flowData));

        final JobsBean jobsBean = new JobsBean();
        final Response response = jobsBean.createJob(uriInfo, jobSpecData);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
    }

    @Test
    public void getChunk_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        final long jobId = 42;
        final long chunkId = 1;
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        when(jobStore.getChunk(jobId, chunkId)).thenReturn(null);

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.getChunk(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getChunk_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        final long jobId = 42;
        final long chunkId = 1;
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        when(jobStore.getChunk(jobId, chunkId)).thenThrow(new JobStoreException("JobStoreException"));

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        jobsBean.getChunk(jobId, chunkId);
    }

    @Test(expected = JobStoreException.class)
    public void getChunk_chunkCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        final long jobId = 42;
        final long chunkId = 1;
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(Chunk.class))).thenThrow(new JsonException("JsonException"));
        when(jobStore.getChunk(jobId, chunkId)).thenReturn(new ChunkBuilder().build());

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.getChunk(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getChunk_jobStoreReturnsChunk_returnsStatusOkResponseWithChunkEntity() throws JobStoreException, JsonException {
        final long jobId = 42;
        final long chunkId = 1;
        final Chunk chunk = new ChunkBuilder().build();
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunk);

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.getChunk(jobId, chunkId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.get("id").longValue(), is(chunk.getId()));
    }

    @Test
    public void getJobs_jobStoreReturnsNull_returnsStatusNotFoundResponse() throws JobStoreException {
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        when(jobStore.getAllJobInfos()).thenReturn(null);

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.getJobs();
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test(expected = JobStoreException.class)
    public void getJobs_jobStoreThrowsJobStoreException_throws() throws JobStoreException {
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        when(jobStore.getAllJobInfos()).thenThrow(new JobStoreException("JobStoreException"));

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        jobsBean.getJobs();
    }

    @Test(expected = JobStoreException.class)
    public void getJobs_jobListCanNotBeMarshalledToJson_throws() throws JobStoreException, JsonException {
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(Chunk.class))).thenThrow(new JsonException("JsonException"));
        final List<JobInfo> JobInfoList = new ArrayList<>();
        JobInfoList.add(new JobInfoBuilder().build());
        when(jobStore.getAllJobInfos()).thenReturn(JobInfoList);

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.getJobs();
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void getJobs_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JobStoreException, JsonException {
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        final List<JobInfo> jobInfoList = new ArrayList<>();
        when(jobStore.getAllJobInfos()).thenReturn(jobInfoList);

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.getJobs();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.isArray(), is(true));
        assertThat(entityNode.elements().hasNext(), is(false));
    }

    @Test
    public void getJobs_jobStoreReturnsJobList_returnsStatusOkResponseWithJobList() throws JobStoreException, JsonException {
        final JobStoreBean jobStore = mock(JobStoreBean.class);
        final List<JobInfo> jobInfoList = new ArrayList<>();
        jobInfoList.add(new JobInfoBuilder().build());
        when(jobStore.getAllJobInfos()).thenReturn(jobInfoList);

        final JobsBean jobsBean = new JobsBean();
        jobsBean.jobStore = jobStore;
        final Response response = jobsBean.getJobs();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        final JsonNode entityNode = JsonUtil.getJsonRoot((String)response.getEntity());
        assertThat(entityNode.isArray(), is(true));
        assertThat(entityNode.elements().hasNext(), is(true));
        assertThat(entityNode.elements().next().get("jobId").longValue(), is(jobInfoList.get(0).getJobId()));
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
