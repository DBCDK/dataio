package dk.dbc.dataio.jobstore.service.ejb;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SupplementaryProcessDataBuilder;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobsBeanTest {
    private final static String LOCATION = "location";
    private final static int PART_NUMBER = 2535678;
    private final static int JOB_ID = 42;
    private final static int CHUNK_ID = 10;
    private UriInfo mockedUriInfo;
    private JobsBean jobsBean;
    private JSONBContext jsonbContext;

    @Before
    public void setup() throws URISyntaxException {
        initializeJobsBean();
        jsonbContext = new JSONBContext();

        mockedUriInfo = mock(UriInfo.class);
        final UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);

        final URI uri = new URI(LOCATION);
        when(mockedUriBuilder.build()).thenReturn(uri);
    }

    // ************************************* ADD JOB TESTS **************************************************************

    @Test(expected = JobStoreException.class)
    public void addJob_addAndScheduleJobFailure_throwsJobStoreException() throws Exception {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenThrow(new JobStoreException("Error"));

        jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
    }

    @Test
    public void addJob_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final Response response = jobsBean.addJob(mockedUriInfo, "invalid JSON");
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
        assertThat(jobErrorReturned.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void addJob_invalidInput_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final JobError jobError = new JobError(JobError.Code.INVALID_DATAFILE, "datafile is invalid", "stack trace");
        final InvalidInputException invalidInputException = new InvalidInputException("error message", jobError);
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenThrow(invalidInputException);

        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
        assertThat(jobErrorReturned.getCode(), is(jobError.getCode()));
    }

    @Test
    public void addJob_returnsResponseWithHttpStatusCreated_returnsJobInfoSnapshot() throws Exception {
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder().setJobId(JOB_ID).build();
        final JobInputStream jobInputStream = new JobInputStream(jobInfoSnapshot.getSpecification(), false, PART_NUMBER);
        final String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);

        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        final JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
        assertThat(returnedJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat(returnedJobInfoSnapshot.getSpecification(), is(jobInfoSnapshot.getSpecification()));
        assertThat(returnedJobInfoSnapshot.getState(), is(jobInfoSnapshot.getState()));
        assertThat(returnedJobInfoSnapshot.getFlowStoreReferences(), is(jobInfoSnapshot.getFlowStoreReferences()));
    }

    // ************************************* ADD CHUNK TESTS **************************************************************

    @Test
    public void addChunk_jobIsUpdated_jobInfoSnapShotReturned() throws Exception {
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder().setJobId(JOB_ID).build();
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        when(jobsBean.jobStoreBean.addChunk(any(ExternalChunk.class))).thenReturn(jobInfoSnapshot);

        final Response response = jobsBean.addChunkProcessed(
                mockedUriInfo, asJson(chunk), chunk.getJobId(), chunk.getChunkId());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        final JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
        assertThat((long) returnedJobInfoSnapshot.getJobId(), is(chunk.getJobId()));
    }

    @Test
    public void addChunk_invalidJobId_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        final Response response = jobsBean.addChunkDelivered(
                mockedUriInfo, asJson(chunk), chunk.getJobId() + 1, chunk.getChunkId());
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
    }

    @Test
    public void addChunk_invalidChunkId_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        final Response response = jobsBean.addChunkProcessed(
                mockedUriInfo, asJson(chunk), chunk.getJobId(), chunk.getChunkId() + 1);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
    }

    @Test
    public void addChunk_invalidChunkType_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        final Response response = jobsBean.addChunkDelivered(
                mockedUriInfo, asJson(chunk), chunk.getJobId(), chunk.getChunkId());
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
    }

    @Test
    public void addChunk_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final Response response = jobsBean.addChunkDelivered(mockedUriInfo, "invalid JSON", JOB_ID, CHUNK_ID);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
        assertThat(jobErrorReturned.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void addChunk_invalidInput__returnsResponseWithHttpStatusBadRequest() throws Exception {
        final JobError jobError = new JobError(JobError.Code.ILLEGAL_CHUNK, "illegal number of items", "stack trace");
        final InvalidInputException invalidInputException = new InvalidInputException("error message", jobError);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();
        final String externalChunkJson = asJson(chunk);

        when(jobsBean.jobStoreBean.addChunk(any(ExternalChunk.class))).thenThrow(invalidInputException);

        final Response response = jobsBean.addChunkProcessed(mockedUriInfo, externalChunkJson, JOB_ID, CHUNK_ID);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
        assertThat(jobErrorReturned.getCode(), is(jobError.getCode()));
    }

    @Test(expected = JobStoreException.class)
    public void addChunk_onFailureToUpdateJob_throwsJobStoreException() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();
        when(jobsBean.jobStoreBean.addChunk(any(ExternalChunk.class))).thenThrow(new JobStoreException("Error"));

        jobsBean.addChunkDelivered(mockedUriInfo, jsonbContext.marshall(chunk), chunk.getJobId(), chunk.getChunkId());
    }

    // ************************************* listJobs() tests **********************************************************

    @Test
    public void listJobs_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JSONBException {
        when(jobsBean.jobStoreBean.listJobs(any(JobListCriteria.class))).thenReturn(Collections.<JobInfoSnapshot>emptyList());

        final Response response = jobsBean.listJobs(asJson(new JobListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType jobInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, JobInfoSnapshot.class);

        List<JobInfoSnapshot> jobInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), jobInfoSnapshotListType);
        assertThat("JobInfoSnapshots", jobInfoSnapshots, is(notNullValue()));
        assertThat("JobInfoSnapshots is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listJobs_unableToUnmarshallJobListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        final Response response = jobsBean.listJobs("Invalid JSON");
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final JobError jobError = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void listJobs_jobStoreReturnsList_returnsStatusOkResponseWithJobInfoSnapshotList() throws JSONBException {
        final List<JobInfoSnapshot> expectedJobInfoSnapshots = new ArrayList<>();
        expectedJobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());
        when(jobsBean.jobStoreBean.listJobs(any(JobListCriteria.class))).thenReturn(expectedJobInfoSnapshots);

        final Response response = jobsBean.listJobs(asJson(new JobListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType jobInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, JobInfoSnapshot.class);

        List<JobInfoSnapshot> jobInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), jobInfoSnapshotListType);
        assertThat("JobInfoSnapshots", jobInfoSnapshots, is(notNullValue()));
        assertThat("JobInfoSnapshots size", jobInfoSnapshots.size(), is(expectedJobInfoSnapshots.size()));
        assertThat("JobInfoSnapshots element", jobInfoSnapshots.get(0).getJobId(), is(expectedJobInfoSnapshots.get(0).getJobId()));
    }

    // ************************************* listItems() tests **********************************************************

    @Test
    public void listItems_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JSONBException {
        when(jobsBean.jobStoreBean.listItems(any(ItemListCriteria.class))).thenReturn(Collections.<ItemInfoSnapshot>emptyList());

        final Response response = jobsBean.listItems(asJson(new ItemListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType itemInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, ItemInfoSnapshot.class);

        List<ItemInfoSnapshot> itemInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), itemInfoSnapshotListType);
        assertThat("ItemInfoSnapshots", itemInfoSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots is empty", itemInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listItems_unableToUnmarshallItemListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        final Response response = jobsBean.listJobs("Invalid JSON");
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final JobError jobError = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void listItems_jobStoreReturnsList_returnsStatusOkResponseWithItemInfoSnapshotList() throws JSONBException {
        final List<ItemInfoSnapshot> expectedItemInfoSnapshots = new ArrayList<>();
        expectedItemInfoSnapshots.add(new ItemInfoSnapshotBuilder().build());
        when(jobsBean.jobStoreBean.listItems(any(ItemListCriteria.class))).thenReturn(expectedItemInfoSnapshots);

        final Response response = jobsBean.listItems(asJson(new JobListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType itemInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, ItemInfoSnapshot.class);

        List<ItemInfoSnapshot> itemInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), itemInfoSnapshotListType);
        assertThat("ItemInfoSnapshots", itemInfoSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots size", itemInfoSnapshots.size(), is(expectedItemInfoSnapshots.size()));
        assertThat("ItemInfoSnapshots element", itemInfoSnapshots.get(0).getItemId(), is(expectedItemInfoSnapshots.get(0).getItemId()));
    }

    // ************************************* getResourceBundle() tests ***********************************************************

    @Test
    public void getResourceBundle_resourcesLocated_returnsStatusOkResponseWithResourceBundle() throws JSONBException, JobStoreException {
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();
        SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessDataBuilder().build();
        ResourceBundle resourceBundle = new ResourceBundle(flow, sink, supplementaryProcessData);

        when(jobsBean.jobStoreBean.getResourceBundle(anyInt())).thenReturn(resourceBundle);

        final Response response = jobsBean.getResourceBundle(JOB_ID);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        final ResourceBundle resourceBundleReturned = jsonbContext.unmarshall((String) response.getEntity(), ResourceBundle.class);
        assertThat("ResourceBundle not null", resourceBundleReturned, not(nullValue()));
    }


    @Test
    public void getResourceBundle_jobEntityNotFound_returnsStatusBadRequestResponseWithJobError() throws Exception {
        JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "job not found", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);

        when(jobsBean.jobStoreBean.getResourceBundle(anyInt())).thenThrow(invalidInputException);

        final Response response = jobsBean.getResourceBundle(JOB_ID);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError not null", jobErrorReturned, is(notNullValue()));
    }

    /*
     Private methods
    */


    private void initializeJobsBean() {
        jobsBean = new JobsBean();
        jobsBean.jobStoreBean = mock(JobStoreBean.class);
    }

    private String asJson(Object object) throws JSONBException {
        return jsonbContext.marshall(object);
    }

}
