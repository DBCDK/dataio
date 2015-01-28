package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobStoreBeanTest {
    private static final String FILE_STORE_URN_STRING = "urn:dataio-fs:67";

    private static FlowStoreServiceConnectorException flowStoreException;
    private static FlowStoreServiceConnectorUnexpectedStatusCodeException flowBinderNotFound;
    private static FlowStoreServiceConnectorUnexpectedStatusCodeException flowStoreUnexpectedException;
    private static FileStoreServiceConnectorException fileStoreException;
    private static FileStoreServiceConnectorUnexpectedStatusCodeException fileStoreUnexpectedException;
    private static FileStoreServiceConnectorUnexpectedStatusCodeException jobDataFileNotFound;

    private JobStoreBean jobStoreBean;
    private FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean;
    private FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean;
    private PgJobStore mockedJobStore;

    @BeforeClass
    public static void setupExceptions() {
        flowStoreException = new FlowStoreServiceConnectorException("internal server error");
        flowBinderNotFound = new FlowStoreServiceConnectorUnexpectedStatusCodeException("not found", 404);
        flowStoreUnexpectedException = new FlowStoreServiceConnectorUnexpectedStatusCodeException("unexpected status code", 400);
        fileStoreException = new FileStoreServiceConnectorException("internal server error");
        fileStoreUnexpectedException = new FileStoreServiceConnectorUnexpectedStatusCodeException("unexpected status code", 400);
        jobDataFileNotFound = new FileStoreServiceConnectorUnexpectedStatusCodeException("not found", 404);
    }

    @Before
    public void setup() {
        mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
        mockedJobStore = mock(PgJobStore.class);
        initializeBean();
    }

    @Test(expected = NullPointerException.class)
    public void addAndScheduleJob_nullArgument_throws() throws Exception {
        jobStoreBean.addAndScheduleJob(null);
        fail("No NullPointerException Thrown");
    }

    @Test
    public void addAndScheduleJob_getFlowBinder_flowStoreServiceConnectorUnexpectedStatusCodeException() throws FlowStoreServiceConnectorException {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        whenGetFlowBinderThenThrow(jobInputStream.getJobSpecification(), flowStoreUnexpectedException);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch (JobStoreException e) {
            assertThat(e instanceof InvalidInputException, is(false));
            assertThat(e.getCause() instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException, is(true));
            assertThat(((FlowStoreServiceConnectorUnexpectedStatusCodeException) e.getCause()).getStatusCode(), is(flowStoreUnexpectedException.getStatusCode()));
        }
    }

    @Test
    public void addAndScheduleJob_getFlowBinder_flowStoreServiceConnectorException() throws FlowStoreServiceConnectorException {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        whenGetFlowBinderThenThrow(jobInputStream.getJobSpecification(), flowStoreException);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch (JobStoreException e) {
            assertThat(e instanceof InvalidInputException, is(false));
            assertThat(e.getCause() instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test
    public void addAndScheduleJob_getFlowBinder_flowBinderNotFound() throws FlowStoreServiceConnectorException {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        whenGetFlowBinderThenThrow(jobInputStream.getJobSpecification(), flowBinderNotFound);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch (JobStoreException e) {
            JobError jobError = ((InvalidInputException) e).getJobError();
            assertThat(jobError, is(notNullValue()));
            assertThat(jobError.getCode(), is(JobError.Code.INVALID_FLOW_BINDER_IDENTIFIER));

        }
    }

    @Test(expected = JobStoreException.class)
    public void addAndScheduleJob_flowNotFound_throws() throws FlowStoreServiceConnectorException, JobStoreException{
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final FlowBinder flowBinder = new FlowBinderBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobInputStream.getJobSpecification(), flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getContent().getFlowId())).thenThrow(flowStoreException);

        jobStoreBean.addAndScheduleJob(jobInputStream);
        fail("No exception thrown by addAndScheduleJob()");
    }

    @Test
    public void addAndScheduleJob_getFile_throwsURISyntaxException() throws FlowStoreServiceConnectorException {
        final JobInputStream jobInputStream = getJobInputStream("invalid");
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobInputStream.getJobSpecification(), flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getContent().getFlowId())).thenReturn(flow);
        when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getContent().getSinkId())).thenReturn(sink);

        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {
            JobError jobError = ((InvalidInputException) e).getJobError();
            assertThat(jobError, is(notNullValue()));
            assertThat(jobError.getCode(), is(JobError.Code.INVALID_URI_SYNTAX));
        }
    }

    @Test
    public void addAndScheduleJob_jobAdded_returnsJobInfoSnapshot() throws Exception {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder()
                .setSpecification(jobInputStream.getJobSpecification())
                .build();

        final String xml = getXml();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        whenGetFlowBinderThenReturnFlowBinder(jobInputStream.getJobSpecification(), flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getContent().getFlowId())).thenReturn(flow);
        when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getContent().getSinkId())).thenReturn(sink);
        when(mockedFileStoreServiceConnectorBean.getFile(anyString())).thenReturn(byteArrayInputStream);
        when(mockedJobStore.addJob(
                any(JobInputStream.class),
                any(DataPartitionerFactory.DataPartitioner.class),
                any(SequenceAnalyserKeyGenerator.class),
                any(Flow.class),
                any(Sink.class))).thenReturn(jobInfoSnapshot);

        try {
            final JobInfoSnapshot jobInfoSnapshotReturned = jobStoreBean.addAndScheduleJob(jobInputStream);
            assertThat(jobInfoSnapshotReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by addAndScheduleJob()");
        }
    }

    @Test
    public void addAndScheduleJob_getInputStream_fileStoreServiceConnectorUnexpectedStatusCodeException() throws Exception {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobInputStream.getJobSpecification(), flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getContent().getFlowId())).thenReturn(flow);
        when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getContent().getSinkId())).thenReturn(sink);
        when(mockedFileStoreServiceConnectorBean.getFile(anyString())).thenThrow(fileStoreUnexpectedException);

        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {
            assertThat(e instanceof InvalidInputException, is(false));
            assertThat(e.getCause() instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(true));
            assertThat(((FileStoreServiceConnectorUnexpectedStatusCodeException) e.getCause()).getStatusCode(), is(fileStoreUnexpectedException.getStatusCode()));
        }
    }

    @Test
    public void addAndScheduleJob_getInputStream_inputStreamNotFound() throws Exception {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobInputStream.getJobSpecification(), flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getContent().getFlowId())).thenReturn(flow);
        when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getContent().getSinkId())).thenReturn(sink);
        when(mockedFileStoreServiceConnectorBean.getFile(anyString())).thenThrow(jobDataFileNotFound);


        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {
            JobError jobError = ((InvalidInputException) e).getJobError();
            assertThat(jobError, is(notNullValue()));
            assertThat(jobError.getCode(), is(JobError.Code.INVALID_DATAFILE));
        }
    }

    @Test
    public void addAndScheduleJob_getInputStream_fileStoreServiceConnectorException() throws Exception {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobInputStream.getJobSpecification(), flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getContent().getFlowId())).thenReturn(flow);
        when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getContent().getSinkId())).thenReturn(sink);
        when(mockedFileStoreServiceConnectorBean.getFile(anyString())).thenThrow(fileStoreException);

        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {
            assertThat(e instanceof InvalidInputException, is(false));
            assertThat(e.getCause() instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test(expected = JobStoreException.class)
    public void addAndScheduleJob_sinkNotFound_throws() throws FlowStoreServiceConnectorException, JobStoreException {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final FlowBinder flowBinder = new FlowBinderBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobInputStream.getJobSpecification(), flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getContent().getSinkId())).thenThrow(flowStoreException);

        jobStoreBean.addAndScheduleJob(jobInputStream);
        fail("No exception thrown by addAndScheduleJob()");
    }

    @Test(expected = JobStoreException.class)
    public void addChunk_onFailureToUpdateJob_throwsJobStoreException() throws JobStoreException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();
        when(mockedJobStore.addChunk(chunk)).thenThrow(new JobStoreException("msg", null));
        jobStoreBean.addChunk(chunk);
        fail("No exception thrown by addChunk()");
    }

    @Test
    public void addChunk_chunkIsAdded_returnsJobInfoSnapShot() throws Exception {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build();
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder().setSpecification(jobSpecification).build();

        when(mockedJobStore.addChunk(chunk)).thenReturn(jobInfoSnapshot);
        try {
            JobInfoSnapshot jobInfoSnapshotReturned = jobStoreBean.addChunk(chunk);
            assertThat(jobInfoSnapshotReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by addChunk()");
        }
    }

    @Test(expected = JobStoreException.class)
    public void getResourceBundle_onFailureToFindJobEntity_throwsJobStoreException() throws JobStoreException {
        JobError jobError = new JobError(JobError.Code.INVALID_ITEM_IDENTIFIER, "msg", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);
        when(mockedJobStore.getResourceBundle(0)).thenThrow(invalidInputException);
        jobStoreBean.getResourceBundle(0);
        fail("No exception thrown by getResourceBundle()");
    }

    @Test
    public void getResourceBundle_bundleIsCreated_returnsResourceBundle() throws Exception {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        ResourceBundle resourceBundle = new ResourceBundle(
                new FlowBuilder().build(),
                new SinkBuilder().build(),
                new SupplementaryProcessData(jobSpecification.getSubmitterId(), jobSpecification.getFormat()));

        when(mockedJobStore.getResourceBundle(0)).thenReturn(resourceBundle);
        try {
            ResourceBundle resourceBundleReturned = jobStoreBean.getResourceBundle(0);
            assertThat(resourceBundleReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by getResourceBundle()");
        }
    }

    @Test
    public void listJobs_delegatesToUnderlyingImplementation() {
        final JobListCriteria jobListCriteria = new JobListCriteria();
        jobStoreBean.listJobs(jobListCriteria);
        verify(mockedJobStore).listJobs(jobListCriteria);
    }

    /*
     * Private methods
     */

    private void initializeBean(){
        jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbBean = new JSONBBean();
        jobStoreBean.jobStore = mockedJobStore;
        jobStoreBean.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;
        jobStoreBean.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
        jobStoreBean.jsonbBean.initialiseContext();
    }

    private JobInputStream getJobInputStream(String datafile) {
        JobSpecification jobSpecification = new JobSpecificationBuilder().setDataFile(datafile).build();
        return new JobInputStream(jobSpecification, false, 3);
    }

    private void whenGetFlowBinderThenThrow(JobSpecification jobSpecification, Exception e) throws FlowStoreServiceConnectorException{
        when(mockedFlowStoreServiceConnectorBean.getFlowBinder(
                jobSpecification.getPackaging(),
                jobSpecification.getFormat(),
                jobSpecification.getCharset(),
                jobSpecification.getSubmitterId(),
                jobSpecification.getDestination())).thenThrow(e);
    }

    private void whenGetFlowBinderThenReturnFlowBinder(JobSpecification jobSpecification, FlowBinder flowBinder) throws FlowStoreServiceConnectorException {
        when(mockedFlowStoreServiceConnectorBean.getFlowBinder(
                jobSpecification.getPackaging(),
                jobSpecification.getFormat(),
                jobSpecification.getCharset(),
                jobSpecification.getSubmitterId(),
                jobSpecification.getDestination())).thenReturn(flowBinder);
    }

    private String getXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<records>"
                + "<record>first</record>"
                + "<record>second</record>"
                + "<record>third</record>"
                + "<record>fourth</record>"
                + "<record>fifth</record>"
                + "<record>sixth</record>"
                + "<record>seventh</record>"
                + "<record>eighth</record>"
                + "<record>ninth</record>"
                + "<record>tenth</record>"
                + "<record>eleventh</record>"
                + "</records>";
    }
}
