package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobsBeanTest {
    private final static String LOCATION = "helloWorld";
    private final static int PART_NUMBER = 2535678;
    private final static int JOB_ID = 42;
    private final static int CHUNK_ID = 10;
    private UriInfo mockedUriInfo;
    private UriBuilder mockedUriBuilder;
    private JobsBean jobsBean;
    private JSONBContext jsonbContext;


    @Before
    public void setup() {
        initializeJobsBean();
        jsonbContext = new JSONBContext();

        mockedUriInfo = mock(UriInfo.class);
        mockedUriBuilder = mock(UriBuilder.class);
        jobsBean.jobStoreBean = mock(JobStoreBean.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
    }

    // ************************************* ADD JOB TESTS **************************************************************

    @Test(expected = JobStoreException.class)
    public void addJob_addAndScheduleJobFailure_throwsJobStoreException() throws Exception{
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = jsonbContext.marshall(jobInputStream);

        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenThrow(new JobStoreException("Error"));

        jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
    }

    @Test
    public void addJob_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception{
        final String jobInputStreamJsonString = jsonbContext.marshall("invalid JSON");
        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJsonString);

        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, not(nullValue()));
        assertThat(jobErrorReturned.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void addJob_invalidInput_throwsInvalidInputException() throws Exception{
        JobError jobError = new JobError(JobError.Code.INVALID_DATAFILE, "datafile is invalid", "stack trace");
        InvalidInputException invalidInputException = new InvalidInputException("error message", jobError);
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = jsonbContext.marshall(jobInputStream);

        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenThrow(invalidInputException);

        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, not(nullValue()));
        assertThat(jobErrorReturned.getCode(), is(jobError.getCode()));
    }

    @Test
    public void addJob_returnsResponseWithHttpStatusCreated_returnsJobInfoSnapshot() throws Exception {
        final URI uri = new URI(LOCATION);
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = jsonbContext.marshall(jobInputStream);
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot();

        when(mockedUriBuilder.build()).thenReturn(uri);
        when(jobsBean.jobStoreBean.addAndScheduleJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);

        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJson);

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, not(nullValue()));
        assertThat(returnedJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
        assertJobSpecificationEquals(returnedJobInfoSnapshot.getSpecification(), jobSpecification);
    }

    // ************************************* ADD CHUNK TESTS **************************************************************

    @Test
    public void addChunk_jobIsUpdated_jobInfoSnapShotReturned() throws Exception {
        final URI uri = new URI(LOCATION);
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot();
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PROCESSED);

        when(mockedUriBuilder.build()).thenReturn(uri);
        when(jobsBean.jobStoreBean.addChunk(any(ExternalChunk.class))).thenReturn(jobInfoSnapshot);

        final Response response = jobsBean.addChunkProcessed(
                mockedUriInfo,
                jsonbContext.marshall(chunk), chunk.getJobId(), chunk.getChunkId());

        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, not(nullValue()));
        assertThat(Long.valueOf(returnedJobInfoSnapshot.getJobId()).longValue(), is(chunk.getJobId()));
    }

    @Test
    public void addChunk_invalidJobId_throwsInvalidInputException() throws Exception {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PROCESSED);
        final Response response = jobsBean.addChunkDelivered(
                mockedUriInfo,
                jsonbContext.marshall(chunk), chunk.getJobId() + 1, chunk.getChunkId());

        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, not(nullValue()));
    }

    @Test
    public void addChunk_invalidChunkId_throwsInvalidInputException() throws Exception {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PROCESSED);
        final Response response = jobsBean.addChunkProcessed(
                mockedUriInfo,
                jsonbContext.marshall(chunk), chunk.getJobId(), chunk.getChunkId() + 1);

        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, not(nullValue()));
    }

    @Test
    public void addChunk_invalidChunkType_throwsInvalidInputException() throws Exception {
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PROCESSED);
        final Response response = jobsBean.addChunkDelivered(
                mockedUriInfo,
                jsonbContext.marshall(chunk), chunk.getJobId(), chunk.getChunkId());

        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, not(nullValue()));
    }

    @Test
    public void addChunk_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception{
        final String externalChunkJsonString = jsonbContext.marshall("invalid JSON");
        final Response response = jobsBean.addChunkDelivered(mockedUriInfo, externalChunkJsonString, JOB_ID, CHUNK_ID);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, not(nullValue()));
        assertThat(jobErrorReturned.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void addChunk_invalidInput_throwsInvalidInputException() throws Exception {
        final JobError jobError = new JobError(JobError.Code.ILLEGAL_CHUNK, "illegal number of items", "stack trace");
        final InvalidInputException invalidInputException = new InvalidInputException("error message", jobError);
        final ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.PROCESSED);
        final String externalChunkJson = jsonbContext.marshall(chunk);

        when(jobsBean.jobStoreBean.addChunk(any(ExternalChunk.class))).thenThrow(invalidInputException);

        final Response response = jobsBean.addChunkProcessed(mockedUriInfo, externalChunkJson, JOB_ID, CHUNK_ID);

        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, not(nullValue()));
        assertThat(jobErrorReturned.getCode(), is(jobError.getCode()));
    }

    @Test(expected = JobStoreException.class)
    public void addChunk_referencedEntityNotFound_throwsJobStoreException() throws Exception{
        ExternalChunk chunk = getExternalChunk(ExternalChunk.Type.DELIVERED);
        when(jobsBean.jobStoreBean.addChunk(any(ExternalChunk.class))).thenThrow(new JobStoreException("Error"));

        jobsBean.addChunkDelivered(mockedUriInfo, jsonbContext.marshall(chunk), chunk.getJobId(), chunk.getChunkId());
    }


    /*
     Private methods
    */

    private void assertJobSpecificationEquals(JobSpecification jobSpecification1, JobSpecification jobSpecification2) {
        assertThat(jobSpecification1.getFormat(), is(jobSpecification2.getFormat()));
        assertThat(jobSpecification1.getCharset(), is(jobSpecification2.getCharset()));
        assertThat(jobSpecification1.getDataFile(), is(jobSpecification2.getDataFile()));
        assertThat(jobSpecification1.getDestination(), is(jobSpecification2.getDestination()));
        assertThat(jobSpecification1.getMailForNotificationAboutProcessing(), is(jobSpecification2.getMailForNotificationAboutProcessing()));
        assertThat(jobSpecification1.getMailForNotificationAboutVerification(), is(jobSpecification2.getMailForNotificationAboutVerification()));
        assertThat(jobSpecification1.getPackaging(), is(jobSpecification2.getPackaging()));
        assertThat(jobSpecification1.getResultmailInitials(), is(jobSpecification2.getResultmailInitials()));
        assertThat(jobSpecification1.getSubmitterId(), is(jobSpecification2.getSubmitterId()));
    }

    private void initializeJobsBean() {
        jobsBean = new JobsBean();
        jobsBean.jsonbBean = new JSONBBean();
        jobsBean.jsonbBean.initialiseContext();
    }

    private JobInfoSnapshot getJobInfoSnapshot() {
        return new JobInfoSnapshot(
                JOB_ID,
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

    private ExternalChunk getExternalChunk(ExternalChunk.Type type) {
        return new ExternalChunk(JOB_ID, CHUNK_ID, type);
    }

}
