package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
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
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.StateElement;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static dk.dbc.dataio.commons.utils.service.Base64Util.base64decode;
import static dk.dbc.dataio.commons.utils.service.Base64Util.base64encode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PgJobStoreTest {
    private static final String ERROR_MESSAGE = "Error Message";
    private static final String FILE_STORE_URN_STRING = "urn:dataio-fs:67";
    private static final FlowCacheEntity EXPECTED_FLOW_CACHE_ENTITY = new FlowCacheEntity();
    private static final SinkCacheEntity EXPECTED_SINK_CACHE_ENTITY = new SinkCacheEntity();
    private static final int EXPECTED_NUMBER_OF_CHUNKS = 2;
    private static final int DEFAULT_JOB_ID = 1;
    private static final int DEFAULT_CHUNK_ID = 1;
    private static final short DEFAULT_ITEM_ID = 1;
    private static final FileStoreUrn FILE_STORE_URN;
    private static final List<String> EXPECTED_DATA_ENTRIES = Arrays.asList(
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>first</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>second</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>third</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>fourth</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>fifth</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>sixth</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>seventh</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>eighth</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>ninth</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>tenth</record></records>"),
            base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>eleventh</record></records>"));
    private static final int EXPECTED_NUMBER_OF_ITEMS = EXPECTED_DATA_ENTRIES.size();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final JobSchedulerBean jobSchedulerBean = mock(JobSchedulerBean.class);
    private static final FileStoreServiceConnectorUnexpectedStatusCodeException fileStoreUnexpectedException
            = new FileStoreServiceConnectorUnexpectedStatusCodeException("unexpected status code", 400);
    private final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
        try {
            FILE_STORE_URN = FileStoreUrn.create("42");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void setupExpectations() {
        final Query cacheFlowQuery = mock(Query.class);
        when(entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(cacheFlowQuery);
        when(cacheFlowQuery.getSingleResult()).thenReturn(EXPECTED_FLOW_CACHE_ENTITY);
        final Query cacheSinkQuery = mock(Query.class);
        when(entityManager.createNamedQuery(SinkCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(cacheSinkQuery);
        when(cacheSinkQuery.getSingleResult()).thenReturn(EXPECTED_SINK_CACHE_ENTITY);
    }

    @Test
    public void addAndScheduleJob_nullArgument_throws() throws Exception {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addAndScheduleJob(null);
            fail("No NullPointerException Thrown");
        } catch(NullPointerException e) {}
    }

    @Test
    public void addAndScheduleJob_jobAdded_returnsJobInfoSnapshot() throws Exception {
        final PgJobStore pgJobStore = newPgJobStore();
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final String xml = getXml();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        setupSuccessfulMockedReturnsFromFlowStore(jobInputStream.getJobSpecification());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(newTestableJobEntity(jobInputStream.getJobSpecification()));
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(byteArrayInputStream);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) xml.getBytes().length);

        try {
            final JobInfoSnapshot jobInfoSnapshotReturned = pgJobStore.addAndScheduleJob(jobInputStream);
            // Verify that the method getByteSize (used in compareByteSize) was invoked.
            verify(mockedFileStoreServiceConnector).getByteSize(anyString());
            assertThat(jobInfoSnapshotReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by addAndScheduleJob()");
        }
    }

    @Test
    public void addAndScheduleJob_differentByteSize_throwsJobStoreException() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final PgJobStore pgJobStore = newPgJobStore();
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(getXml().getBytes(StandardCharsets.UTF_8));

        setupSuccessfulMockedReturnsFromFlowStore(jobInputStream.getJobSpecification());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(newTestableJobEntity(jobInputStream.getJobSpecification()));
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(byteArrayInputStream);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(42L);

        try {
            pgJobStore.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {}
    }

    @Test
    public void addAndScheduleJob_byteSizeNotFound_throwsJobStoreException() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final PgJobStore pgJobStore = newPgJobStore();
        final JobInputStream jobInputStream = getJobInputStream(FILE_STORE_URN_STRING);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(getXml().getBytes(StandardCharsets.UTF_8));

        setupSuccessfulMockedReturnsFromFlowStore(jobInputStream.getJobSpecification());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(newTestableJobEntity(jobInputStream.getJobSpecification()));
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenThrow(fileStoreUnexpectedException);
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(byteArrayInputStream);
        try {
            pgJobStore.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(JobStoreException e) {}
    }

    @Test
    public void compareByteSize_byteSizesIdentical() throws IOException, FileStoreServiceConnectorException, JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final DataPartitionerFactory.DataPartitioner mockedDataPartitioner = mock(DataPartitionerFactory.DataPartitioner.class);
        final String xml = getXml();

        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) xml.getBytes().length);
        when(mockedDataPartitioner.getBytesRead()).thenReturn((long) xml.getBytes().length);

        pgJobStore.compareByteSize("42", mockedDataPartitioner);
    }

    @Test
    public void compareByteSize_byteSizesNotIdentical_throwsJobStoreException() throws IOException, JobStoreException, FileStoreServiceConnectorException {
        final PgJobStore pgJobStore = newPgJobStore();
        final DataPartitionerFactory.DataPartitioner mockedDataPartitioner = mock(DataPartitionerFactory.DataPartitioner.class);
        final String xml = getXml();

        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) xml.getBytes().length);
        when(mockedDataPartitioner.getBytesRead()).thenReturn((long)(xml.getBytes().length - 1));
        try {
            pgJobStore.compareByteSize("42", mockedDataPartitioner);
        } catch (IOException e) {}
    }

    @Test
    public void addJob_addJobParamArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addJob(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void addJob_diagnosticWithLevelFatalFound_returnsJobInformationSnapshotWithJobMarkedAsCompleted() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        mockedAddJobParam.setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.FATAL, ERROR_MESSAGE)));

        JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
        assertThat("State.Phase.PARTITIONING.beginDate", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getBeginDate(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedAddJobParam.getDiagnostics()));
        assertThat("JobInfoSnapshot.FlowStoreReferences", jobInfoSnapshot.getFlowStoreReferences(), is(mockedAddJobParam.getFlowStoreReferences()));
    }

    @Test
    public void addJob_allArgsAreValid_returnsJobInformationSnapshot() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        final FlowStoreReferences flowStoreReferences = buildFlowStoreReferences(
                mockedAddJobParam.getSubmitter(),
                mockedAddJobParam.getFlowBinder(),
                mockedAddJobParam.getFlow(),
                mockedAddJobParam.getSink());

        mockedAddJobParam.setFlowStoreReferences(flowStoreReferences);

        final TestableJobEntity jobEntity = new TestableJobEntity();

        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(new State());
        jobEntity.setFlowStoreReferences(mockedAddJobParam.getFlowStoreReferences());
        jobEntity.setSpecification(mockedAddJobParam.getJobInputStream().getJobSpecification());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);

        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedAddJobParam.getDiagnostics()));
        assertThat("JobInfoSnapshot.FlowStoreReferences",jobInfoSnapshot.getFlowStoreReferences(), is(mockedAddJobParam.getFlowStoreReferences()));
    }

    @Test
    public void addJob_diagnosticWithLevelWarningFound_returnsJobInformationSnapshot() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        mockedAddJobParam.setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.WARNING, ERROR_MESSAGE)));

        final FlowStoreReferences flowStoreReferences = buildFlowStoreReferences(
                mockedAddJobParam.getSubmitter(),
                mockedAddJobParam.getFlowBinder(),
                mockedAddJobParam.getFlow(),
                mockedAddJobParam.getSink());

        mockedAddJobParam.setFlowStoreReferences(flowStoreReferences);

        final State state = new State();
        state.getDiagnostics().addAll(mockedAddJobParam.getDiagnostics());

        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(state);
        jobEntity.setFlowStoreReferences(mockedAddJobParam.getFlowStoreReferences());
        jobEntity.setSpecification(new JobSpecificationBuilder().build());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);

        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedAddJobParam.getDiagnostics()));
        assertThat("JobInfoSnapshot.FlowStoreReferences",jobInfoSnapshot.getFlowStoreReferences(), is(mockedAddJobParam.getFlowStoreReferences()));
    }

    @Test
    public void createChunkItemEntities() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Params params = new Params();

        PgJobStore.ChunkItemEntities chunkItemEntities =
                pgJobStore.createChunkItemEntities(1, 0, params.maxChunkSize, params.dataPartitioner);
        assertThat("First chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("First chunk: number of items", chunkItemEntities.size(), is((short) 10));
        assertThat("First chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
        assertChunkItemEntities(chunkItemEntities, State.Phase.PARTITIONING, EXPECTED_DATA_ENTRIES.subList(0, 10), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.createChunkItemEntities(1, 1, params.maxChunkSize, params.dataPartitioner);
        assertThat("Second chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkItemEntities.size(), is((short) 1));
        assertThat("Second chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
        assertChunkItemEntities(chunkItemEntities, State.Phase.PARTITIONING, EXPECTED_DATA_ENTRIES.subList(10, 11), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.createChunkItemEntities(1, 2, params.maxChunkSize, params.dataPartitioner);
        assertThat("Third chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Third chunk: number of items", chunkItemEntities.size(), is((short) 0));
        assertThat("Third chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
    }

    @Test
    public void createChunkItemEntities_dataPartitionerThrowsDataException_failedItemIsCreated() throws JobStoreException, JSONBException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Params params = new Params();
        final String invalidXml =
                  "<records>"
                + "<record>first</record>"
                + "<record>second"
                + "</records>";

        params.dataPartitioner =  new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());

        final PgJobStore.ChunkItemEntities chunkItemEntities =
                pgJobStore.createChunkItemEntities(1, 0, params.maxChunkSize, params.dataPartitioner);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) 2));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("First item: succeeded", chunkItemEntities.entities.get(0).getState().getPhase(State.Phase.PARTITIONING).getSucceeded(), is(1));
        assertThat("Second item: failed", chunkItemEntities.entities.get(1).getState().getPhase(State.Phase.PARTITIONING).getFailed(), is(1));

        final JobError jobError = new JSONBContext().unmarshall(base64decode(
                chunkItemEntities.entities.get(1).getPartitioningOutcome().getData()), JobError.class);
        assertThat(jobError.getCode(), is(JobError.Code.INVALID_DATA));
        assertThat(jobError.getDescription().isEmpty(), is(false));
        assertThat(jobError.getStacktrace().isEmpty(), is(false));
    }

    @Test
    public void createChunkEntity() throws JobStoreException {
        final Params params = new Params();
        final PgJobStore pgJobStore = newPgJobStore();
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setState(new State());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        ChunkEntity chunkEntity = pgJobStore.createChunkEntity(
                1, 0, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.dataFileId);
        assertThat("First chunk", chunkEntity, is(notNullValue()));
        assertThat("First chunk: number of items", chunkEntity.getNumberOfItems(), is(params.maxChunkSize));
        assertThat("First chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("First chunk: number of seq keys", chunkEntity.getSequenceAnalysisData().getData().size(), is(1));
        assertThat("First chunk: seq keys", chunkEntity.getSequenceAnalysisData().getData().contains(params.sink.getContent().getName()), is(true));
        assertThat("Job: number of chunks after first chunk", jobEntity.getNumberOfChunks(), is(1));
        assertThat("Job: number of items after first chunk", jobEntity.getNumberOfItems(), is((int) params.maxChunkSize));
        assertThat("Job: partitioning phase endDate not set after first chunk", jobEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(nullValue()));

        chunkEntity = pgJobStore.createChunkEntity(
                1, 1, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.dataFileId);
        assertThat("Second chunk", chunkEntity, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkEntity.getNumberOfItems(), is((short) (EXPECTED_NUMBER_OF_ITEMS - params.maxChunkSize)));
        assertThat("Second chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Second chunk: number of seq keys", chunkEntity.getSequenceAnalysisData().getData().size(), is(1));
        assertThat("Second chunk: seq keys", chunkEntity.getSequenceAnalysisData().getData().contains(params.sink.getContent().getName()), is(true));
        assertThat("Job: number of chunks after second chunk", jobEntity.getNumberOfChunks(), is(2));
        assertThat("Job: number of items after second chunk", jobEntity.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Job: partitioning phase endDate not set after second chunk", jobEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(nullValue()));

        chunkEntity = pgJobStore.createChunkEntity(
                1, 2, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.dataFileId);
        assertThat("Third chunk", chunkEntity, is(nullValue()));
        assertThat("Job: number of chunks after third chunk", jobEntity.getNumberOfChunks(), is(2));
        assertThat("Job: number of items after third chunk", jobEntity.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Job: partitioning phase endDate not set after third chunk", jobEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(nullValue()));
    }

    @Test
    public void addChunk_chunkArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addChunk_chunkEntityCanNotBeFound_throws() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(null);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_jobEntityCanNotBeFound_throws() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class)))
                .thenReturn(getChunkEntity(1, 0, chunk.size(), Collections.singletonList(State.Phase.PARTITIONING)));
        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(null);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_numberOfExternalChunkItemsDiffersFromInternal_throws() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class)))
                .thenReturn(getChunkEntity(1, 0, chunk.size() + 42, Collections.singletonList(State.Phase.PARTITIONING)));

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_attemptToOverwriteExistingChunk_throws() throws JobStoreException {
        PgJobStore pgJobStore = null;
        ExternalChunk chunk = null;
        try {
            final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
            chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                    Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
            setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

            final ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Collections.singletonList(State.Phase.PARTITIONING));
            final JobEntity jobEntity = getJobEntity(chunk.size(), Collections.singletonList(State.Phase.PARTITIONING));

            when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
            when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

            pgJobStore = newPgJobStore();
            pgJobStore.addChunk(chunk);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            pgJobStore.addChunk(chunk);
            fail("No exception thrown");
        } catch (DuplicateChunkException e) {
        }
    }

    @Test
    public void addChunk_chunkAdded_chunkEntityPhaseComplete() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        final ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Collections.singletonList(State.Phase.PARTITIONING));
        final JobEntity jobEntity = getJobEntity(chunk.size(), Collections.singletonList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);

        // assert ChunkEntity (counters + beginDate and endDate set)

        final StateElement chunkStateElement = chunkEntity.getState().getPhase(State.Phase.PROCESSING);
        assertThat("ChunkEntity: processing phase beginDate set", chunkStateElement.getBeginDate(), is(notNullValue()));
        assertThat("ChunkEntity: processing phase endDate set", chunkStateElement.getEndDate(), is(notNullValue()));
        assertThat("ChunkEntity: number of failed items", chunkStateElement.getFailed(), is(1));
        assertThat("ChunkEntity: number of ignored items", chunkStateElement.getIgnored(), is(1));
        assertThat("ChunkEntity: number of succeeded items", chunkStateElement.getSucceeded(), is(1));
        assertThat("ChunkEntity: number of items", chunkEntity.getNumberOfItems(), is((short) chunk.size()));
        assertThat("ChunkEntity: time of completion not set", chunkEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("JobInfoSnapshot: time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
    }

    @Test
    public void addChunk_finalChunkAdded_jobEntityPhaseComplete() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        final ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Collections.singletonList(State.Phase.PARTITIONING));
        final JobEntity jobEntity = getJobEntity(chunk.size(), Collections.singletonList(State.Phase.PARTITIONING));
        jobEntity.getState().getPhase(State.Phase.PARTITIONING).setSucceeded(126);
        jobEntity.getState().getPhase(State.Phase.PROCESSING).setFailed(41);
        jobEntity.getState().getPhase(State.Phase.PROCESSING).setIgnored(41);
        jobEntity.getState().getPhase(State.Phase.PROCESSING).setSucceeded(41);

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot: time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        // assert JobEntity (counters + beginDate and endDate set)

        final StateElement jobStateElement = jobEntity.getState().getPhase(State.Phase.PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate set", jobStateElement.getEndDate(), is(notNullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(42));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(42));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(42));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
        assertThat("JobEntity: time of completion not set", jobEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("ChunkEntity: time of completion not set", chunkEntity.getTimeOfCompletion(), is(nullValue()));
    }

    @Test
    public void addChunk_nonFinalChunkAdded_jobEntityPhaseIncomplete() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        final ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Collections.singletonList(State.Phase.PARTITIONING));
        final JobEntity jobEntity = getJobEntity(chunk.size(), Collections.<State.Phase>emptyList());

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot: time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        // assert JobEntity (counters + beginDate set, endData == null)

        final StateElement jobStateElement = jobEntity.getState().getPhase(State.Phase.PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate not set", jobStateElement.getEndDate(), is(nullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(1));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(1));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(1));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
        assertThat("JobEntity: time of completion not set", jobEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("ChunkEntity: time of completion not set", chunkEntity.getTimeOfCompletion(), is(nullValue()));
    }

    @Test
    public void addChunk_allPhasesComplete_timeOfCompletionIsSet() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.DELIVERED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS));
        setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING, State.Phase.PROCESSING));

        ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Arrays.asList(State.Phase.PARTITIONING, State.Phase.PROCESSING));
        JobEntity jobEntity = getJobEntity(chunk.size(), Arrays.asList(State.Phase.PARTITIONING, State.Phase.PROCESSING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));

        final StateElement jobStateElement = jobEntity.getState().getPhase(State.Phase.PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate set", jobStateElement.getEndDate(), is(notNullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(0));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(0));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(3));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
        assertThat("JobEntity: time of completion", jobEntity.getTimeOfCompletion(), is(notNullValue()));
    }

    @Test
    public void updateChunkItemEntities_itemsCanNotBeFound_throws() throws JobStoreException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(null);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.updateChunkItemEntities(chunk);
            fail("No exception thrown");
        } catch (InvalidInputException e) {
            assertThat("JobError:", e.getJobError(), is(notNullValue()));
            assertThat("JobError: code", e.getJobError().getCode(), is(JobError.Code.INVALID_ITEM_IDENTIFIER));
        }
    }

    @Test
    public void updateChunkItemEntities_itemsForPartitioningPhase_throws() throws JobStoreException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).build();
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setState(new State());

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.updateChunkItemEntities(chunk);
            fail("No exception thrown");
        } catch (InvalidInputException e) {
            assertThat("JobError:", e.getJobError(), is(notNullValue()));
            assertThat("JobError: code", e.getJobError().getCode(), is(JobError.Code.ILLEGAL_CHUNK));
        }
    }

    @Test
    public void updateChunkItemEntities_itemsForProcessingPhase() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        final List<ItemEntity> entities = setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        final PgJobStore pgJobStore = newPgJobStore();
        final PgJobStore.ChunkItemEntities chunkItemEntities = pgJobStore.updateChunkItemEntities(chunk);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) chunk.size()));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("Chunk: number of ignored items", chunkItemEntities.chunkStateChange.getIgnored(), is(1));
        assertThat("Chunk: number of succeeded items", chunkItemEntities.chunkStateChange.getSucceeded(), is(1));

        assertChunkItemEntities(chunkItemEntities, State.Phase.PROCESSING, chunkData, StandardCharsets.UTF_8);
        StateElement entityStateElement = entities.get(0).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(0).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(0).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(0).getKey()),
                entityStateElement.getSucceeded(), is(1));
        entityStateElement = entities.get(1).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(1).getKey()),
                entityStateElement.getFailed(), is(1));
        assertThat(String.format("%s ignored counter", entities.get(1).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(1).getKey()),
                entityStateElement.getSucceeded(), is(0));
        entityStateElement = entities.get(2).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(2).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(2).getKey()),
                entityStateElement.getIgnored(), is(1));
        assertThat(String.format("%s succeeded counter", entities.get(2).getKey()),
                entityStateElement.getSucceeded(), is(0));
    }

    @Test
    public void updateChunkItemEntities_itemsForDeliveringPhase() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.DELIVERED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        final List<ItemEntity> entities = setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        final PgJobStore pgJobStore = newPgJobStore();
        final PgJobStore.ChunkItemEntities chunkItemEntities = pgJobStore.updateChunkItemEntities(chunk);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) chunk.size()));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("Chunk: number of ignored items", chunkItemEntities.chunkStateChange.getIgnored(), is(1));
        assertThat("Chunk: number of succeeded items", chunkItemEntities.chunkStateChange.getSucceeded(), is(1));

        assertChunkItemEntities(chunkItemEntities, State.Phase.DELIVERING, chunkData, StandardCharsets.UTF_8);
        StateElement entityStateElement = entities.get(0).getState().getPhase(State.Phase.DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(0).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(0).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(0).getKey()),
                entityStateElement.getSucceeded(), is(1));
        entityStateElement = entities.get(1).getState().getPhase(State.Phase.DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(1).getKey()),
                entityStateElement.getFailed(), is(1));
        assertThat(String.format("%s ignored counter", entities.get(1).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(1).getKey()),
                entityStateElement.getSucceeded(), is(0));
        entityStateElement = entities.get(2).getState().getPhase(State.Phase.DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(2).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(2).getKey()),
                entityStateElement.getIgnored(), is(1));
        assertThat(String.format("%s succeeded counter", entities.get(2).getKey()),
                entityStateElement.getSucceeded(), is(0));
    }

    @Test
    public void updateChunkItemEntities_attemptToOverwriteAlreadyAddedChunk_throws() throws JobStoreException {
        final List<String> chunkData = Collections.singletonList("itemData0");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Collections.singletonList(ChunkItem.Status.SUCCESS));
        final List<String> chunkDataOverwrite = Collections.singletonList("itemData0-overwrite");
        final ExternalChunk chunkOverwrite = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkDataOverwrite,
                Collections.singletonList(ChunkItem.Status.FAILURE));

        setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        final PgJobStore pgJobStore = newPgJobStore();
        // add original chunk
        pgJobStore.updateChunkItemEntities(chunk);

        try {
            pgJobStore.updateChunkItemEntities(chunkOverwrite);
            fail("No exception thrown");
        } catch (DuplicateChunkException e) {
        }
    }

    @Test
    public void getResourceBundle_jobEntityNotFound_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(null);
        try {
            pgJobStore.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (JobStoreException e) {}
    }

    @Test
    public void getResourceBundle_flowIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Sink sink = new SinkBuilder().build();

        FlowCacheEntity mockedFlowCacheEntity = mock(FlowCacheEntity.class);
        SinkCacheEntity mockedSinkCacheEntity = mock(SinkCacheEntity.class);

        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID, Collections.singletonList(State.Phase.PARTITIONING));
        jobEntity.setCachedFlow(mockedFlowCacheEntity);
        jobEntity.setCachedSink(mockedSinkCacheEntity);

        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(null);
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        try {
            pgJobStore.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void getResourceBundle_sinkIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Flow flow = new FlowBuilder().build();

        FlowCacheEntity mockedFlowCacheEntity = mock(FlowCacheEntity.class);
        SinkCacheEntity mockedSinkCacheEntity = mock(SinkCacheEntity.class);

        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID, Collections.singletonList(State.Phase.PARTITIONING));
        jobEntity.setCachedFlow(mockedFlowCacheEntity);
        jobEntity.setCachedSink(mockedSinkCacheEntity);

        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(flow);
        when(jobEntity.getCachedSink().getSink()).thenReturn(null);

        try {
            pgJobStore.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void getResourceBundle_resourcesAddedToBundle_returns() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();

        FlowCacheEntity mockedFlowCacheEntity = mock(FlowCacheEntity.class);
        SinkCacheEntity mockedSinkCacheEntity = mock(SinkCacheEntity.class);

        JobEntity jobEntity = getJobEntity(1, Collections.singletonList(State.Phase.PARTITIONING));
        jobEntity.setCachedFlow(mockedFlowCacheEntity);
        jobEntity.setCachedSink(mockedSinkCacheEntity);

        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(flow);
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        final ResourceBundle resourceBundle = pgJobStore.getResourceBundle(jobEntity.getId());
        assertThat("ResourceBundle not null", resourceBundle, not(nullValue()));
        assertThat(String.format("ResourceBundle.flow: %s expected to match: %s", resourceBundle.getFlow(), flow), resourceBundle.getFlow(), is(flow));
        assertThat(String.format("ResourceBundle.sink: %s expected to match: %s", resourceBundle.getSink(), sink), resourceBundle.getSink(), is(sink));

        assertThat(String.format("ResourceBundle.supplementaryProcessData.format: %s expected to match: %s:",
                        resourceBundle.getSupplementaryProcessData().getFormat(),
                        jobEntity.getSpecification().getFormat()),
                resourceBundle.getSupplementaryProcessData().getFormat(), is(jobEntity.getSpecification().getFormat()));

        assertThat(String.format("ResourceBundle.supplementaryProcessData.submitter: %s expected to match: %s:",
                        resourceBundle.getSupplementaryProcessData().getSubmitter(),
                        jobEntity.getSpecification().getSubmitterId()),
                resourceBundle.getSupplementaryProcessData().getSubmitter(), is(jobEntity.getSpecification().getSubmitterId()));
    }

    @Test
    public void cacheFlow_flowArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.cacheFlow(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheFlow_flowArgIsCached_returnsFlowCacheEntityInstance() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(new FlowBuilder().build());
        assertThat(flowCacheEntity, is(EXPECTED_FLOW_CACHE_ENTITY));
    }

    @Test
    public void cacheSink_sinkArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.cacheSink(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheSink_sinkArgIsCached_returnsSinkCacheEntityInstance() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(new SinkBuilder().build());
        assertThat(sinkCacheEntity, is(EXPECTED_SINK_CACHE_ENTITY));
    }

    @Test
    public void listJobs_criteriaArgIsNull_throws() {
       final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.listJobs(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listJobs_queryReturnsEmptyList_returnsEmptySnapshotList() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(JobEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        final List<JobInfoSnapshot> jobInfoSnapshots = pgJobStore.listJobs(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listJobs_queryReturnsNonEmptyList_returnsSnapshotList() {
        final Query query = mock(Query.class);
        final JobEntity jobEntity1 = new JobEntity();
        jobEntity1.setNumberOfItems(42);
        jobEntity1.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
        final JobEntity jobEntity2 = new JobEntity();
        jobEntity2.setNumberOfItems(4242);
        jobEntity2.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
        when(entityManager.createNativeQuery(anyString(), eq(JobEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(jobEntity1, jobEntity2));

        final PgJobStore pgJobStore = newPgJobStore();
        final List<JobInfoSnapshot> jobInfoSnapshots = pgJobStore.listJobs(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot size", jobInfoSnapshots.size(), is(2));
        assertThat("List of JobInfoSnapshot first element numberOfItems",
                jobInfoSnapshots.get(0).getNumberOfItems(), is(jobEntity1.getNumberOfItems()));
        assertThat("List of JobInfoSnapshot second element numberOfItems",
                jobInfoSnapshots.get(1).getNumberOfItems(), is(jobEntity2.getNumberOfItems()));
    }

    @Test
    public void listItems_queryReturnsEmptyList_returnsEmptySnapshotList() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        final List<ItemInfoSnapshot> itemInfoSnapshots = pgJobStore.listItems(new ItemListCriteria());
        assertThat("List of ItemInfoSnapshot", itemInfoSnapshots, is(notNullValue()));
        assertThat("List of ItemInfoSnapshot is empty", itemInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listItems_queryReturnsNonEmptyList_returnsSnapshotList() {
        final Query query = mock(Query.class);
        final ItemEntity itemEntity1 = new ItemEntity();
        itemEntity1.setKey(new ItemEntity.Key(1, 0, (short) 0));
        final ItemEntity itemEntity2 = new ItemEntity();
        itemEntity2.setKey(new ItemEntity.Key(1, 0,(short) 1));
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(itemEntity1, itemEntity2));

        final PgJobStore pgJobStore = newPgJobStore();
        final List<ItemInfoSnapshot> itemInfoSnapshots = pgJobStore.listItems(new ItemListCriteria());
        assertThat("List of ItemInfoSnapshot", itemInfoSnapshots, is(notNullValue()));
        assertThat("List of ItemInfoSnapshot size", itemInfoSnapshots.size(), is(2));
        assertThat("List of ItemInfoSnapshot first element itemId",
                itemInfoSnapshots.get(0).getItemId(), is(itemEntity1.getKey().getId()));
        assertThat("List of ItemInfoSnapshot first element chunkId",
                itemInfoSnapshots.get(0).getChunkId(), is(itemEntity1.getKey().getChunkId()));
        assertThat("List of ItemInfoSnapshot first element jobId",
                itemInfoSnapshots.get(0).getJobId(), is(itemEntity1.getKey().getJobId()));
        assertThat("List of ItemInfoSnapshot first element itemNumber",
                itemInfoSnapshots.get(0).getItemNumber(), is(1));

        assertThat("List of JobInfoSnapshot second element itemId",
                itemInfoSnapshots.get(1).getItemId(), is(itemEntity2.getKey().getId()));
        assertThat("List of JobInfoSnapshot second element chunkId",
                itemInfoSnapshots.get(1).getChunkId(), is(itemEntity2.getKey().getChunkId()));
        assertThat("List of JobInfoSnapshot second element jobId",
                itemInfoSnapshots.get(1).getJobId(), is(itemEntity2.getKey().getJobId()));
        assertThat("List of ItemInfoSnapshot second element itemNumber",
                itemInfoSnapshots.get(1).getItemNumber(), is(2));
    }

    @Test
    public void listChunksCollisionDetectionElements_criteriaArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.listChunksCollisionDetectionElements(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listChunksCollisionDetectionElements_queryReturnsEmptyList_returnsEmptyCollisionDetectionElementList() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ChunkEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        final List<CollisionDetectionElement> collisionDetectionElements = pgJobStore.listChunksCollisionDetectionElements(new ChunkListCriteria());
        assertThat("List of CollisionDetectionElement", collisionDetectionElements, is(notNullValue()));
        assertThat("List of CollisionDetectionElement is empty", collisionDetectionElements.isEmpty(), is(true));
    }

    @Test
    public void listChunksCollisionDetectionElements_queryReturnsNonEmptyList_returnsCollisionDetectionElementList() {
        final Query query = mock(Query.class);
        final SequenceAnalysisData mockedSequenceAnalysisData = mock(SequenceAnalysisData.class);
        final ChunkEntity chunkEntity1 = new ChunkEntity();
        chunkEntity1.setKey(new ChunkEntity.Key(1, 1));
        chunkEntity1.setSequenceAnalysisData(mockedSequenceAnalysisData);

        final ChunkEntity chunkEntity2 = new ChunkEntity();
        chunkEntity2.setKey(new ChunkEntity.Key(0, 1));
        chunkEntity2.setSequenceAnalysisData(mockedSequenceAnalysisData);

        when(entityManager.createNativeQuery(anyString(), eq(ChunkEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(chunkEntity1, chunkEntity2));


        final PgJobStore pgJobStore = newPgJobStore();
        final List<CollisionDetectionElement> collisionDetectionElements = pgJobStore.listChunksCollisionDetectionElements(new ChunkListCriteria());
        assertThat("List of CollisionDetectionElement", collisionDetectionElements, is(notNullValue()));
        assertThat("List of CollisionDetectionElement size", collisionDetectionElements.size(), is(2));
        assertThat("List of CollisionDetectionElement first element numberOfItems",
                ((ChunkIdentifier) collisionDetectionElements.get(0).getIdentifier()).getChunkId(), is((long) chunkEntity1.getKey().getId()));
        assertThat("List of CollisionDetectionElement second element numberOfItems",
                ((ChunkIdentifier) collisionDetectionElements.get(1).getIdentifier()).getChunkId(), is((long) chunkEntity2.getKey().getId()));
    }

    @Test
    public void getChunk_typeArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.getChunk(null, 2, 1);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getChunk_queryReturnsEmptyList_returnsNull() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        assertThat(pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, 2, 1), is(nullValue()));
    }

    @Test
    public void getChunk_queryReturnsItemEntityWithoutData_throws() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new ItemEntity()));

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, 2, 1);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getChunk_queryReturnsItemEntityWithData_returnsChunk() {
        final ItemData data1 = new ItemData("data1", StandardCharsets.UTF_8);
        final State state1 = new State();
        state1.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
        final ItemEntity entity1 = new ItemEntity();
        entity1.setKey(new ItemEntity.Key(2, 1, (short) 0));
        entity1.setPartitioningOutcome(data1);
        entity1.setState(state1);

        final ItemData data2 = new ItemData("data2", StandardCharsets.ISO_8859_1);
        final State state2 = new State();
        state2.getPhase(State.Phase.PARTITIONING).setFailed(1);
        final ItemEntity entity2 = new ItemEntity();
        entity2.setKey(new ItemEntity.Key(2, 1, (short) 1));
        entity2.setPartitioningOutcome(data2);
        entity2.setState(state2);

        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(entity1, entity2));

        final PgJobStore pgJobStore = newPgJobStore();
        final ExternalChunk chunk = pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, 2, 1);
        assertThat("chunk", chunk, is(notNullValue()));
        assertThat("chunk.size()", chunk.size(), is(2));
        assertThat("chunk.getEncoding()", chunk.getEncoding(), is(data1.getEncoding()));
        final Iterator<ChunkItem> iterator = chunk.iterator();
        final ChunkItem firstChunkItem = iterator.next();
        assertThat("chunk[0].getId()", firstChunkItem.getId(), is((long) entity1.getKey().getId()));
        assertThat("chunk[0].getData()", firstChunkItem.getData(), is(data1.getData()));
        final ChunkItem secondChunkItem = iterator.next();
        assertThat("chunk[1].getId()", secondChunkItem.getId(), is((long) entity2.getKey().getId()));
        assertThat("chunk[1].getData()", secondChunkItem.getData(), is(data2.getData()));
    }

    @Test
    public void getItemData_itemEntityNotFound_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(null);
        try {
            pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PARTITIONING);
            fail("No exception thrown");
        } catch (JobStoreException e) {}
    }

    @Test
    public void getItemData_phasePartitioning_returnsItemData() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();

        ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final ItemData itemData = pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PARTITIONING);
        assertThat("itemData not null", itemData, not(nullValue()));
        assertThat(String.format("itemData.data: {%s} expected to match: {%s}", itemData.getData(), itemEntity.getPartitioningOutcome().getData()),
                itemData.getData(), is(itemEntity.getPartitioningOutcome().getData()));
    }


    @Test
    public void getItemData_phaseProcessing_returnsItemData() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();

        ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final ItemData itemData = pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PROCESSING);
        assertThat("itemData not null", itemData, not(nullValue()));
        assertThat(String.format("itemData.data: {%s} expected to match: {%s}", itemData.getData(), itemEntity.getProcessingOutcome().getData()),
                itemData.getData(), is(itemEntity.getProcessingOutcome().getData()));
    }

    @Test
    public void getItemData_phaseDelivering_returnsItemData() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();

        ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final ItemData itemData = pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.DELIVERING);
        assertThat("itemData not null", itemData, not(nullValue()));
        assertThat(String.format("itemData.data: {%s} expected to match: {%s}", itemData.getData(), itemEntity.getDeliveringOutcome().getData()),
                itemData.getData(), is(itemEntity.getDeliveringOutcome().getData()));
    }

    /*
     * Private methods
     */

    private PgJobStore newPgJobStore() {
        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.jobSchedulerBean = jobSchedulerBean;
        pgJobStore.entityManager = entityManager;
        pgJobStore.sessionContext = sessionContext;
        pgJobStore.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;
        pgJobStore.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
        when(sessionContext.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);
        when(mockedFileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);
        when(mockedFlowStoreServiceConnectorBean.getConnector()).thenReturn(mockedFlowStoreServiceConnector);
        return pgJobStore;
    }

    private ItemEntity getItemEntity(int jobId, int chunkId, short itemId, List<State.Phase> phasesDone) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemKey);
        final StateChange itemStateChange = new StateChange();
        final State itemState = new State();
        for (State.Phase phase : phasesDone) {
            itemStateChange.setPhase(phase)
                    .setSucceeded(1)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());

            itemState.updateState(itemStateChange);
        }

        itemEntity.setState(itemState);
        return itemEntity;
    }

    private JobEntity getJobEntity(int numberOfItems, List<State.Phase> phasesDone) {
        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setNumberOfItems(numberOfItems);
        final StateChange jobStateChange = new StateChange();
        final State jobState = new State();
        for (State.Phase phase : phasesDone) {
            jobStateChange.setPhase(phase)
                    .setSucceeded(numberOfItems)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
            jobState.updateState(jobStateChange);
        }

        jobEntity.setState(jobState);
        jobEntity.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
        jobEntity.setSpecification(new JobSpecificationBuilder().build());
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        return jobEntity;
    }

    private ChunkEntity getChunkEntity(int jobId, int chunkId, int numberOfItems, List<State.Phase> phasesDone) {
        final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobId);
        final ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(chunkKey);
        chunkEntity.setNumberOfItems((short) numberOfItems);
        final StateChange chunkStateChange = new StateChange();
        final State chunkState = new State();
        for (State.Phase phase : phasesDone) {
            chunkStateChange.setPhase(phase)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
            chunkState.updateState(chunkStateChange);
        }
        chunkEntity.setSequenceAnalysisData(new SequenceAnalysisData(Collections.<String>emptySet()));

        chunkEntity.setState(chunkState);
        return chunkEntity;
    }

    private ItemEntity getItemEntity(int jobId, int chunkId, short itemId) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemKey);
        itemEntity.setState(new State());
        itemEntity.setPartitioningOutcome(new ItemData("Partitioning data", Charset.defaultCharset()));
        itemEntity.setProcessingOutcome(new ItemData("processing data", Charset.defaultCharset()));
        itemEntity.setDeliveringOutcome(new ItemData("delivering data", Charset.defaultCharset()));
        return itemEntity;
    }

    private ExternalChunk getExternalChunk(int jobId, int chunkId, ExternalChunk.Type type, List<String> data, List<ChunkItem.Status> statuses) {
        final List<ChunkItem> chunkItems = new ArrayList<>(data.size());
        final LinkedList<ChunkItem.Status> statusStack = new LinkedList<>(statuses);
        short itemId = 0;
        for (String itemData : data) {
            chunkItems.add(new ChunkItem(itemId++, itemData, statusStack.pop()));
        }
        return new ExternalChunkBuilder(type)
                .setJobId(jobId)
                .setChunkId(chunkId)
                .setItems(chunkItems)
                .build();
    }

    private List<ItemEntity> setItemEntityExpectations(ExternalChunk chunk, List<State.Phase> phasesDone) {
        final List<ItemEntity> entities = new ArrayList<>(chunk.size());
        for (ChunkItem chunkItem : chunk) {
            final ItemEntity itemEntity = getItemEntity((int) chunk.getJobId(), (int) chunk.getChunkId(), (short) chunkItem.getId(), phasesDone);
            entities.add(itemEntity);
        }
        final ItemEntity[] expectations = entities.toArray(new ItemEntity[entities.size()]);
        if (expectations.length > 1) {
            when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class)))
                    .thenReturn(expectations[0], Arrays.copyOfRange(expectations, 1, expectations.length));
        } else {
            when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class)))
                    .thenReturn(expectations[0]);
        }
        return entities;
    }

    private void assertChunkItemEntities(PgJobStore.ChunkItemEntities chunkItemEntities, State.Phase phase, List<String> dataEntries, Charset dataEncoding) {
        final LinkedList<String> expectedData = new LinkedList<>(dataEntries);
        assertThat("Chunk item entities: phase", chunkItemEntities.chunkStateChange.getPhase(), is(phase));

        for (ItemEntity itemEntity : chunkItemEntities.entities) {
            final String itemEntityKey = String.format("ItemEntity.Key{jobId=%d, chunkId=%d, itemId=%d}",
                    itemEntity.getKey().getJobId(), itemEntity.getKey().getChunkId(), itemEntity.getKey().getId());

            assertThat(String.format("%s %s phase beginDate set", itemEntityKey, phase),
                    itemEntity.getState().getPhase(phase).getEndDate(), is(notNullValue()));
            assertThat(String.format("%s %s phase endDate set", itemEntityKey, phase),
                    itemEntity.getState().getPhase(phase).getEndDate(), is(notNullValue()));

            ItemData itemData = null;
            switch (phase) {
                case PARTITIONING: itemData = itemEntity.getPartitioningOutcome();
                    break;
                case PROCESSING: itemData = itemEntity.getProcessingOutcome();
                    break;
                case DELIVERING: itemData = itemEntity.getDeliveringOutcome();
                    break;
            }
            assertThat(String.format("%s %s phase data", itemEntityKey, phase),
                    itemData.getData(), is(expectedData.pop()));
            assertThat(String.format("%s %s phase data encoding", itemEntityKey, phase),
                    itemData.getEncoding(), is(dataEncoding));
        }
    }

    private FlowStoreReferences buildFlowStoreReferences(Submitter submitter, FlowBinder flowBinder, Flow flow, Sink sink) {
        return new FlowStoreReferencesBuilder()
                .setFlowStoreReference(FlowStoreReferences.Elements.SUBMITTER,
                        new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.FLOW_BINDER,
                        new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.FLOW,
                        new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.SINK,
                        new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()))
                .build();
    }

    private JobInputStream getJobInputStream(String datafile) {
        JobSpecification jobSpecification = new JobSpecificationBuilder().setCharset("utf8").setDataFile(datafile).build();
        return new JobInputStream(jobSpecification, true, 3);
    }

    private TestableJobEntity newTestableJobEntity(JobSpecification jobSpecification) {
        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(new State());
        jobEntity.setSpecification(jobSpecification);
        return jobEntity;
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

    private void setupSuccessfulMockedReturnsFromFlowStore(JobSpecification jobSpecification) throws FlowStoreServiceConnectorException{
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();
        final Submitter submitter = new SubmitterBuilder().build();

        whenGetFlowBinderThenReturnFlowBinder(jobSpecification, flowBinder);
        when(mockedFlowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId())).thenReturn(flow);
        when(mockedFlowStoreServiceConnector.getSink(flowBinder.getContent().getSinkId())).thenReturn(sink);
        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(jobSpecification.getSubmitterId())).thenReturn(submitter);
    }

    private void whenGetFlowBinderThenReturnFlowBinder(JobSpecification jobSpecification, FlowBinder flowBinder) throws FlowStoreServiceConnectorException {
        when(mockedFlowStoreServiceConnector.getFlowBinder(
                jobSpecification.getPackaging(),
                jobSpecification.getFormat(),
                jobSpecification.getCharset(),
                jobSpecification.getSubmitterId(),
                jobSpecification.getDestination())).thenReturn(flowBinder);
    }

    /* Helper class for parameter values (with defaults)
     */
    private static class Params {
        final String xml =
                  "<records>"
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

        public JobInputStream jobInputStream;
        public DataPartitionerFactory.DataPartitioner dataPartitioner;
        public SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator;
        public Flow flow;
        public Sink sink;
        public FlowStoreReferences flowStoreReferences;
        public String dataFileId;
        public short maxChunkSize;

        public Params() {
            jobInputStream = new JobInputStream(new JobSpecificationBuilder().build(), true, 0);
            dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            flowStoreReferences = new FlowStoreReferencesBuilder().build();
            sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
            maxChunkSize = 10;
            dataFileId = "datafile";
        }
    }

    private class MockedAddJobParam extends AddJobParam {
        final String xml =
                "<records>"
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

        public MockedAddJobParam() {
            super(new JobInputStream(new JobSpecificationBuilder()
                    .setDataFile(FILE_STORE_URN.toString())
                    .build(), true, 0), mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
            submitter = new SubmitterBuilder().build();
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            flowBinder = new FlowBinderBuilder().build();
            flowStoreReferences = new FlowStoreReferencesBuilder().build();
            diagnostics = new ArrayList<>();

            sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
            dataFileInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream,
                    StandardCharsets.UTF_8.name());
        }

        public void setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
            this.flowStoreReferences = flowStoreReferences;
        }

        public void setDiagnostics(List<Diagnostic> diagnostics) {
            this.diagnostics.clear();
            this.diagnostics.addAll(diagnostics);
        }
    }

    private static class TestableJobEntity extends JobEntity {
        public void setTimeOfCreation(Timestamp timeOfCreation) {
            this.timeOfCreation = timeOfCreation;
        }
    }
}