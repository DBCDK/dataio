package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
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
    private static final int JOB_ID = 1;
    private static final int CHUNK_ID = 0;
    private static final short ITEM_ID = 0;

    private static FileStoreServiceConnectorUnexpectedStatusCodeException fileStoreUnexpectedException;

    private JobStoreBean jobStoreBean;
    private FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean;
    private FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean;
    private FlowStoreServiceConnector mockedFlowStoreServiceConnector;
    private FileStoreServiceConnector mockedFileStoreServiceConnector;
    private PgJobStore mockedJobStore;


    @BeforeClass
    public static void setupExceptions() {
        fileStoreUnexpectedException = new FileStoreServiceConnectorUnexpectedStatusCodeException("unexpected status code", 400);
    }

    @Before
    public void setup() {
        mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
        mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
        mockedJobStore = mock(PgJobStore.class);
        initializeBean();
    }

    @Test
    public void addAndScheduleJob_nullArgument_throws() throws Exception {
        try {
            jobStoreBean.addAndScheduleJob(null);
            fail("No NullPointerException Thrown");
        } catch(NullPointerException e) {}
    }

    @Test
    public void addAndScheduleJob_differentByteSize_throwsJobStoreException() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException, JobStoreException {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        setupSuccessfulMockedReturnsFromFlowStore(jobInputStream.getJobSpecification());
        setupSuccessfulMockedReturnsFromJobStore(jobInputStream.getJobSpecification());
        final String xml = getXml();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        when(mockedFileStoreServiceConnectorBean.getConnector().getFile(anyString())).thenReturn(byteArrayInputStream);
        when(mockedFileStoreServiceConnectorBean.getConnector().getByteSize(anyString())).thenReturn(42L);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {}
    }

    @Test
    public void addAndScheduleJob_byteSizeNotFound_throwsJobStoreException() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException, JobStoreException {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        setupSuccessfulMockedReturnsFromFlowStore(jobInputStream.getJobSpecification());
        setupSuccessfulMockedReturnsFromJobStore(jobInputStream.getJobSpecification());
        final String xml = getXml();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        when(mockedFileStoreServiceConnectorBean.getConnector().getByteSize(anyString())).thenThrow(fileStoreUnexpectedException);
        when(mockedFileStoreServiceConnectorBean.getConnector().getFile(anyString())).thenReturn(byteArrayInputStream);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {}
    }

    @Test
    public void addAndScheduleJob_compareByteSize_byteSizesIdentical() throws IOException, FileStoreServiceConnectorException, JobStoreException {
        DataPartitionerFactory.DataPartitioner mockedDataPartitioner = mock(DataPartitionerFactory.DataPartitioner.class);
        when(mockedFileStoreServiceConnectorBean.getConnector().getByteSize(anyString())).thenReturn((long) getXml().getBytes().length);
        when(mockedDataPartitioner.getBytesRead()).thenReturn((long) getXml().getBytes().length);
        jobStoreBean.compareByteSize("42", mockedDataPartitioner);
    }

    @Test
    public void addAndScheduleJob_compareByteSize_byteSizesNotIdenticalThrowsIOException() throws IOException, JobStoreException, FileStoreServiceConnectorException {
        DataPartitionerFactory.DataPartitioner mockedDataPartitioner = mock(DataPartitionerFactory.DataPartitioner.class);
        when(mockedFileStoreServiceConnectorBean.getConnector().getByteSize(anyString())).thenReturn((long) getXml().getBytes().length);
        when(mockedDataPartitioner.getBytesRead()).thenReturn((long) (getXml().getBytes().length - 1));
        try {
            jobStoreBean.compareByteSize("42", mockedDataPartitioner);
        } catch (IOException e) {}
    }

    @Test
    public void addAndScheduleJob_jobAdded_returnsJobInfoSnapshot() throws Exception {
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        setupSuccessfulMockedReturnsFromFlowStore(jobInputStream.getJobSpecification());
        setupSuccessfulMockedReturnsFromJobStore(jobInputStream.getJobSpecification());
        final String xml = getXml();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        when(mockedFileStoreServiceConnectorBean.getConnector().getFile(anyString())).thenReturn(byteArrayInputStream);
        try {
            final JobInfoSnapshot jobInfoSnapshotReturned = jobStoreBean.addAndScheduleJob(jobInputStream);
            // Verify that the method getByteSize (used in compareByteSize) was invoked.
            verify(mockedFileStoreServiceConnectorBean.getConnector()).getByteSize(anyString());
            assertThat(jobInfoSnapshotReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by addAndScheduleJob()");
        }
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

    @Test
    public void listItems_delegatesToUnderlyingImplementation() {
        final ItemListCriteria itemListCriteria = new ItemListCriteria();
        jobStoreBean.listItems(itemListCriteria);
        verify(mockedJobStore).listItems(itemListCriteria);
    }

    @Test
    public void getItemData_itemDataIsCreatedForPartitioningPhase_returnsItemData() throws Exception {
        ItemData itemData = new ItemData("data", Charset.defaultCharset());

        when(mockedJobStore.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING)).thenReturn(itemData);
        try {
            ItemData itemDataReturned = jobStoreBean.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING);
            assertThat(itemDataReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by getItemData()");
        }
    }

    @Test
    public void getItemData_itemDataIsCreatedForProcessingPhase_returnsItemData() throws Exception {
        ItemData itemData = new ItemData("data", Charset.defaultCharset());

        when(mockedJobStore.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PROCESSING)).thenReturn(itemData);
        try {
            ItemData itemDataReturned = jobStoreBean.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PROCESSING);
            assertThat(itemDataReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by getItemData()");
        }
    }

    @Test
    public void getItemData_itemDataIsCreatedForDeliveringPhase_returnsItemData() throws Exception {
        ItemData itemData = new ItemData("data", Charset.defaultCharset());

        when(mockedJobStore.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.DELIVERING)).thenReturn(itemData);
        try {
            ItemData itemDataReturned = jobStoreBean.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.DELIVERING);
            assertThat(itemDataReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by getItemData()");
        }
    }

    /*
     * Private methods
     */

    private void initializeBean(){
        jobStoreBean = new JobStoreBean();
        jobStoreBean.jobStore = mockedJobStore;
        jobStoreBean.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;
        jobStoreBean.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
        when(jobStoreBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(mockedFlowStoreServiceConnector);
        when(jobStoreBean.fileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);
    }

    private void setupSuccessfulMockedReturnsFromFlowStore(JobSpecification jobSpecification) throws FlowStoreServiceConnectorException{
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();
        final Submitter submitter = new SubmitterBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobSpecification, flowBinder);
        when(mockedFlowStoreServiceConnectorBean.getConnector().getFlow(flowBinder.getContent().getFlowId())).thenReturn(flow);
        when(mockedFlowStoreServiceConnectorBean.getConnector().getSink(flowBinder.getContent().getSinkId())).thenReturn(sink);
        when(mockedFlowStoreServiceConnectorBean.getConnector().getSubmitterBySubmitterNumber(jobSpecification.getSubmitterId())).thenReturn(submitter);
    }

    private void setupSuccessfulMockedReturnsFromJobStore(JobSpecification jobSpecification) throws JobStoreException {
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder()
                .setSpecification(jobSpecification)
                .build();

        when(mockedJobStore.addJob(
                any(AddJobParam.class))).thenReturn(jobInfoSnapshot);
    }

    private JobInputStream getJobInputStream(String datafile) {
        JobSpecification jobSpecification = new JobSpecificationBuilder().setDataFile(datafile).build();
        return new JobInputStream(jobSpecification, false, 3);
    }

    private void whenGetFlowBinderThenReturnFlowBinder(JobSpecification jobSpecification, FlowBinder flowBinder) throws FlowStoreServiceConnectorException {
        when(mockedFlowStoreServiceConnectorBean.getConnector().getFlowBinder(
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
