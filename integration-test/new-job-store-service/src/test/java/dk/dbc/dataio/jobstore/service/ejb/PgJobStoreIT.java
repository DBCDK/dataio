package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
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
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStoreIT {
    public static final String DATABASE_NAME = "jobstore";
    public static final String JOB_TABLE_NAME = "job";
    public static final String CHUNK_TABLE_NAME = "chunk";
    public static final String ITEM_TABLE_NAME = "item";
    public static final String FLOW_CACHE_TABLE_NAME = "flowcache";
    public static final String SINK_CACHE_TABLE_NAME = "sinkcache";

    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreIT.class);
    private static final FileStoreUrn FILE_STORE_URN;
    private static final SessionContext SESSION_CONTEXT = mock(SessionContext.class);
    private static final JobSchedulerBean JOB_SCHEDULER_BEAN = mock(JobSchedulerBean.class);
    private static final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private static final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);

    private static final State.Phase PROCESSING = State.Phase.PROCESSING;
    private static final PGSimpleDataSource datasource;
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
    public void cacheFlow_addingNeverBeforeSeenFlow_isCached() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        // When...
        final Flow flow = new FlowBuilder().build();
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(flow);

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
    public void cacheFlow_addingAlreadyCachedFlow_leavesCacheUnchanged() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final Flow flow = new FlowBuilder().build();
        final FlowCacheEntity existingFlowCacheEntity = pgJobStore.cacheFlow(flow);

        initialiseEntityManager();
        clearEntityManagerCache();

        // When...
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(flow);

        // Then...
        assertThat("entity.id", flowCacheEntity.getId(), is(existingFlowCacheEntity.getId()));
        assertThat("entity.checksum", flowCacheEntity.getChecksum(), is(existingFlowCacheEntity.getChecksum()));
        assertThat("entity.flow", flowCacheEntity.getFlow(), is(existingFlowCacheEntity.getFlow()));
        assertThat("table size", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(1L));
    }

    /**
     * Given: a jobstore with empty sinkcache
     * When : a sink is added
     * Then : the sink is inserted into the sinkcache
     */
    @Test
    public void cacheSink_addingNeverBeforeSeenSink_isCached() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        // When...
        final Sink sink = new SinkBuilder().build();
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(sink);

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
    public void cacheSink_addingAlreadyCachedSink_leavesCacheUnchanged() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final Sink sink = new SinkBuilder().build();
        final SinkCacheEntity existingSinkCacheEntity = pgJobStore.cacheSink(sink);

        initialiseEntityManager();
        clearEntityManagerCache();

        // When...
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(sink);

        // Then...
        assertThat("entity.id", sinkCacheEntity.getId(), is(existingSinkCacheEntity.getId()));
        assertThat("entity.checksum", sinkCacheEntity.getChecksum(), is(existingSinkCacheEntity.getChecksum()));
        assertThat("entity.sink", sinkCacheEntity.getSink(), is(existingSinkCacheEntity.getSink()));
        assertThat("table size", getSizeOfTable(SINK_CACHE_TABLE_NAME), is(1L));
    }

    /**
     * Given: an empty jobstore
     * When : a job is added
     * Then : a new job entity is created
     * And  : a flow cache entity is created
     * And  : a sink cache entity is created
     * And  : the required number of chunk and item entities are created
     */
    @Test
    public void addJob() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(true);
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);

        transaction.commit();

        // Then...
        assertThat("Job table size", getSizeOfTable(JOB_TABLE_NAME), is(1L));
        assertThat("Chunk table size", getSizeOfTable(CHUNK_TABLE_NAME), is((long) expectedNumberOfChunks));
        assertThat("Item table size", getSizeOfTable(ITEM_TABLE_NAME), is((long) expectedNumberOfItems));

        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertJobEntity(jobEntity, expectedNumberOfChunks, expectedNumberOfItems,
                Arrays.asList(State.Phase.PARTITIONING));

        // And...
        assertThat("JobEntity: cached flow", jobEntity.getCachedFlow(), is(notNullValue()));

        // And...
        assertThat("JobEntity: cached sink", jobEntity.getCachedSink(), is(notNullValue()));

        // And...
        for (int chunkId = 0; chunkId < expectedNumberOfChunks; chunkId++) {
            final short expectedNumberOfChunkItems = expectedNumberOfItems / ((chunkId + 1) * mockedAddJobParam.MAX_CHUNK_SIZE) > 0 ? mockedAddJobParam.MAX_CHUNK_SIZE
                    : (short) (expectedNumberOfItems - (chunkId * mockedAddJobParam.MAX_CHUNK_SIZE));
            final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobEntity.getId());
            final ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkKey);
            assertChunkEntity(chunkEntity, chunkKey, expectedNumberOfChunkItems, Arrays.asList(State.Phase.PARTITIONING));

            for (short itemId = 0; itemId < expectedNumberOfChunkItems; itemId++) {
                final ItemEntity.Key itemKey = new ItemEntity.Key(jobEntity.getId(), chunkId, itemId);
                final ItemEntity itemEntity = entityManager.find(ItemEntity.class, new ItemEntity.Key(jobEntity.getId(), chunkId, itemId));
                entityManager.refresh(itemEntity);
                assertItemEntity(itemEntity, itemKey, Arrays.asList(State.Phase.PARTITIONING));
            }
        }
    }

    /**
     * Given: a job store where a job is added
     * When : an external chunk is added
     * Then : the job info snapshot is updated
     * And  : the referenced entities are updated
     */
    @Test
    public void addChunk() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 1;                   // second chunk is used, hence the chunk id is 1.
        final short itemId = 0;                  // The second chunk contains only one item, hence the item id is 0.

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
     * When : the same external chunk is added twice
     */
    @Ignore
    @Test
    public void addChunkMultipleTimesMultipleItems() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;                   // first chunk is used, hence the chunk id is 0.
        final int numberOfItems = 10;

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

        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();
    }

    /**
     * Given: a job store containing a number of jobs
     * When : requesting a job listing with a criteria selecting a subset of the jobs
     * Then : only the filtered snapshots are returned
     */
    @Test
    public void listJobs() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final List<JobInfoSnapshot> snapshots = addJobs(4, pgJobStore);
        final List<JobInfoSnapshot> expectedSnapshots = snapshots.subList(1, snapshots.size() - 1);

        // When...
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.GREATER_THAN, snapshots.get(0).getJobId()))
                .and(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.LESS_THAN, snapshots.get(snapshots.size() - 1).getJobId()));

        final List<JobInfoSnapshot> returnedSnapshots = pgJobStore.listJobs(jobListCriteria);

        // Then...
        assertThat("Number of returned snapshots", returnedSnapshots.size(), is(expectedSnapshots.size()));
        assertThat("First snapshot in result set", returnedSnapshots.get(0).getJobId(), is(expectedSnapshots.get(0).getJobId()));
        assertThat("Second snapshot in result set", returnedSnapshots.get(1).getJobId(), is(expectedSnapshots.get(1).getJobId()));
    }

    /**
     * Given    : a job store containing a number of jobs, where one has failed during delivering
     * When     : requesting a job listing with a criteria selecting only jobs failed in processing
     * Then     : no snapshots are returned.
     * And when : requesting a job listen with a criteria selecting only jobs failed in delivering
     * Then     : one filtered snapshot is returned
     */
    @Test
    public void listDeliveringFailedJobs() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int CHUNK_ID = 0;
        final short FAILED_ITEM_ID = 3;

        final JobListCriteria jobListCriteriaDeliveringFailed = buildJobListCriteria(JobListCriteria.Field.STATE_DELIVERING_FAILED);
        final JobListCriteria jobListCriteriaProcessingFailed = buildJobListCriteria(JobListCriteria.Field.STATE_PROCESSING_FAILED);

        final List<JobInfoSnapshot> snapshots = addJobs(3, pgJobStore);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, snapshots.get(0).getJobId(), CHUNK_ID, FAILED_ITEM_ID, 6, ExternalChunk.Type.DELIVERED);

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
    public void listProcessingFailedJobs() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int CHUNK_ID = 0;
        final short FAILED_ITEM_ID = 3;

        final JobListCriteria jobListCriteriaDeliveringFailed = buildJobListCriteria(JobListCriteria.Field.STATE_DELIVERING_FAILED);
        final JobListCriteria jobListCriteriaProcessingFailed = buildJobListCriteria(JobListCriteria.Field.STATE_PROCESSING_FAILED);

        final List<JobInfoSnapshot> snapshots = addJobs(3, pgJobStore);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, snapshots.get(0).getJobId(), CHUNK_ID, FAILED_ITEM_ID, 6, ExternalChunk.Type.PROCESSED);

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
     * Given: a job store containing 3 number of jobs all with reference to different sinks
     * When : requesting a job listing with a criteria selecting jobs with reference to a specific sink
     * Then : only one filtered snapshot is returned
     */
    @Test
    public void listJobsForSpecificSink() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int numberOfJobs = 3;
        List<JobInfoSnapshot> snapshots = new ArrayList<>(numberOfJobs);

        // Create 3 jobs, each containing params with reference to different sinks.
        for(int i = 0; i < numberOfJobs; i ++) {
            final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(true);
            FlowStoreReference flowStoreReference = new FlowStoreReference(i + 1, i + 1, "sinkName" + (i + 1));
            mockedAddJobParam.setFlowStoreReferences(new FlowStoreReferencesBuilder()
                    .setFlowStoreReference(FlowStoreReferences.Elements.SINK, flowStoreReference).build());

            final EntityTransaction jobTransaction = entityManager.getTransaction();
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
    public void listFailedItemsForJob() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int CHUNK_ID = 0;                  // first chunk is used, hence the chunk id is 0.
        final short FAILED_ITEM_ID = 3;          // The failed item will be the 4th out of 10

        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), CHUNK_ID, FAILED_ITEM_ID, 6, ExternalChunk.Type.PROCESSED);

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
        assertThat("Item id", returnedItemInfoSnapshots.get(0).getItemId(), is(FAILED_ITEM_ID));
        assertThat("Item number", returnedItemInfoSnapshots.get(0).getItemNumber(), is(FAILED_ITEM_ID + 1));
    }

    /**
     * Given   : a job store containing a number of jobs
     * When    : requesting an item listing with a criteria selecting ignored items from a specific job
     * Then    : the expected filtered snapshot is returned, sorted by chunk id ASC > item id ASC
     */
    @Test
    public void listIgnoredItemsForJob() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int CHUNK_ID = 0;                  // first chunk is used, hence the chunk id is 0.
        final short FAILED_ITEM_ID = 3;          // The failed item will be the 4th out of 10
        final short IGNORED_ITEM_ID = 4;         // The ignored item is the 5th out of 10

        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), CHUNK_ID, FAILED_ITEM_ID, IGNORED_ITEM_ID, ExternalChunk.Type.PROCESSED);

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
        assertThat("Item id", returnedItemInfoSnapshots.get(0).getItemId(), is(IGNORED_ITEM_ID));
        assertThat("Item number", returnedItemInfoSnapshots.get(0).getItemNumber(), is(IGNORED_ITEM_ID + 1));
    }

    /**
     * Given   : a job store containing a number of jobs
     * When    : requesting an item listing with a criteria selecting all items from a specific job
     * Then    : the expected filtered snapshots are returned, sorted by chunk id ASC > item id ASC
     */
    @Test
    public void listAllItemsForJob() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int CHUNK_ID = 0;                  // first chunk is used, hence the chunk id is 0.
        final short FAILED_ITEM_ID = 3;          // The failed item will be the 4th out of 10
        final short IGNORED_ITEM_ID = 4;         // The ignored item is the 5th out of 10

        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), CHUNK_ID, FAILED_ITEM_ID, IGNORED_ITEM_ID, ExternalChunk.Type.PROCESSED);

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
    public void listChunksCollisionDetectionElements() throws JobStoreException {
        // Given...
        Timestamp timeOfCreation = new Timestamp(System.currentTimeMillis()); //timestamp older than creation time for any of the chunks.
        final PgJobStore pgJobStore = newPgJobStore();
        for(int i = 0; i < 4; i++) {
            final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(false);
            final EntityTransaction transaction = entityManager.getTransaction();
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
    public void getResourceBundle() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(true);

        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();

        final JobInfoSnapshot jobInfoSnapshot =
                pgJobStore.addJob(mockedAddJobParam);

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
    public void getChunk() throws JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(true);
        final EntityTransaction transaction = entityManager.getTransaction();
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
    public void getItemDataPartitioned() throws JobStoreException {
        final int CHUNK_ID = 0;                  // first chunk is used, hence the chunk id is 0.
        final short ITEM_ID = 3;
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshot, not(nullValue()));

        final ItemEntity.Key itemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), CHUNK_ID, ITEM_ID);
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
    public void getItemDataProcessed() throws JobStoreException {
        // Given...
        final int CHUNK_ID = 0;                  // first chunk is used, hence the chunk id is 0.
        final short FAILED_ITEM_ID = 3;          // The failed item will be the 4th out of 10
        final short IGNORED_ITEM_ID = 4;         // The ignored item is the 5th out of 10
        final short SUCCESSFUL_ITEM_ID = 0;
        final PgJobStore pgJobStore = newPgJobStore();

        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk chunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), CHUNK_ID, FAILED_ITEM_ID, IGNORED_ITEM_ID, ExternalChunk.Type.PROCESSED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key failedItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), CHUNK_ID, FAILED_ITEM_ID);
        final ItemEntity failedItemEntity = entityManager.find(ItemEntity.class, failedItemKey);
        ItemData failedItemData = pgJobStore.getItemData(failedItemKey.getJobId(), failedItemKey.getChunkId(), failedItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("itemData", failedItemData, not(nullValue()));
        assertThat("itemData.data", failedItemData.getData(), is(failedItemEntity.getProcessingOutcome().getData()));

        // And when...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), CHUNK_ID, SUCCESSFUL_ITEM_ID);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ItemData successfulItemData = pgJobStore.getItemData(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("itemData", successfulItemEntity, not(nullValue()));
        assertThat("itemData.data", successfulItemData.getData(), is(successfulItemEntity.getProcessingOutcome().getData()));

        // And when...
        final ItemEntity.Key ignoredItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), CHUNK_ID, IGNORED_ITEM_ID);
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
    public void getItemDataDelivered() throws JobStoreException {
        // Given...
        final int CHUNK_ID = 0;                  // first chunk is used, hence the chunk id is 0.
        final short FAILED_ITEM_ID = 3;          // The failed item will be the 4th out of 10
        final short IGNORED_ITEM_ID = 4;         // The ignored item is the 5th out of 10
        final short SUCCESSFUL_ITEM_ID = 0;
        final PgJobStore pgJobStore = newPgJobStore();

        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        ExternalChunk processedChunk = buildExternalChunk(jobInfoSnapshot.getJobId(), CHUNK_ID, 10, ExternalChunk.Type.PROCESSED, ChunkItem.Status.SUCCESS);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(processedChunk);
        chunkTransaction.commit();

        ExternalChunk deliveredChunk = buildExternalChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), CHUNK_ID, FAILED_ITEM_ID, IGNORED_ITEM_ID, ExternalChunk.Type.DELIVERED);

        chunkTransaction.begin();
        pgJobStore.addChunk(deliveredChunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key failedItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), CHUNK_ID, FAILED_ITEM_ID);
        final ItemEntity failedItemEntity = entityManager.find(ItemEntity.class, failedItemKey);
        ItemData failedItemData = pgJobStore.getItemData(failedItemKey.getJobId(), failedItemKey.getChunkId(), failedItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("itemData", failedItemData, not(nullValue()));
        assertThat("itemData.data", failedItemData.getData(), is(failedItemEntity.getDeliveringOutcome().getData()));

        // When...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), CHUNK_ID, SUCCESSFUL_ITEM_ID);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ItemData successfulItemData = pgJobStore.getItemData(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("itemData", successfulItemData, not(nullValue()));
        assertThat("itemData.data", successfulItemData.getData(), is(successfulItemEntity.getDeliveringOutcome().getData()));

        // When...
        final ItemEntity.Key ignoredItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), CHUNK_ID, IGNORED_ITEM_ID);
        final ItemEntity ignoredItemEntity = entityManager.find(ItemEntity.class, ignoredItemKey);
        ItemData ignoredItemData = pgJobStore.getItemData(ignoredItemKey.getJobId(), ignoredItemKey.getChunkId(), ignoredItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("itemData", ignoredItemData, not(nullValue()));
        assertThat("itemData.data", ignoredItemData.getData(), is(ignoredItemEntity.getDeliveringOutcome().getData()));
    }


    private PgJobStore newPgJobStore() {
        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.entityManager = entityManager;
        pgJobStore.sessionContext = SESSION_CONTEXT;
        pgJobStore.jobSchedulerBean = JOB_SCHEDULER_BEAN;

        when(SESSION_CONTEXT.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);

        return pgJobStore;
    }

    private List<JobInfoSnapshot> addJobs(int numberOfJobs, PgJobStore pgJobStore) throws JobStoreException {
        List<JobInfoSnapshot> snapshots = new ArrayList<>(numberOfJobs);
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam(true);
        for (int i = 0; i < numberOfJobs; i++) {
            final EntityTransaction jobTransaction = entityManager.getTransaction();
            jobTransaction.begin();
            snapshots.add(pgJobStore.addJob(mockedAddJobParam));
            jobTransaction.commit();
        }
        return snapshots;
    }

    private Connection newConnection() throws SQLException {
        final Connection connection = datasource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    private long getSizeOfTable(String tableName) throws SQLException {
        try (final Connection connection = newConnection()) {
            final List<List<Object>> rs = JDBCUtil.queryForRowLists(connection,
                    String.format("SELECT COUNT(*) FROM %s", tableName));
            return ((long) rs.get(0).get(0));
        }
    }

    private void assertJobEntity(JobEntity jobEntity, int numberOfChunks, int numberOfItems, List<State.Phase> phasesDone) {
        final String jobLabel = String.format("JobEntity[%d]:", jobEntity.getId());
        assertThat(String.format("%s", jobLabel), jobEntity, is(notNullValue()));
        assertThat(String.format("%s number of chunks created", jobLabel), jobEntity.getNumberOfChunks(), is(numberOfChunks));
        assertThat(String.format("%s number of items created", jobLabel), jobEntity.getNumberOfItems(), is(numberOfItems));
        assertThat(String.format("%s time of creation", jobLabel), jobEntity.getTimeOfCreation(), is(notNullValue()));
        assertThat(String.format("%s time of last modification", jobLabel), jobEntity.getTimeOfLastModification(), is(notNullValue()));
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
            items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(status).build());
        }
        return new ExternalChunkBuilder(type).setJobId(jobId).setChunkId(chunkId).setItems(items).build();
    }

    private ExternalChunk buildExternalChunkContainingFailedAndIgnoredItem(int numberOfItems, long jobId, long chunkId, long failedItemId, long ignoredItemId, ExternalChunk.Type type) {
        List<ChunkItem> items = new ArrayList<>(numberOfItems);
        for(int i = 0; i < numberOfItems; i++) {
            if(i == failedItemId) {
                items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(ChunkItem.Status.FAILURE).build());
            } else if( i == ignoredItemId) {
                items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(ChunkItem.Status.IGNORE).build());
            } else {
                items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(ChunkItem.Status.SUCCESS).build());
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
            assertThat(itemEntity.getProcessingOutcome().getData(), is(getData(ExternalChunk.Type.PROCESSED)));
        }
        return itemState;
    }

    private static class MockedAddJobParam extends AddJobParam {
        public static final short MAX_CHUNK_SIZE = 10;
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

        public MockedAddJobParam(boolean isEOJ) {
            super(new JobInputStream(new JobSpecificationBuilder()
                    .setDataFile(FILE_STORE_URN.toString())
                    .build(), isEOJ, 0), mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            flowBinder = new FlowBinderBuilder().build();
            flowStoreReferences = new FlowStoreReferencesBuilder().build();
            sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
            dataFileInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream,
                    StandardCharsets.UTF_8.name());
        }

        public void setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
            this.flowStoreReferences = flowStoreReferences;
        }
    }
}
