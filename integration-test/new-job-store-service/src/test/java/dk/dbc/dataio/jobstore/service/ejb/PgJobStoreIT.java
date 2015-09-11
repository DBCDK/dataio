package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.SessionContext;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.jobstore.types.Diagnostic.Level.FATAL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStoreIT {

    private static final long SLEEP_INTERVAL_IN_MS = 1000;
    private static final long MAX_WAIT_IN_MS = 10000;

    public static final String DATABASE_NAME = "jobstore";
    public static final String JOB_TABLE_NAME = "job";
    public static final String CHUNK_TABLE_NAME = "chunk";
    public static final String ITEM_TABLE_NAME = "item";
    public static final String FLOW_CACHE_TABLE_NAME = "flowcache";
    public static final String SINK_CACHE_TABLE_NAME = "sinkcache";

    private static final String ERROR_MESSAGE = "Referenced entity not found";
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreIT.class);
    private static final FileStoreUrn FILE_STORE_URN;
    private static final SessionContext SESSION_CONTEXT = mock(SessionContext.class);
    private static final JobSchedulerBean JOB_SCHEDULER_BEAN = mock(JobSchedulerBean.class);

    private static final State.Phase PROCESSING = State.Phase.PROCESSING;
    private static final PGSimpleDataSource datasource;

    private final FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    
    private EntityManager entityManager;

    static {
        try {
            FILE_STORE_URN = FileStoreUrn.create("42");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        datasource = new PGSimpleDataSource();
        datasource.setDatabaseName(DATABASE_NAME);
        datasource.setServerName("localhost");
        datasource.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port")));
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
    }

    @BeforeClass
    public static void createDb() {
        final StartupDBMigrator dbMigrator = new StartupDBMigrator();
        dbMigrator.dataSource = datasource;
        dbMigrator.onStartup();
    }

    @Before
    public void initialiseEntityManager() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, System.getProperty("user.name"));
        properties.put(JDBC_PASSWORD, System.getProperty("user.name"));
        properties.put(JDBC_URL, String.format("jdbc:postgresql://localhost:%s/jobstore", System.getProperty("postgresql.port")));
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");

        final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("jobstoreIT", properties);
        entityManager = entityManagerFactory.createEntityManager(properties);
    }

    @After
    public void clearJobStore() throws SQLException {
        if( entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();

        try (final Connection connection = newConnection()) {
            for (String tableName : Arrays.asList(
                    JOB_TABLE_NAME, CHUNK_TABLE_NAME, ITEM_TABLE_NAME, FLOW_CACHE_TABLE_NAME, SINK_CACHE_TABLE_NAME)) {
                JDBCUtil.update(connection, String.format("DELETE FROM %s", tableName));
            }
            connection.commit();
        }
    }

    @Before
    public void clearJobStoreBefore() throws SQLException {
        clearJobStore();
    }

    @After
    public void clearEntityManagerCache() {
        entityManager.clear();
        entityManager.getEntityManagerFactory().getCache().evictAll();
    }


    /**
     * Given: a jobstore with empty flowcache
     * When : a flow is added
     * Then : the flow is inserted into the flowcache
     */
    @Test
    public void cacheFlow_addingNeverBeforeSeenFlow_isCached() throws JobStoreException, SQLException, JSONBException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        // When...
        final Flow flow = new FlowBuilder().build();
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(pgJobStore.jsonbContext.marshall(flow));

        // Then...
        assertThat("entity", flowCacheEntity, is(notNullValue()));
        assertThat("table size", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(1L));
        assertThat("entity.flow.id", flowCacheEntity.getFlow().getId(), is(flow.getId()));
    }

    /**
     * Given: a jobstore with non-empty flowcache
     * When : a flow is added matching an already cached flow
     * Then : no new flow is inserted into the flowcache
     */
    @Test
    public void cacheFlow_addingAlreadyCachedFlow_leavesCacheUnchanged() throws JobStoreException, SQLException, JSONBException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final Flow flow = new FlowBuilder().build();
        final String flowJson = pgJobStore.jsonbContext.marshall(flow);
        final FlowCacheEntity existingFlowCacheEntity = pgJobStore.cacheFlow(flowJson);

        initialiseEntityManager();
        clearEntityManagerCache();

        // When...
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(flowJson);

        // Then...
        assertThat("entity.id", flowCacheEntity.getId(), is(existingFlowCacheEntity.getId()));
        assertThat("entity.checksum", flowCacheEntity.getChecksum(), is(existingFlowCacheEntity.getChecksum()));
        assertThat("entity.flow", flowCacheEntity.getFlow(), is(existingFlowCacheEntity.getFlow()));
        assertThat("table size", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(1L));
    }

    @Test
    public void createJobEntity_trimsNonAcctestFlows() throws JobStoreException, SQLException {
        final PgJobStore pgJobStore = newPgJobStore();
        int nextRevision = 1;
        for (JobSpecification.Type type : JobSpecification.Type.values()) {
            if (type == JobSpecification.Type.ACCTEST)
                continue;

            // When adding jobs with non-ACCTEST type referencing flows which only differs on
            // their "next" components the flows are trimmed resulting in cache hits.

            final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(new JobInputStream(
                    new JobSpecificationBuilder()
                            .setType(type)
                            .setDataFile(FILE_STORE_URN.toString())
                            .build(), true, 0));
            final FlowContent flowContent = new FlowContentBuilder()
                    .setComponents(Collections.singletonList(
                            new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().setName("next_" + nextRevision++).build()).build()
                    ))
                    .build();
            final Flow flow = new FlowBuilder()
                    .setContent(flowContent)
                    .build();
            mockedAddJobParam.setFlow(flow);

            final EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            pgJobStore.createJobEntity(mockedAddJobParam);
            transaction.commit();

            assertThat("flow cache table size when flow is trimmed", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(1L));
        }

        // When adding job with type ACCTEST the flow is not trimmed resulting in cache insert.

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(new JobInputStream(
                new JobSpecificationBuilder()
                        .setType(JobSpecification.Type.ACCTEST)
                        .setDataFile(FILE_STORE_URN.toString())
                        .build(), true, 0));
        final FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Collections.singletonList(
                        new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().setName("next_" + nextRevision++).build()).build()
                ))
                .build();
        final Flow flow = new FlowBuilder()
                .setContent(flowContent)
                .build();
        mockedAddJobParam.setFlow(flow);

        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        pgJobStore.createJobEntity(mockedAddJobParam);
        transaction.commit();

        assertThat("flow cache table size when flow is not trimmed", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(2L));
    }

    /**
     * Given: a jobstore with empty sinkcache
     * When : a sink is added
     * Then : the sink is inserted into the sinkcache
     */
    @Test
    public void cacheSink_addingNeverBeforeSeenSink_isCached() throws JobStoreException, SQLException, JSONBException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        // When...
        final Sink sink = new SinkBuilder().build();
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(pgJobStore.jsonbContext.marshall(sink));

        // Then...
        assertThat("entity", sinkCacheEntity, is(notNullValue()));
        assertThat("table size", getSizeOfTable(SINK_CACHE_TABLE_NAME), is(1L));
        assertThat("entity.sink.id", sinkCacheEntity.getSink().getId(), is(sink.getId()));
    }

    /**
     * Given: a jobstore with non-empty sinkcache
     * When : a sink is added matching an already cached sink
     * Then : no new sink is inserted into the sinkcache
     */
    @Test
    public void cacheSink_addingAlreadyCachedSink_leavesCacheUnchanged() throws JobStoreException, SQLException, JSONBException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final Sink sink = new SinkBuilder().build();
        final String sinkJson = pgJobStore.jsonbContext.marshall(sink);
        final SinkCacheEntity existingSinkCacheEntity = pgJobStore.cacheSink(sinkJson);

        initialiseEntityManager();
        clearEntityManagerCache();

        // When...
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(sinkJson);

        // Then...
        assertThat("entity.id", sinkCacheEntity.getId(), is(existingSinkCacheEntity.getId()));
        assertThat("entity.checksum", sinkCacheEntity.getChecksum(), is(existingSinkCacheEntity.getChecksum()));
        assertThat("entity.sink", sinkCacheEntity.getSink(), is(existingSinkCacheEntity.getSink()));
        assertThat("table size", getSizeOfTable(SINK_CACHE_TABLE_NAME), is(1L));
    }

    /**
     * Given: an empty jobstore
     * When : calling addAndScheduleJob() with mocked returns from flow store where compareByteSize() returns the expected byte size
     * Then : a new job entity and the required number of chunk and item entities are created
     * And  : a flow cache entity is created
     * And  : a sink cache entity is created
     * And  : no diagnostics were created while adding job
     * And  : the returned JobInfoSnapshot holds the expected values
     */
    @Test
    public void addAndScheduleJob_identicalByteSize_returnsJobInfoSnapShot() throws JobStoreException, SQLException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();

        // Setup mocks
        setupSuccessfulMockedReturnsFromFlowStore(mockedAddJobParam);
        setupSuccessfulMockedReturnsFromFileStore(mockedAddJobParam);

        // Set up mocked return for identical byte sizes
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) MockedAddJobParam.XML.getBytes(StandardCharsets.UTF_8).length);

        // When...
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addAndScheduleJob(mockedAddJobParam.getJobInputStream());
        jobTransaction.commit();

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, Collections.singletonList(State.Phase.PARTITIONING));

        // And...
        assertThat("JobEntity: cached flow", jobEntity.getCachedFlow(), is(notNullValue()));

        // And...
        assertThat("JobEntity: cached sink", jobEntity.getCachedSink(), is(notNullValue()));

        // And...
        assertThat("jobEntity.State : diagnostics", jobEntity.getState().getDiagnostics().size(), is(0));
        assertThat("jobEntity.fatalError", jobEntity.hasFatalError(), is(false));

        // And ..
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.State.Diagnostics.size", jobInfoSnapshot.getState().getDiagnostics().size(), is(0));
        assertThat("JobInforSnapshot.fatalError", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
        assertThat("JobInfoSnapshot submitter reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SUBMITTER), is(notNullValue()));
        assertThat("JobInfoSnapshot flowbinder reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW_BINDER), is(notNullValue()));
        assertThat("JobInfoSnapshot flow reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW), is(notNullValue()));
        assertThat("JobInfoSnapshot sink reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK), is(notNullValue()));
    }

    /**
     * Given: an empty jobstore
     * When : calling addAndScheduleJob() with mocked returns from flow store where compareByteSize() returns an unexpected byte size
     * Then : a JobStoreException is thrown
     * And  : the jobStoreException contains the expected message
     * And  : no entities are created
     */
    @Test
    public void addAndScheduleJob_differentByteSize_fatalDiagnosticExists() throws SQLException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException, JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;
        final long fileStoreByteSize = 42;

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        final long dataPartitionerByteSize = MockedAddJobParam.XML.getBytes(StandardCharsets.UTF_8).length;

        // Setup mocks
        setupSuccessfulMockedReturnsFromFlowStore(mockedAddJobParam);
        setupSuccessfulMockedReturnsFromFileStore(mockedAddJobParam);

        // Set up mocked return for different byte sizes
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(fileStoreByteSize);

        // When...
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addAndScheduleJob(mockedAddJobParam.getJobInputStream());
        jobTransaction.commit();

        final JobInfoSnapshot jobInfoSnapshotAfterWait = waitForJobCompletion(jobInfoSnapshot.getJobId(), pgJobStore);
        assertTrue(!jobInfoSnapshotAfterWait.getState().getDiagnostics().isEmpty());
        final Diagnostic diagnostic = jobInfoSnapshotAfterWait.getState().getDiagnostics().get(0);
        assertThat("Diagnostics level", diagnostic.getLevel(), is(FATAL));
        assertThat("jobInfoSnapshot.fatalError", jobInfoSnapshot.hasFatalError(), is(true));

        // And...
        final String expectedStacktrace = "DataPartitioner.byteSize was: " + dataPartitionerByteSize + ". FileStore.byteSize was: " + fileStoreByteSize;
        assertThat(expectedStacktrace, diagnostic.getStacktrace().contains("Error reading data file"), is(true));

        // And...
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
    }

    private JobInfoSnapshot waitForJobCompletion(long jobId, PgJobStore pgJobStore) {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        // Wait for Job-completion
        long remainingWaitInMs = MAX_WAIT_IN_MS;

        LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - waiting in MAX milliseconds: " + MAX_WAIT_IN_MS);
        LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - sleeping in milliseconds: " + SLEEP_INTERVAL_IN_MS);


        while ( remainingWaitInMs > 0 ) {
            LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - remaining wait in milliseconds: " + remainingWaitInMs);

            jobInfoSnapshot = pgJobStore.listJobs(criteria).get(0);
            if (phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
                break;
            } else {
                try {
                    Thread.sleep(SLEEP_INTERVAL_IN_MS);
                    remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        if (!phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
            throw new IllegalStateException(String.format("Job %d did not complete successfully in time",
                    jobInfoSnapshot.getJobId()));
        }

        return jobInfoSnapshot;
    }

    private boolean phasePartitioningDoneSuccessfully(JobInfoSnapshot jobInfoSnapshot) {
        final State state = jobInfoSnapshot.getState();
        return state.phaseIsDone(State.Phase.PARTITIONING);
    }




    /**
     * Given: an empty jobstore
     * When : calling addAndScheduleJob() with mocked returns from flow store to secure that addJobParam.level.FATAL will is given as as input to addJob()
     * Then : a new job entity is created but chunks and item entities are not created
     * And  : a flow cache entity is not created
     * And  : a sink cache entity is not created
     * And  : time of completion is set on the job entity.
     * And  : a diagnostic with level FATAL is set on the state of the job entity
     * And  : The jobInfoSnapShot returned holds the expected values.
     */
    @Test
    public void addAndScheduleJob_fatalDiagnosticExists_returnsJobInfoSnapShot() throws JobStoreException, SQLException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 0;
        final int expectedNumberOfItems = 0;

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();

        // Setup mocks
        when(mockedFlowStoreServiceConnector.getFlow(anyLong())).thenReturn(mockedAddJobParam.getFlow());
        when(mockedFlowStoreServiceConnector.getSink(anyLong())).thenThrow(new FlowStoreServiceConnectorException("error"));
        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(anyLong())).thenReturn(mockedAddJobParam.getSubmitter());
        when(mockedFlowStoreServiceConnector.getFlowBinder(
                anyString(),
                anyString(),
                anyString(),
                anyLong(),
                anyString())).
                thenReturn(mockedAddJobParam.getFlowBinder());

        setupSuccessfulMockedReturnsFromFileStore(mockedAddJobParam);

        // When...
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addAndScheduleJob(mockedAddJobParam.getJobInputStream());
        jobTransaction.commit();

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, new ArrayList<State.Phase>());

        // And...
        assertThat("JobEntity: cached flow", jobEntity.getCachedFlow(), is(nullValue()));

        // And...
        assertThat("JobEntity: cached sink", jobEntity.getCachedSink(), is(nullValue()));

        // And ...
        assertThat("jobEntity.State.Diagnostics contains FATAL diagnostic", jobEntity.getState().fatalDiagnosticExists(), is(true));
        assertThat("jobEntity.fatalError", jobEntity.hasFatalError(), is(true));

        // And ..
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.State.Diagnostics.size", jobInfoSnapshot.getState().getDiagnostics().size(), is(1));
        assertThat("JobInfoSnapshot.State.Diagnostics fatal diagnostic", jobInfoSnapshot.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("JobInfoSnapshot.fatalError", jobInfoSnapshot.hasFatalError(), is(true));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
        assertThat("JobInfoSnapshot submitter reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SUBMITTER), is(notNullValue()));
        assertThat("JobInfoSnapshot flowbinder reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW_BINDER), is(notNullValue()));
        assertThat("JobInfoSnapshot flow reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW), is(notNullValue()));
        assertThat("JobInfoSnapshot sink reference", jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK), is(nullValue()));
    }

    /**
     * Given: an empty jobstore
     * When : adding a job with addJobParam.level.FATAL as input
     * Then : a new job entity is created but chunks and item entities are not created
     * And  : a flow cache entity is not created
     * And  : a sink cache entity is not created
     * And  : time of completion is set on the job entity.
     * And  : a diagnostic with level FATAL is set on the state of the job entity
     * And  : the returned JobInfoSnapshot holds the expected values
     */
    @Test
    public void addJobFatalDiagnosticLocated() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 0;
        final int expectedNumberOfItems = 0;

        // When...
        JobInfoSnapshot jobInfoSnapshot = addJobWithDiagnostic(pgJobStore, Diagnostic.Level.FATAL);

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, new ArrayList<State.Phase>());

        // And...
        assertThat("JobEntity: cached flow", jobEntity.getCachedFlow(), is(nullValue()));

        // And...
        assertThat("JobEntity: cached sink", jobEntity.getCachedSink(), is(nullValue()));

        // And ...
        assertThat("jobEntity.State.Diagnostics", jobEntity.getState().getDiagnostics().size(), is(1));
        assertThat("jobEntity.State.Diagnostics contains FATAL diagnostic", jobEntity.getState().fatalDiagnosticExists(), is(true));
        assertThat("jobEntity.fatalError", jobEntity.hasFatalError(), is(true));

        // And...
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.fatalError", jobInfoSnapshot.hasFatalError(), is(true));
        assertThat("JobInfoSnapshot.State.Diagnostics.size", jobInfoSnapshot.getState().getDiagnostics().size(), is(1));
        assertThat("JobInfoSnapshot.State.Diagnostics fatal diagnostic", jobInfoSnapshot.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }

    /**
     * Given: an empty jobstore
     * When : adding a job which fails immediately during partitioning
     * Then : a new job entity with a fatal diagnostic is created
     */
    @Test
    public void addJob_failFast() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final String iso8859 =
                "<?xml encoding=\"ISO-8859-1\""
                + "<records>"
                     + "<record>first</record>"
                + "</records>";
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(iso8859);

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        transaction.commit();

        // Then...
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.fatalError", jobInfoSnapshot.hasFatalError(), is(true));
        assertThat("JobInfoSnapshot.State.Diagnostics.size", jobInfoSnapshot.getState().getDiagnostics().size(), is(1));
        assertThat("JobInfoSnapshot.State.Diagnostics fatal diagnostic", jobInfoSnapshot.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }

    /**
     * Given: an empty jobstore
     * When : adding a job which fails eventually during partitioning
     * Then : a new job entity with a fatal diagnostic is created
     */
    @Test
    public void addJob_failEventually() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(getInvalidXml());

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        transaction.commit();

        // Then...
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.fatalError", jobInfoSnapshot.hasFatalError(), is(true));
        assertThat("JobInfoSnapshot.State.Diagnostics.size", jobInfoSnapshot.getState().getDiagnostics().size(), is(1));
        assertThat("JobInfoSnapshot.State.Diagnostics fatal diagnostic", jobInfoSnapshot.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }

    /**
     * Given: an empty jobstore
     * When : adding a job with addJobParam.level.WARNING as input, a job is added
     * Then : a new job entity and the required number of chunk and item entities are created
     * And  : a flow cache entity is created
     * And  : a sink cache entity is created
     * And  : a diagnostic with level WARNING is set on the state of the job entity
     * And  : the returned JobInfoSnapshot holds the expected values
     */
    @Test
    public void addJobWarningDiagnosticLocated() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        // When...
        setupExpectationOnGetByteSize();
        JobInfoSnapshot jobInfoSnapshot = addJobWithDiagnostic(pgJobStore, Diagnostic.Level.WARNING);

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, Collections.singletonList(State.Phase.PARTITIONING));

        // And...
        assertThat("JobEntity: cached flow", jobEntity.getCachedFlow(), is(notNullValue()));

        // And...
        assertThat("JobEntity: cached sink", jobEntity.getCachedSink(), is(notNullValue()));

        // And ...
        assertThat("jobEntity.State : diagnostics", jobEntity.getState().getDiagnostics().size(), is(1));
        assertThat(jobEntity.getState().fatalDiagnosticExists(), is(false));

        // And...
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.fatalError", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("JobInfoSnapshot.State.Diagnostics.size", jobInfoSnapshot.getState().getDiagnostics().size(), is(1));
        assertThat("JobInfoSnapshot.State.Diagnostics fatal diagnostic", jobInfoSnapshot.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.WARNING));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
    }

    /**
     * Given: an empty jobstore
     * When : adding a job with addJobParam.level.WARNING as input, a job is added
     * Then : a new job entity and the required number of chunk and item entities are created
     * And  : a flow cache entity is created
     * And  : a sink cache entity is created
     * And  : no diagnostics were created while adding job
     */
    @Test
    public void addJob() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        // When...
        setupExpectationOnGetByteSize();
        JobInfoSnapshot jobInfoSnapshot = addJobs(expectedNumberOfJobs, pgJobStore).get(0);

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, Collections.singletonList(State.Phase.PARTITIONING));

        // And...
        assertThat("JobEntity: cached flow", jobEntity.getCachedFlow(), is(notNullValue()));

        // And...
        assertThat("JobEntity: cached sink", jobEntity.getCachedSink(), is(notNullValue()));

        // And ...
        assertThat("jobEntity.State : diagnostics", jobEntity.getState().getDiagnostics().size(), is(0));
    }

    /**
     * Given: a job store where a job is added
     * When : an external chunk is added
     * Then : the job info snapshot is updated
     * And  : the referenced entities are updated
     */
    @Test
    public void addChunk() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 1;                   // second chunk is used, hence the chunk id is 1.
        final short itemId = 0;                  // The second chunk contains only one item, hence the item id is 0.

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshotNewJob = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshotNewJob, not(nullValue()));

        // Validate that nothing has been processed on job level
        assertThat(jobInfoSnapshotNewJob.getState().getPhase(PROCESSING).getSucceeded(), is(0));

        // Validate that nothing has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapshotNewJob.getJobId(), chunkId, 0, PROCESSING, false);

        // Validate that nothing has been processed on item level
        assertAndReturnItemState(jobInfoSnapshotNewJob.getJobId(), chunkId, itemId, 0, PROCESSING, false);

        ExternalChunk chunk = buildExternalChunk(
                jobInfoSnapshotNewJob.getJobId(),
                chunkId, 1,
                ExternalChunk.Type.PROCESSED,
                ChunkItem.Status.SUCCESS);

        // When...
        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        final JobInfoSnapshot jobInfoSnapShotUpdatedJob = pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // Then...
        assertThat(jobInfoSnapShotUpdatedJob, not(nullValue()));

        // Validate that one external chunk has been processed on job level
        assertThat(jobInfoSnapShotUpdatedJob.getState().getPhase(PROCESSING).getSucceeded(), is(1));
        LOGGER.info("new-job: {} updated-job: {}", jobInfoSnapshotNewJob.getTimeOfLastModification().getTime(), jobInfoSnapShotUpdatedJob.getTimeOfLastModification().getTime());
        assertThat(jobInfoSnapShotUpdatedJob.getTimeOfLastModification().after(jobInfoSnapshotNewJob.getTimeOfLastModification()), is(true));

        // And...

        // Validate that one external chunk has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, 1, PROCESSING, true);

        // Validate that one external chunk has been processed on item level
        assertAndReturnItemState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, itemId, 1, PROCESSING, true);
    }


    /**
     * Given: a job store where a job is added
     * When : an external chunk with Next processing Data is added
     * Then : the job info snapshot is updated
     * And  : the referenced entities are updated
     */
    @Test
    public void addChunkWithNextData() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 1;                   // second chunk is used, hence the chunk id is 1.
        final short itemId = 0;                  // The second chunk contains only one item, hence the item id is 0.

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshotNewJob = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshotNewJob, not(nullValue()));

        // Validate that nothing has been processed on job level
        assertThat(jobInfoSnapshotNewJob.getState().getPhase(PROCESSING).getSucceeded(), is(0));

        // Validate that nothing has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapshotNewJob.getJobId(), chunkId, 0, PROCESSING, false);

        // Validate that nothing has been processed on item level
        assertAndReturnItemState(jobInfoSnapshotNewJob.getJobId(), chunkId, itemId, 0, PROCESSING, false);

        ExternalChunk chunk = buildExternalChunkWithNextItems(
                jobInfoSnapshotNewJob.getJobId(),
                chunkId, 1,
                ExternalChunk.Type.PROCESSED,
                ChunkItem.Status.SUCCESS);

        // When...
        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        final JobInfoSnapshot jobInfoSnapShotUpdatedJob = pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // Then...
        assertThat(jobInfoSnapShotUpdatedJob, not(nullValue()));

        // Validate that one external chunk has been processed on job level
        assertThat(jobInfoSnapShotUpdatedJob.getState().getPhase(PROCESSING).getSucceeded(), is(1));
        LOGGER.info("new-job: {} updated-job: {}", jobInfoSnapshotNewJob.getTimeOfLastModification().getTime(), jobInfoSnapShotUpdatedJob.getTimeOfLastModification().getTime());
        assertThat(jobInfoSnapShotUpdatedJob.getTimeOfLastModification().after(jobInfoSnapshotNewJob.getTimeOfLastModification()), is(true));

        // And...

        // Validate that one external chunk has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, 1, PROCESSING, true);

        // Validate that one external chunk has been processed on item level
        assertAndReturnItemState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, itemId, 1, PROCESSING, true);

        clearEntityManagerCache();

        ExternalChunk fromDB = pgJobStore.getChunk(ExternalChunk.Type.PROCESSED, jobInfoSnapshotNewJob.getJobId(), chunkId);
        assertThat(fromDB.hasNextItems(), is(true));

        // extra checks for next items.
        assertThat(fromDB.getNext().size(), is(1));
        assertThat(fromDB.getItems().size(), is(1));

        ChunkItem chunkItem = fromDB.getItems().get(0);
        ChunkItem nextChunkItem = fromDB.getNext().get(0);

        assertThat("nextChunkItem.getData() NOT chunkItem.getData()", nextChunkItem.getData(), not(chunkItem.getData()));
        assertThat("nextChunkItem.getStatus() IS nextChunkItem.getStatus()", nextChunkItem.getStatus(), is(chunkItem.getStatus()));
    }


    /**
     * Given: a job store where a job is added
     * When : the same external chunk is added twice
     * Then : a DuplicateChunkException is thrown
     * And  : the DuplicateChunkException contains a JobError with Code.ILLEGAL_CHUNK
     * And  : job, chunk and item entities have not been updated after the second add.
     */
    @Test
    public void addChunkMultipleTimesMultipleItems() throws JobStoreException, FileStoreServiceConnectorException {
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;                   // first chunk is used, hence the chunk id is 0.
        final int numberOfItems = 10;

        setupExpectationOnGetByteSize();
        JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunk(
                jobInfoSnapshot.getJobId(),
                chunkId,
                numberOfItems,
                ExternalChunk.Type.PROCESSED,
                ChunkItem.Status.SUCCESS);

        // When...
        final EntityTransaction chunkTransaction = entityManager.getTransaction();

        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // Retrieve the entities for comparison
        final JobEntity jobEntityFirstAddChunk = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());

        final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobEntityFirstAddChunk.getId());
        final ChunkEntity chunkEntityFirstAddChunk = entityManager.find(ChunkEntity.class, chunkKey);

        final List<ItemEntity> itemEntities = new ArrayList<>(numberOfItems);
        for (int i = 0; i < numberOfItems; i++) {
            itemEntities.add(entityManager.find(ItemEntity.class, new ItemEntity.Key(jobEntityFirstAddChunk.getId(), chunkId, (short) i)));
        }

        try {
            chunkTransaction.begin();
            pgJobStore.addChunk(chunk);
            chunkTransaction.commit();

        // Then...
        } catch (DuplicateChunkException e) {

            // And...
            assertThat(e.getJobError().getCode(), is(JobError.Code.ILLEGAL_CHUNK));

            // And...
            final JobEntity jobEntitySecondAddChunk = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
            assertThat("JobEntity not updated", jobEntitySecondAddChunk.getState(), is(jobEntityFirstAddChunk.getState()));

            final ChunkEntity chunkEntitySecondAddChunk = entityManager.find(ChunkEntity.class, chunkKey);
            assertThat("ChunkEntity not updated", chunkEntitySecondAddChunk, is(chunkEntityFirstAddChunk));

            for(int i = 0; i < numberOfItems; i++) {
                    final ItemEntity itemEntitySecondAddChunk = entityManager.find(ItemEntity.class, new ItemEntity.Key(jobEntityFirstAddChunk.getId(), chunkId, (short) i));
                    assertThat("ItemEmtity not updated", itemEntitySecondAddChunk, is(itemEntities.get(i)));
            }
        }
    }

    /**
     * Given: a job store containing a number of jobs
     * When : requesting a job listing with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are returned
     */
    @Test
    public void listJobs() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        setupExpectationOnGetByteSize();
        final List<JobInfoSnapshot> snapshots = addJobs(4, pgJobStore);
        final List<JobInfoSnapshot> expectedSnapshots = snapshots.subList(1, snapshots.size() - 1);

        // When...
        final JsonObject jsonValue = Json.createObjectBuilder()
                .add("destination", snapshots.get(0).getSpecification().getDestination())
                .add("type", snapshots.get(0).getSpecification().getType().name())
                .build();

        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, jsonValue.toString()))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.GREATER_THAN, snapshots.get(0).getJobId()))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.LESS_THAN, snapshots.get(snapshots.size() - 1).getJobId()));

        final List<JobInfoSnapshot> returnedSnapshots = pgJobStore.listJobs(jobListCriteria);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(expectedSnapshots.size()));
        assertThat("First snapshot in result set", returnedSnapshots.get(0).getJobId(), is(expectedSnapshots.get(0).getJobId()));
        assertThat("Second snapshot in result set", returnedSnapshots.get(1).getJobId(), is(expectedSnapshots.get(1).getJobId()));
    }

    /**
     * Given: a job store containing a number of jobs
     * When : requesting a job count with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are counted and orderby/offset is ignored
     */
    @Test
    public void countJobs() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        setupExpectationOnGetByteSize();
        final List<JobInfoSnapshot> snapshots = addJobs(4, pgJobStore);

        // When...
        final JsonObject jsonValue = Json.createObjectBuilder()
                .add("destination", snapshots.get(0).getSpecification().getDestination())
                .add("type", snapshots.get(0).getSpecification().getType().name())
                .build();

        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, jsonValue.toString()))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.GREATER_THAN, snapshots.get(0).getJobId()))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.LESS_THAN, snapshots.get(snapshots.size() - 1).getJobId()))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.ASC))
                .limit(1).offset(12);

        final long count= pgJobStore.countJobs(jobListCriteria);

        // Then...
        assertThat( count, is(2L));
    }


    /**
     * Given    : a job store containing a number of jobs, where one has failed during delivering
     * When     : requesting a job listing with a criteria selecting only jobs failed in processing
     * Then     : no snapshots are returned.
     * And when : requesting a job listen with a criteria selecting only jobs failed in delivering
     * Then     : one filtered snapshot is returned
     */
    @Test
    public void listDeliveringFailedJobs() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;
        final short failedItemId = 3;

        final JobListCriteria jobListCriteriaDeliveringFailed = buildJobListCriteria(JobListCriteria.Field.STATE_DELIVERING_FAILED);
        final JobListCriteria jobListCriteriaProcessingFailed = buildJobListCriteria(JobListCriteria.Field.STATE_PROCESSING_FAILED);

        setupExpectationOnGetByteSize();
        final List<JobInfoSnapshot> snapshots = addJobs(3, pgJobStore);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, snapshots.get(0).getJobId(), chunkId, failedItemId, 6, ExternalChunk.Type.DELIVERED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final List<JobInfoSnapshot> returnedSnapshotsProcessingFailed = pgJobStore.listJobs(jobListCriteriaProcessingFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshotsProcessingFailed.size(), is(0));

        // And when...
        final List<JobInfoSnapshot> returnedSnapshotsDeliveringFailed = pgJobStore.listJobs(jobListCriteriaDeliveringFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshotsDeliveringFailed.size(), is(1));
        JobInfoSnapshot jobInfoSnapshot = returnedSnapshotsDeliveringFailed.get(0);
        assertThat(jobInfoSnapshot.getJobId(), is(snapshots.get(0).getJobId()));
    }

    /**
     * Given    : a job store containing a number of jobs, where one has failed during processing
     * When     : requesting a job listing with a criteria selecting only jobs failed in processing
     * Then     : one filtered snapshot is returned.
     * And when : requesting a job listen with a criteria selecting only jobs failed in delivering
     * Then     : no snapshots are returned.
     */
    @Test
    public void listProcessingFailedJobs() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;
        final short failedItemId = 3;

        final JobListCriteria jobListCriteriaDeliveringFailed = buildJobListCriteria(JobListCriteria.Field.STATE_DELIVERING_FAILED);
        final JobListCriteria jobListCriteriaProcessingFailed = buildJobListCriteria(JobListCriteria.Field.STATE_PROCESSING_FAILED);

        setupExpectationOnGetByteSize();
        final List<JobInfoSnapshot> snapshots = addJobs(3, pgJobStore);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, snapshots.get(0).getJobId(), chunkId, failedItemId, 6, ExternalChunk.Type.PROCESSED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final List<JobInfoSnapshot> returnedSnapshotsProcessingFailed = pgJobStore.listJobs(jobListCriteriaProcessingFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshotsProcessingFailed.size(), is(1));
        JobInfoSnapshot jobInfoSnapshot = returnedSnapshotsProcessingFailed.get(0);
        assertThat(jobInfoSnapshot.getJobId(), is(snapshots.get(0).getJobId()));

        // And when...
        final List<JobInfoSnapshot> returnedSnapshotsDeliveringFailed = pgJobStore.listJobs(jobListCriteriaDeliveringFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshotsDeliveringFailed.size(), is(0));
    }

    /**
     * Given    : a job store containing a number of jobs, where:
     *               - One job has failed during job creation
     *               - One job has been added without failure or diagnostics
     *               - One job has been added without failure but has a diagnostic with level WARNING
     *               - One job has failed during partitioning
     *
     * When     : requesting a job listing with a criteria selecting only jobs failed in job creation or partitioning
     * Then     : two filtered snapshot is returned.
     * And      : one snapshots is referring to the job that failed in partitioning and has been marked with fatal error.
     * And      : one snapshots is referring to the job that failed in job creation and has been marked with fatal error.
     */
    @Test
    public void listJobsWithFatalError() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        // Adding job that fails in job creation
        final JobInfoSnapshot jobFailedInJobCreation = addJobs(1, pgJobStore).get(0);

        // Setup mock to prevent instant fail in job creation
        setupExpectationOnGetByteSize();

        // Adding job without failure
        addJobs(1, pgJobStore);

        // Adding job with diagnostic with level WARNING
        addJobWithDiagnostic(pgJobStore, Diagnostic.Level.WARNING);

        // Adding job that fails in partitioning
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(getInvalidXml());
        final EntityTransaction transaction = entityManager.getTransaction();

        transaction.begin();
        final JobInfoSnapshot jobFailedInPartitioning = pgJobStore.addJob(mockedAddJobParam);
        transaction.commit();

        // When...
        final JobListCriteria jobListCriteriaJobCreationFailed = buildJobListCriteria(JobListCriteria.Field.WITH_FATAL_ERROR);
        final List<JobInfoSnapshot> returnedSnapshots = pgJobStore.listJobs(jobListCriteriaJobCreationFailed);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(2));

        // And...
        final JobInfoSnapshot returnedJobPartitioningFailedSnapshot = returnedSnapshots.get(0);
        assertThat("ReturnedJobPartitioningFailedSnapshot.jobId", returnedJobPartitioningFailedSnapshot.getJobId(), is(jobFailedInPartitioning.getJobId()));
        assertThat("ReturnedJobPartitioningFailedSnapshot.fatalError", returnedJobPartitioningFailedSnapshot.hasFatalError(), is(true));

        // And...
        final JobInfoSnapshot returnedJobCreationFailedSnapshot = returnedSnapshots.get(1);
        assertThat("ReturnedJobCreationFailedSnapshot.jobId", returnedJobCreationFailedSnapshot.getJobId(), is(jobFailedInJobCreation.getJobId()));
        assertThat("ReturnedJobCreationFailedSnapshot.fatalError", returnedJobCreationFailedSnapshot.hasFatalError(), is(true));
    }

    /**
     * Given: a job store containing a number of jobs
     * When : requesting an item count with a criteria selecting items from a specific job
     * Then : only the filtered snapshots from the specific job are counted and orderby/offset is ignored
     */
    @Test
    public void countItems() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        setupExpectationOnGetByteSize();
        final List<JobInfoSnapshot> snapshots = addJobs(2, pgJobStore);

        // When...
         final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, snapshots.get(0).getJobId()))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC))
                .limit(1).offset(6);

        final long count = pgJobStore.countItems(itemListCriteria);

        // Then...
        assertThat("item count returned", count, is(11L));
    }

    /**
     * Given: a job store containing 3 number of jobs all with reference to different sinks
     * When : requesting a job listing with a criteria selecting jobs with reference to a specific sink
     * Then : only one filtered snapshot is returned
     */
    @Test
    public void listJobsForSpecificSink() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int numberOfJobs = 3;
        List<JobInfoSnapshot> snapshots = new ArrayList<>(numberOfJobs);

        // Create 3 jobs, each containing params with reference to different sinks.
        for(int i = 0; i < numberOfJobs; i ++) {
            final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
            FlowStoreReference flowStoreReference = new FlowStoreReference(i + 1, i + 1, "sinkName" + (i + 1));
            mockedAddJobParam.setFlowStoreReferences(new FlowStoreReferencesBuilder()
                    .setFlowStoreReference(FlowStoreReferences.Elements.SINK, flowStoreReference).build());

            final EntityTransaction jobTransaction = entityManager.getTransaction();
            setupExpectationOnGetByteSize();
            jobTransaction.begin();
            snapshots.add(pgJobStore.addJob(mockedAddJobParam));
            jobTransaction.commit();
        }

        // When...
        final JobListCriteria jobListCriteria = new JobListCriteria().where(new ListFilter<>(
                JobListCriteria.Field.SINK_ID,
                ListFilter.Op.EQUAL,
                Long.valueOf(snapshots.get(1).getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId()).intValue()));

        final List<JobInfoSnapshot> returnedSnapshotsForSink = pgJobStore.listJobs(jobListCriteria);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshotsForSink.size(), is(1));
        JobInfoSnapshot jobInfoSnapshot = returnedSnapshotsForSink.get(0);
        assertThat("jobInfoSnapshot.flowStoreReferences.Element.Sink.id: " + jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId(),
                jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId(), is(2L));
        assertThat("jobInfoSnapshot.flowStoreReferences.Element.Sink.name ",
                jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getName(), is("sinkName2"));
        assertThat("jobInfoSnapshot.jobId: " + jobInfoSnapshot.getJobId(), jobInfoSnapshot.getJobId(), is(snapshots.get(1).getJobId()));
    }

    /**
     * Given   : a job store containing a number of jobs
     * When    : requesting an item listing with a criteria selecting failed items from a specific job
     * Then    : the expected filtered snapshot is returned, sorted by chunk id ASC > item id ASC
     */
    @Test
    public void listFailedItemsForJob() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short failedItemId = 3;          // The failed item will be the 4th out of 10

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), chunkId, failedItemId, 6, ExternalChunk.Type.PROCESSED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemListCriteria findAllItemsForJobWithStatusFailed = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobInfoSnapshot.getJobId()))
                .and(new ListFilter<>(ItemListCriteria.Field.STATE_FAILED));

        List<ItemInfoSnapshot> returnedItemInfoSnapshots = pgJobStore.listItems(findAllItemsForJobWithStatusFailed);
        assertThat("Number of returned snapshots", returnedItemInfoSnapshots.size(), is(1));
        assertThat("Job id referred to by item", returnedItemInfoSnapshots.get(0).getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat("Item id", returnedItemInfoSnapshots.get(0).getItemId(), is(failedItemId));
        assertThat("Item number", returnedItemInfoSnapshots.get(0).getItemNumber(), is(failedItemId + 1));
    }

    /**
     * Given   : a job store containing a number of jobs
     * When    : requesting an item listing with a criteria selecting ignored items from a specific job
     * Then    : the expected filtered snapshot is returned, sorted by chunk id ASC > item id ASC
     */
    @Test
    public void listIgnoredItemsForJob() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short failedItemId = 3;          // The failed item will be the 4th out of 10
        final short ignoredItemId = 4;         // The ignored item is the 5th out of 10

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), chunkId, failedItemId, ignoredItemId, ExternalChunk.Type.PROCESSED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemListCriteria findAllItemsForJobWithStatusIgnored = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobInfoSnapshot.getJobId()))
                .and(new ListFilter<>(ItemListCriteria.Field.STATE_IGNORED));

        List<ItemInfoSnapshot> returnedItemInfoSnapshots = pgJobStore.listItems(findAllItemsForJobWithStatusIgnored);
        assertThat("Number of returned snapshots", returnedItemInfoSnapshots.size(), is(1));
        assertThat("Job id referred to by item", returnedItemInfoSnapshots.get(0).getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat("Item id", returnedItemInfoSnapshots.get(0).getItemId(), is(ignoredItemId));
        assertThat("Item number", returnedItemInfoSnapshots.get(0).getItemNumber(), is(ignoredItemId + 1));
    }

    /**
     * Given   : a job store containing a number of jobs
     * When    : requesting an item listing with a criteria selecting all items from a specific job
     * Then    : the expected filtered snapshots are returned, sorted by chunk id ASC > item id ASC
     */
    @Test
    public void listAllItemsForJob() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short failedItemId = 3;          // The failed item will be the 4th out of 10
        final short ignoredItemId = 4;         // The ignored item is the 5th out of 10

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), chunkId, failedItemId, ignoredItemId, ExternalChunk.Type.PROCESSED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobInfoSnapshot.getJobId()))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));

        List<ItemInfoSnapshot> returnedItemInfoSnapshots = pgJobStore.listItems(itemListCriteria);

        // Then
        assertThat("Number of returned items", returnedItemInfoSnapshots.size(), is(jobInfoSnapshot.getNumberOfItems()));
        int expectedItemId = 1;
        for(ItemInfoSnapshot itemInfoSnapshot : returnedItemInfoSnapshots) {
            assertThat("Job id referred to by item", itemInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
            assertThat("Item number", itemInfoSnapshot.getItemNumber(), is(expectedItemId));
            expectedItemId++;
        }
    }

    /**
     * Given   : a job store containing job
     * When    : requesting a chunk collision detection element listing with a criteria selecting all chunks that has not finished
     * Then    : the expected filtered chunk collision detection elements are returned, sorted by creation time ASC
     */
    @Test
    public void listChunksCollisionDetectionElements() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        Timestamp timeOfCreation = new Timestamp(System.currentTimeMillis()); //timestamp older than creation time for any of the chunks.
        final PgJobStore pgJobStore = newPgJobStore();
        for(int i = 0; i < 4; i++) {
            final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
            final EntityTransaction transaction = entityManager.getTransaction();
            setupExpectationOnGetByteSize();
            transaction.begin();
            pgJobStore.addJob(mockedAddJobParam);
            transaction.commit();
        }

        // When...
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL))
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));

        // Then ...
        List<CollisionDetectionElement> returnedChunkCollisionDetectionElements = pgJobStore.listChunksCollisionDetectionElements(chunkListCriteria);
        assertThat(returnedChunkCollisionDetectionElements, not(nullValue()));
        assertThat(returnedChunkCollisionDetectionElements.size(), is(8));

        for(CollisionDetectionElement cde : returnedChunkCollisionDetectionElements) {
            final ChunkIdentifier chunkIdentifier = (ChunkIdentifier) cde.getIdentifier();
            ChunkEntity.Key chunkEntityKey = new ChunkEntity.Key(Long.valueOf(chunkIdentifier.getChunkId()).intValue(), Long.valueOf(chunkIdentifier.getJobId()).intValue());
            ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkEntityKey);

            assertThat("Time of completion is null", chunkEntity.getTimeOfCompletion(), is(nullValue())); // no end date
            assertThat(
                    "Previous collisionDetectionElement.timeOfCreation: {"
                            + timeOfCreation
                            + "} is before or equal to next collisionDetectionElement.timeOfCreation: {"
                            + chunkEntity.getTimeOfCreation() +"}.",
                    timeOfCreation.before(chunkEntity.getTimeOfCreation()) || timeOfCreation.equals(chunkEntity.getTimeOfCreation()), is(true)); // oldest first
            timeOfCreation = chunkEntity.getTimeOfCreation();
        }
    }


    /**
     * Given: a job store where a job exists
     * When : requesting a resource bundle for the existing job
     * Then : the resource bundle contains the correct flow, sink and supplementary process data
     */
    @Test
    public void getResourceBundle() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();

        final EntityTransaction jobTransaction = entityManager.getTransaction();

        setupExpectationOnGetByteSize();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        jobTransaction.commit();

        assertThat(jobInfoSnapshot, not(nullValue()));

        // When...
        ResourceBundle resourceBundle = pgJobStore.getResourceBundle(jobInfoSnapshot.getJobId());

        // Then...
        assertThat("ResourceBundle", resourceBundle, not(nullValue()));
        assertThat("ResourceBundle.flow", resourceBundle.getFlow(), not(nullValue()));
        assertThat(resourceBundle.getFlow(), is(mockedAddJobParam.getFlow()));

        assertThat("ResourceBundle.sink", resourceBundle.getSink(), not(nullValue()));
        assertThat(resourceBundle.getSink(), is(mockedAddJobParam.getSink()));

        assertThat("ResourceBundle.supplementaryProcessData", resourceBundle.getSupplementaryProcessData(), not(nullValue()));
        assertThat(resourceBundle.getSupplementaryProcessData().getSubmitter(), is(mockedAddJobParam.getJobInputStream().getJobSpecification().getSubmitterId()));
        assertThat(resourceBundle.getSupplementaryProcessData().getFormat(), is(mockedAddJobParam.getJobInputStream().getJobSpecification().getFormat()));
    }

    /**
     * Given: a non-empty jobstore
     * Then : a chunks can be retrieved
     */
    @Test
    public void getChunk() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        final EntityTransaction transaction = entityManager.getTransaction();

        setupExpectationOnGetByteSize();
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        transaction.commit();

        // Then...
        final ExternalChunk chunk0 = pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, jobInfoSnapshot.getJobId(), 0);
        assertThat("chunk0", chunk0, is(notNullValue()));
        assertThat("chunk0.size()", chunk0.size(), is(10));
        final ExternalChunk chunk1 = pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, jobInfoSnapshot.getJobId(), 1);
        assertThat("chunk1", chunk1, is(notNullValue()));
        assertThat("chunk1.size()", chunk1.size(), is(1));
    }

    /**
     * Given: a job store where a job exists and where:
     *          10 items have been successfully partitioned.
     * When : requesting item data for the existing job for phase: PARTITIONING
     * Then : the item data is returned and contains the the correct data.
     */
    @Test
    public void getItemDataPartitioned() throws JobStoreException, FileStoreServiceConnectorException {
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short itemId = 3;
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshot, not(nullValue()));

        final ItemEntity.Key itemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);

        // When...
        ItemData itemData = pgJobStore.getItemData(itemKey.getJobId(), itemKey.getChunkId(), itemKey.getId(), State.Phase.PARTITIONING);

        // Then...
        assertThat("itemData", itemData, not(nullValue()));
        assertThat("itemData.data", itemData.getData(), is(itemEntity.getPartitioningOutcome().getData()));
    }

    /**
     * Given: a job store where a job exists and where:
     *          10 items have been successfully partitioned.
     *          8 items have been successfully processed.
     *          1 item has failed in processing.
     *          1 item has been ignored in processing.
     *
     * When : requesting the data for the item failed in processing.
     * Then : the item data returned contains the the correct data.
     *
     * And when : requesting the data for one of the successful items.
     * Then : the item data returned contains the the correct data.
     *
     * And when : requesting the data for the item ignored in processing.
     * Then : the item data returned contains the the correct data.
     */
    @Test
    public void getItemDataProcessed() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short failedItemId = 3;          // The failed item will be the 4th out of 10
        final short ignoredItemId = 4;         // The ignored item is the 5th out of 10
        final short successfulItemId = 0;
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), chunkId, failedItemId, ignoredItemId, ExternalChunk.Type.PROCESSED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key failedItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, failedItemId);
        final ItemEntity failedItemEntity = entityManager.find(ItemEntity.class, failedItemKey);
        ItemData failedItemData = pgJobStore.getItemData(failedItemKey.getJobId(), failedItemKey.getChunkId(), failedItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("itemData", failedItemData, not(nullValue()));
        assertThat("itemData.data", failedItemData.getData(), is(failedItemEntity.getProcessingOutcome().getData()));

        // And when...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, successfulItemId);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ItemData successfulItemData = pgJobStore.getItemData(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("itemData", successfulItemEntity, not(nullValue()));
        assertThat("itemData.data", successfulItemData.getData(), is(successfulItemEntity.getProcessingOutcome().getData()));

        // And when...
        final ItemEntity.Key ignoredItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, ignoredItemId);
        final ItemEntity ignoredItemEntity = entityManager.find(ItemEntity.class, ignoredItemKey);
        ItemData ignoredItemData = pgJobStore.getItemData(ignoredItemKey.getJobId(), ignoredItemKey.getChunkId(), ignoredItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("itemData", ignoredItemEntity, not(nullValue()));
        assertThat("itemData.data", ignoredItemData.getData(), is(ignoredItemEntity.getProcessingOutcome().getData()));
    }

    /**
     * Given: a job store where a job exists and:
     *          10 items have been successfully partitioned.
     *          10 items have been successfully processed.
     *          8 items have been successfully delivered.
     *          1 item has failed in delivering.
     *          1 item has been ignored in delivering.
     *
     * When : requesting the data for the item failed in delivering.
     * Then : the item data returned contains the the correct data.
     *
     * And when : requesting the data for one of the successful items.
     * Then : the item data returned contains the the correct, data.
     *
     * And when : requesting the data for the item ignored in delivering.
     * Then : the item data returned contains the the correct data.
     */
    @Test
    public void getItemDataDelivered() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short failedItemId = 3;          // The failed item will be the 4th out of 10
        final short ignoredItemId = 4;         // The ignored item is the 5th out of 10
        final short successfulItemId = 0;
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize();
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk processedChunk = buildExternalChunk(jobInfoSnapshot.getJobId(), chunkId, 10, ExternalChunk.Type.PROCESSED, ChunkItem.Status.SUCCESS);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(processedChunk);
        chunkTransaction.commit();

        ExternalChunk deliveredChunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), chunkId, failedItemId, ignoredItemId, ExternalChunk.Type.DELIVERED);

        chunkTransaction.begin();
        pgJobStore.addChunk(deliveredChunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key failedItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, failedItemId);
        final ItemEntity failedItemEntity = entityManager.find(ItemEntity.class, failedItemKey);
        ItemData failedItemData = pgJobStore.getItemData(failedItemKey.getJobId(), failedItemKey.getChunkId(), failedItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("itemData", failedItemData, not(nullValue()));
        assertThat("itemData.data", failedItemData.getData(), is(failedItemEntity.getDeliveringOutcome().getData()));

        // When...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, successfulItemId);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ItemData successfulItemData = pgJobStore.getItemData(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("itemData", successfulItemData, not(nullValue()));
        assertThat("itemData.data", successfulItemData.getData(), is(successfulItemEntity.getDeliveringOutcome().getData()));

        // When...
        final ItemEntity.Key ignoredItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, ignoredItemId);
        final ItemEntity ignoredItemEntity = entityManager.find(ItemEntity.class, ignoredItemKey);
        ItemData ignoredItemData = pgJobStore.getItemData(ignoredItemKey.getJobId(), ignoredItemKey.getChunkId(), ignoredItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("itemData", ignoredItemData, not(nullValue()));
        assertThat("itemData.data", ignoredItemData.getData(), is(ignoredItemEntity.getDeliveringOutcome().getData()));
    }

    /**
     * Given: a job store containing a job
     *
     * When : requesting next processing outcome
     * Then : the next processing outcome returned contains the the correct data.
     */
    @Test
    public void getNextProcessingOutcome() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final int chunkId = 1;                  // second chunk is used, hence the chunk id is 1.
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize();

        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkWithNextItems(jobInfoSnapshot.getJobId(), chunkId, 1, ExternalChunk.Type.PROCESSED, ChunkItem.Status.SUCCESS);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, (short)0);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ChunkItem chunkItem = pgJobStore.getNextProcessingOutcome(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId());

        // Then...
        assertThat("chunkItem", chunkItem, not(nullValue()));
        assertThat("chunkItem.data", StringUtil.asString(chunkItem.getData()), is(StringUtil.asString(successfulItemEntity.getNextProcessingOutcome().getData())));
    }

    private void setupExpectationOnGetByteSize() throws FileStoreServiceConnectorException {
        Long byteSizeOfMockedData = (long) MockedAddJobParam.XML.getBytes(StandardCharsets.UTF_8).length;
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(byteSizeOfMockedData);
    }

    private PgJobStore newPgJobStore() {
        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.entityManager = entityManager;
        pgJobStore.sessionContext = SESSION_CONTEXT;
        pgJobStore.jobSchedulerBean = JOB_SCHEDULER_BEAN;
        pgJobStore.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
        pgJobStore.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;

        when(SESSION_CONTEXT.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);
        when(mockedFileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);
        when(mockedFlowStoreServiceConnectorBean.getConnector()).thenReturn(mockedFlowStoreServiceConnector);

        return pgJobStore;
    }

    private JobInfoSnapshot addJobWithDiagnostic(PgJobStore pgJobStore, Diagnostic.Level level) throws JobStoreException {
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        mockedAddJobParam.setDiagnostics(Collections.singletonList(new Diagnostic(level, ERROR_MESSAGE)));
        return commitJob(pgJobStore, mockedAddJobParam);
    }

    private List<JobInfoSnapshot> addJobs(int numberOfJobs, PgJobStore pgJobStore) throws JobStoreException {
        List<JobInfoSnapshot> snapshots = new ArrayList<>(numberOfJobs);
        for (int i = 0; i < numberOfJobs; i++) {

            final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
            JobInfoSnapshot jobInfoSnapshot = commitJob(pgJobStore, mockedAddJobParam);
            snapshots.add(jobInfoSnapshot);
        }
        return snapshots;
    }

    private JobInfoSnapshot commitJob(PgJobStore pgJobStore, MockedAddJobParam mockedAddJobParam) throws JobStoreException {
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        jobTransaction.commit();
        return jobInfoSnapshot;
    }

    private Connection newConnection() throws SQLException {
        final Connection connection = datasource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    private void setupSuccessfulMockedReturnsFromFlowStore(MockedAddJobParam mockedAddJobParam) throws FlowStoreServiceConnectorException {
        when(mockedFlowStoreServiceConnector.getFlow(anyLong())).thenReturn(mockedAddJobParam.getFlow());
        when(mockedFlowStoreServiceConnector.getSink(anyLong())).thenReturn(mockedAddJobParam.getSink());
        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(anyLong())).thenReturn(mockedAddJobParam.getSubmitter());
        when(mockedFlowStoreServiceConnector.getFlowBinder(
                anyString(),
                anyString(),
                anyString(),
                anyLong(),
                anyString())).
                thenReturn(mockedAddJobParam.getFlowBinder());
    }

    private void setupSuccessfulMockedReturnsFromFileStore(MockedAddJobParam mockedAddJobParam) throws FileStoreServiceConnectorException {
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(mockedAddJobParam.getDatafileInputStream());
    }

    private void assertTableSizes(int expectedJobTableSize, int expectedChunkTableSize, int expectedItemTableSize) throws SQLException {
        assertThat("Job table size", getSizeOfTable(JOB_TABLE_NAME), is((long) expectedJobTableSize));
        assertThat("Chunk table size", getSizeOfTable(CHUNK_TABLE_NAME), is((long) expectedChunkTableSize));
        assertThat("Item table size", getSizeOfTable(ITEM_TABLE_NAME), is((long) expectedItemTableSize));
    }

    private long getSizeOfTable(String tableName) throws SQLException {
        try (final Connection connection = newConnection()) {
            final List<List<Object>> rs = JDBCUtil.queryForRowLists(connection,
                    String.format("SELECT COUNT(*) FROM %s", tableName));
            return ((long) rs.get(0).get(0));
        }
    }

    private void assertEntities(JobEntity jobEntity, int expectedNumberOfChunks, int expectedNumberOfItems, List<State.Phase> phases) {
        assertJobEntity(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, phases);
        for (int chunkId = 0; chunkId < expectedNumberOfChunks; chunkId++) {
            final short expectedNumberOfChunkItems = expectedNumberOfItems / ((chunkId + 1) * MockedAddJobParam.MAX_CHUNK_SIZE) > 0 ? MockedAddJobParam.MAX_CHUNK_SIZE
                    : (short) (expectedNumberOfItems - (chunkId * MockedAddJobParam.MAX_CHUNK_SIZE));
            final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobEntity.getId());
            final ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkKey);
            assertChunkEntity(chunkEntity, chunkKey, expectedNumberOfChunkItems, phases);

            for (short itemId = 0; itemId < expectedNumberOfChunkItems; itemId++) {
                final ItemEntity.Key itemKey = new ItemEntity.Key(jobEntity.getId(), chunkId, itemId);
                final ItemEntity itemEntity = entityManager.find(ItemEntity.class, new ItemEntity.Key(jobEntity.getId(), chunkId, itemId));
                entityManager.refresh(itemEntity);
                assertItemEntity(itemEntity, itemKey, phases);
            }
        }
    }

    private void assertJobEntity(JobEntity jobEntity, int numberOfChunks, int numberOfItems, List<State.Phase> phasesDone) {
        final String jobLabel = String.format("JobEntity[%d]:", jobEntity.getId());
        assertThat(String.format("%s", jobLabel), jobEntity, is(notNullValue()));
        assertThat(String.format("%s number of chunks created", jobLabel), jobEntity.getNumberOfChunks(), is(numberOfChunks));
        assertThat(String.format("%s number of items created", jobLabel), jobEntity.getNumberOfItems(), is(numberOfItems));
        assertThat(String.format("%s time of creation", jobLabel), jobEntity.getTimeOfCreation(), is(notNullValue()));
        assertThat(String.format("%s time of last modification", jobLabel), jobEntity.getTimeOfLastModification(), is(notNullValue()));
        if(phasesDone.isEmpty()) {
            assertThat(String.format("%s time of completion", jobLabel), jobEntity.getTimeOfCompletion(), is(notNullValue()));
        }
        for (State.Phase phase : phasesDone) {
            assertThat(String.format("%s %s phase done", jobLabel, phase), jobEntity.getState().phaseIsDone(phase), is(true));
        }
    }

    private void assertChunkEntity(ChunkEntity chunkEntity, ChunkEntity.Key key, short numberOfItems, List<State.Phase> phasesDone) {
        final String chunkLabel = String.format("ChunkEntity[%d,%d]:", key.getJobId(), key.getId());
        assertThat(String.format("%s", chunkLabel), chunkEntity, is(notNullValue()));
        assertThat(String.format("%s number of items", chunkLabel), chunkEntity.getNumberOfItems(), is(numberOfItems));
        assertThat(String.format("%s time of creation", chunkLabel), chunkEntity.getTimeOfCreation(), is(notNullValue()));
        assertThat(String.format("%s time of last modification", chunkLabel), chunkEntity.getTimeOfLastModification(), is(notNullValue()));
        assertThat(String.format("%s sequence analysis data", chunkLabel), chunkEntity.getSequenceAnalysisData().getData().isEmpty(), is(not(true)));
        for (State.Phase phase : phasesDone) {
            assertThat(String.format("%s %s phase done", chunkLabel, phase), chunkEntity.getState().phaseIsDone(phase), is(true));
        }
    }

    private void assertItemEntity(ItemEntity itemEntity, ItemEntity.Key key, List<State.Phase> phasesDone) {
        final String itemLabel = String.format("ItemEntity[%d,%d,%d]:", key.getJobId(), key.getChunkId(), key.getId());
        assertThat(String.format("%s", itemLabel), itemEntity, is(notNullValue()));
        assertThat(String.format("%s time of creation", itemLabel), itemEntity.getTimeOfCreation(), is(notNullValue()));
        assertThat(String.format("%s time of last modification", itemLabel), itemEntity.getTimeOfLastModification(), is(notNullValue()));
        for (State.Phase phase : phasesDone) {
            assertThat(String.format("%s %s phase done", itemLabel, phase), itemEntity.getState().phaseIsDone(phase), is(true));
            switch (phase) {
                case PARTITIONING:
                    assertThat(String.format("%s %s data", itemLabel, phase), itemEntity.getPartitioningOutcome(), is(notNullValue()));
                    break;
                case PROCESSING:
                    assertThat(String.format("%s %s data", itemLabel, phase), itemEntity.getProcessingOutcome(), is(notNullValue()));
                    break;
                case DELIVERING:
                    assertThat(String.format("%s %S data", itemLabel, phase), itemEntity.getDeliveringOutcome(), is(notNullValue()));
                    break;
            }
        }
    }

    private JobListCriteria buildJobListCriteria(JobListCriteria.Field jobStateValue) {
        return new JobListCriteria()
                .where(new ListFilter<>(jobStateValue))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));
    }

    private ExternalChunk buildExternalChunk(long jobId, long chunkId, int numberOfItems, ExternalChunk.Type type, ChunkItem.Status status) {
        List<ChunkItem> items = new ArrayList<>();
        for(long i = 0; i < numberOfItems; i++) {
            items.add(new ChunkItemBuilder().setId(i).setData(StringUtil.asBytes(getData(type))).setStatus(status).build());
        }
        return new ExternalChunkBuilder(type).setJobId(jobId).setChunkId(chunkId).setItems(items).build();
    }


    private ExternalChunk buildExternalChunkWithNextItems(long jobId, long chunkId, int numberOfItems, ExternalChunk.Type type, ChunkItem.Status status) {
        List<ChunkItem> items = new ArrayList<>();
        List<ChunkItem> nextItems = new ArrayList<>();

        for(long i = 0; i < numberOfItems; i++) {
            items.add(new ChunkItemBuilder().setId(i).setData(StringUtil.asBytes(getData(type))).setStatus(status).build());
            nextItems.add(new ChunkItemBuilder().setId(i).setData(StringUtil.asBytes("next:" + getData(type))).setStatus(status).build());
        }
        return new ExternalChunkBuilder(type).setJobId(jobId).setChunkId(chunkId).setItems(items).setNextItems(nextItems).build();
    }

    private ExternalChunk buildExternalChunkContainingFailedAndIgnoredItem(int numberOfItems, long jobId, long chunkId, long failedItemId, long ignoredItemId, ExternalChunk.Type type) {
        List<ChunkItem> items = new ArrayList<>(numberOfItems);
        for(int i = 0; i < numberOfItems; i++) {
            if(i == failedItemId) {
                items.add(new ChunkItemBuilder().setId(i).setData(StringUtil.asBytes(getData(type))).setStatus(ChunkItem.Status.FAILURE).build());
            } else if( i == ignoredItemId) {
                items.add(new ChunkItemBuilder().setId(i).setData(StringUtil.asBytes(getData(type))).setStatus(ChunkItem.Status.IGNORE).build());
            } else {
                items.add(new ChunkItemBuilder().setId(i).setData(StringUtil.asBytes(getData(type))).setStatus(ChunkItem.Status.SUCCESS).build());
            }
        }
        return new ExternalChunkBuilder(type).setJobId(jobId).setChunkId(chunkId).setItems(items).build();
    }

    private String getData(ExternalChunk.Type type) {
        switch (type) {
            case PARTITIONED:
                return "partitioned test data";
            case PROCESSED:
                return "processed test data";
            default:
                return "delivered test data";
        }
    }

    private State assertAndReturnChunkState(int jobId, int chunkId, int succeeded, State.Phase phase, boolean isPhaseDone) {
        final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobId);
        final ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkKey);
        State chunkState = chunkEntity.getState();
        assertThat(chunkState.getPhase(phase).getSucceeded(), is(succeeded));
        assertThat(chunkState.phaseIsDone(phase), is(isPhaseDone));
        return chunkState;
    }

    private State assertAndReturnItemState(int jobId, int chunkId, short itemId, int succeeded, State.Phase phase, boolean isPhaseDone) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);
        State itemState = itemEntity.getState();
        assertThat(itemState.getPhase(phase).getSucceeded(), is(succeeded));
        assertThat(itemState.phaseIsDone(phase), is(isPhaseDone));
        if(isPhaseDone) {
            assertThat(itemEntity.getProcessingOutcome().getData(), is(StringUtil.base64encode(getData(ExternalChunk.Type.PROCESSED))));
        }
        return itemState;
    }

    private String getInvalidXml() {
        return "<records>"
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
                + "<record>eleventh"
                + "</records>";
    }

    private class MockedAddJobParam extends AddJobParam {
        public static final short MAX_CHUNK_SIZE = 10;
        public static final String UNDEFINED_DATA = "";
        public static final String XML =
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

        public MockedAddJobParam(JobInputStream jobInputStream, String utf8Data) {
            super(jobInputStream, mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
            submitter = new SubmitterBuilder().build();
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            flowBinder = new FlowBinderBuilder().build();
            flowStoreReferences = new FlowStoreReferencesBuilder().build();
            sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator(sink.getId());
            if (!utf8Data.equals(UNDEFINED_DATA)) {
                dataFileInputStream = new ByteArrayInputStream(utf8Data.getBytes(StandardCharsets.UTF_8));
                dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream,
                        StandardCharsets.UTF_8.name());
            }
        }

        public MockedAddJobParam(JobInputStream jobInputStream) {
            this(jobInputStream, UNDEFINED_DATA);
        }

        public MockedAddJobParam(String utf8Data) {
            this(new JobInputStream(
                    new JobSpecificationBuilder().setDataFile(FILE_STORE_URN.toString()).setCharset(StandardCharsets.UTF_8.name()).build(), true, 0
            ), utf8Data);
        }

        public MockedAddJobParam() {
            this(XML);
        }

        public InputStream getDatafileInputStream() {
            return this.dataFileInputStream;
        }

        public void setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
            this.flowStoreReferences = flowStoreReferences;
        }

        public void setDiagnostics(List<Diagnostic> diagnostics) {
            this.diagnostics = diagnostics;
        }

        public void setFlow(Flow flow) {
            this.flow = flow;
        }
    }
}
