package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.DefaultXmlDataPartitioner;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Assertions;

import types.TestablePartitioningParamBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStore_HandlePartitioningTest extends PgJobStoreBaseTest {
    private PgJobStore pgJobStore;
    private JobEntity jobEntity;
    private TestablePartitioningParamBuilder partitioningParamBuilder;

    @org.junit.Before
    public void hazelcastSetup() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("hz-data.xml")) {
            Hazelcast.testInstance(createHazelcastInstance(Hazelcast.makeConfig(is)));
            JobsBean.testingUpdateStaticTestHazelcast();
        }
    }

    @org.junit.Before
    public void createPgJobStore() {
        pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();
    }

    @org.junit.Before
    public void createPartitioningParamBuilder() {
        Sink sink = new SinkBuilder().build();
        SinkCacheEntity sinkCacheEntity = SinkCacheEntity.create(sink);
        jobEntity = getJobEntity(0);
        jobEntity.setCachedSink(sinkCacheEntity);
        partitioningParamBuilder = new TestablePartitioningParamBuilder()
                .setJobEntity(jobEntity)
                .setFileStoreServiceConnector(mockedFileStoreServiceConnector)
                .setFlowStoreServiceConnector(mockedFlowStoreServiceConnector)
        ;
        setupMocks();
    }

    private void setupMocks() {
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(jobEntity);
    }

    @org.junit.Test
    public void partition_byteSizeNotFound_returnsSnapshotWithJobMarkedAsCompletedAndDiagnosticsAdded() throws FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Setup preconditions
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenThrow(fileStoreUnexpectedException);
        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(EXPECTED_SUBMITTER);

        PartitioningParam param = partitioningParamBuilder.build();

        // Subject Under Test
        Partitioning partitioning = pgJobStore.partition(param);
        JobInfoSnapshot jobInfoSnapshot = partitioning.getJobInfoSnapshot();

        // Verify
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        Diagnostic diagnostic = param.getJobEntity().getState().getDiagnostics().get(0);
        String diagnosticsStacktrace = diagnostic.getStacktrace();
        Assertions.assertFalse(param.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("Caused by: dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException: unexpected status code"));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("dk.dbc.dataio.jobstore.types.JobStoreException: Could not retrieve byte size"));
    }

    @org.junit.Test
    public void partition_differentByteSize_returnsSnapshotWithJobMarkedAsCompletedAndDiagnosticsAdded() throws FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Setup preconditions
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(99999L);
        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(EXPECTED_SUBMITTER);

        PartitioningParam param = partitioningParamBuilder.build();

        // Subject Under Test
        Partitioning partitioning = pgJobStore.partition(param);
        JobInfoSnapshot jobInfoSnapshot = partitioning.getJobInfoSnapshot();


        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        Diagnostic diagnostic = param.getJobEntity().getState().getDiagnostics().get(0);
        String diagnosticsMessage = diagnostic.getMessage();
        Assertions.assertFalse(param.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("DataPartitioner.byteSize was: 307"));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("FileStore.byteSize was: 99999"));
    }

    @org.junit.Test
    public void handlePartitioning_submitterEnabled_returnsJobInfoSnapshot() throws FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Setup preconditions
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(307L);
        when(mockedFlowStoreServiceConnector.getSubmitter(EXPECTED_SUBMITTER.getId())).thenReturn(EXPECTED_SUBMITTER);

        PartitioningParam param = partitioningParamBuilder.build();

        // Subject Under Test
        Partitioning partitioning = pgJobStore.handlePartitioning(param);
        JobInfoSnapshot jobInfoSnapshot = partitioning.getJobInfoSnapshot();

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(param.getDiagnostics()));
    }

    @org.junit.Test
    public void handlePartitioning_submitterDisabled_returnsJobInfoSnapshot() throws FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Setup preconditions
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(307L);
        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(new SubmitterBuilder().setContent(new SubmitterContentBuilder().setEnabled(false).build()).build());

        PartitioningParam param = partitioningParamBuilder.build();

        // Subject Under Test
        Partitioning preview = pgJobStore.handlePartitioning(param);
        JobInfoSnapshot jobInfoSnapshot = preview.getJobInfoSnapshot();

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(0));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(11));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion set", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(param.getDiagnostics()));
    }

    @org.junit.Test
    public void partition_noRecords_returnsJobInfoSnapshot() throws FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Setup preconditions
        byte[] records = "<records></records>".getBytes(StandardCharsets.UTF_8);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(Long.valueOf(records.length));
        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(EXPECTED_SUBMITTER);

        InputStream dataFileInputStream = new ByteArrayInputStream(records);
        PartitioningParam param = partitioningParamBuilder.setDataPartitioner(DefaultXmlDataPartitioner.newInstance(dataFileInputStream, StandardCharsets.UTF_8.name())).build();

        // Subject Under Test
        Partitioning partitioning = pgJobStore.partition(param);
        JobInfoSnapshot jobInfoSnapshot = partitioning.getJobInfoSnapshot();

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));

        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(0));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(0));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Processing phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getEndDate(), is(notNullValue()));
        assertThat("Delivering phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion set", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }


    @org.junit.Test
    public void compareByteSize_4GiBMultiple()
            throws FileStoreServiceConnectorException, IOException, JobStoreException {
        // The size reported by the file-store plus a multiple of four GiB
        // matches the number of bytes read
        final String fileId = "gzFileLargerThan4GiB";
        final long sizeOverflow = 123L;
        DataPartitioner dataPartitioner = mock(DataPartitioner.class);
        when(dataPartitioner.getBytesRead()).thenReturn(sizeOverflow + 4 * 1024 * 1024 * 1024L);
        when(mockedFileStoreServiceConnector.getByteSize(fileId)).thenReturn(sizeOverflow);

        pgJobStore.compareByteSize(fileId, dataPartitioner);
    }

    @org.junit.Test
    public void compareByteSize_noByteCountAvailable() throws IOException, JobStoreException {
        DataPartitioner dataPartitioner = mock(DataPartitioner.class);
        when(dataPartitioner.getBytesRead()).thenReturn(DataPartitioner.NO_BYTE_COUNT_AVAILABLE);
        pgJobStore.compareByteSize("fileId", dataPartitioner);
    }

    @org.junit.Test
    public void compareByteSize_bzip2() throws FileStoreServiceConnectorException, IOException, JobStoreException {
        DataPartitioner dataPartitioner = mock(DataPartitioner.class);
        when(dataPartitioner.getBytesRead())
                .thenReturn(42L);
        when(mockedFileStoreServiceConnector.getByteSize("fileId"))
                .thenReturn(-4_348_520L);
        pgJobStore.compareByteSize("fileId", dataPartitioner);
    }
}
