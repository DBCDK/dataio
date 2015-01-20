package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
    private static final SessionContext SESSION_CONTEXT = mock(SessionContext.class);
    private EntityManager entityManager;
//    private static final long CHUNK_ID = 1;
//    private static final short ITEM_ID = 0;
    private static final String DATA = "this is some test data";

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
        final Params params = new Params(true);
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(params.jobInputStream, params.dataPartitioner,
                params.sequenceAnalyserKeyGenerator, params.flow, params.sink);
        transaction.commit();

        // Then...
        assertThat("Job table size", getSizeOfTable(JOB_TABLE_NAME), is(1L));
        assertThat("Chunk table size", getSizeOfTable(CHUNK_TABLE_NAME), is((long) expectedNumberOfChunks));
        assertThat("Item table size", getSizeOfTable(ITEM_TABLE_NAME), is((long) expectedNumberOfItems));

        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertJobEntity(jobEntity, jobInfoSnapshot.getJobId(), expectedNumberOfChunks, expectedNumberOfItems,
                Arrays.asList(State.Phase.PARTITIONING));

        // And...
        assertThat("JobEntity: cached flow", jobEntity.getCachedFlow(), is(notNullValue()));

        // And...
        assertThat("JobEntity: cached sink", jobEntity.getCachedSink(), is(notNullValue()));

        // And...
        for (int chunkId = 0; chunkId < expectedNumberOfChunks; chunkId++) {
            final short expectedNumberOfChunkItems = expectedNumberOfItems / ((chunkId + 1) * params.maxChunkSize) > 0 ? params.maxChunkSize
                    : (short) (expectedNumberOfItems - (chunkId * params.maxChunkSize));
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
        final Params params = new Params(false); // The params define that the job is created with 2 chunks.
        final int chunkId = 1;                   // second chunk is used, hence the chunk id is 1.
        final short itemId = 0;                  // The second chunk contains only one item, hence the item id is 0.

        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();

        final JobInfoSnapshot jobInfoSnapshotNewJob = pgJobStore.addJob(params.jobInputStream, params.dataPartitioner,
                params.sequenceAnalyserKeyGenerator, params.flow, params.sink);
        jobTransaction.commit();

        assertThat(jobInfoSnapshotNewJob, not(nullValue()));

        // Validate that nothing has been processed on job level
        assertThat(jobInfoSnapshotNewJob.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(0));

        // Validate nothing has been processed on chunk level
        assertChunkState(jobInfoSnapshotNewJob.getJobId(), chunkId, 0, false);

        // Validate that nothing has been processed on item level
        assertItemState(jobInfoSnapshotNewJob.getJobId(), chunkId, itemId, 0, false);

        ExternalChunk chunk = buildExternalChunk(
                jobInfoSnapshotNewJob.getJobId(),
                chunkId, itemId,
                ExternalChunk.Type.PROCESSED,
                ChunkItem.Status.SUCCESS);


        // When...
        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        final JobInfoSnapshot jobInfoSnapShotUpdatedJob = pgJobStore.addChunk(chunk);
        jobTransaction.commit();

        // Then...
        assertThat(jobInfoSnapShotUpdatedJob, not(nullValue()));

        // Validate that one external chunk has been processed on job level
        assertThat(jobInfoSnapShotUpdatedJob.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(1));
        assertThat(jobInfoSnapShotUpdatedJob.getTimeOfLastModification().after(jobInfoSnapshotNewJob.getTimeOfLastModification()), is(true));

        // And...

        // Validate that one external chunk has been processed on chunk level
        assertChunkState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, 1, true);

        // Validate that one external chunk has been processed on item level
        assertItemState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, itemId, 1, true);
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

    @After
    public void clearEntityManagerCache() {
        entityManager.clear();
        entityManager.getEntityManagerFactory().getCache().evictAll();
    }

    private PgJobStore newPgJobStore() {
        final JSONBBean jsonbBean = new JSONBBean();
        jsonbBean.initialiseContext();
        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.entityManager = entityManager;
        pgJobStore.jsonbBean = jsonbBean;
        pgJobStore.sessionContext = SESSION_CONTEXT;

        when(SESSION_CONTEXT.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);

        return pgJobStore;
    }

    private Connection newConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        final String dbUrl = String.format("jdbc:postgresql://localhost:%s/%s",
                System.getProperty("postgresql.port"), DATABASE_NAME);
        final Connection connection = DriverManager.getConnection(dbUrl,
                System.getProperty("user.name"), System.getProperty("user.name"));
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

    private void assertJobEntity(JobEntity jobEntity, int jobId, int numberOfChunks, int numberOfItems, List<State.Phase> phasesDone) {
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

    private ExternalChunk buildExternalChunk(long jobId, long chunkId, long itemId, ExternalChunk.Type type, ChunkItem.Status status) {
        ExternalChunk chunk = new ExternalChunk(jobId, chunkId, type);
        chunk.insertItem(getChunkItem(itemId, status));
        return chunk;
    }

    private ChunkItem getChunkItem(long itemId, ChunkItem.Status status) {
        return new ChunkItem(itemId, DATA, status);
    }

    private void assertChunkState(int jobId, int chunkId, int succeeded, boolean isProcessingDone) {
        final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobId);
        final ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkKey);
        State chunkState = chunkEntity.getState();
        assertThat(chunkState.getPhase(State.Phase.PROCESSING).getSucceeded(), is(succeeded));
        assertThat(chunkState.phaseIsDone(State.Phase.PROCESSING), is(isProcessingDone));
    }

    private void assertItemState(int jobId, int chunkId, short itemId, int succeeded, boolean isProcessingDone) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);
        State itemState = itemEntity.getState();
        assertThat(itemState.getPhase(State.Phase.PROCESSING).getSucceeded(), is(succeeded));
        assertThat(itemState.phaseIsDone(State.Phase.PROCESSING), is(isProcessingDone));
        if(isProcessingDone) {
            assertThat(itemEntity.getProcessingOutcome().getData(), is(DATA));
        }
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
        public String dataFileId;
        public short maxChunkSize;

        public Params(boolean isEOJ) {
            jobInputStream = new JobInputStream(new JobSpecificationBuilder().build(), isEOJ, 0);
            dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
            maxChunkSize = 10;
            dataFileId = "datafile";
        }
    }
}
