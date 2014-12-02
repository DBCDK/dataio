package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PgJobStoreIT {
    public static String DATABASE_NAME = "jobstore";
    public static String JOB_TABLE_NAME = "job";
    public static String CHUNK_TABLE_NAME = "chunk";
    public static String ITEM_TABLE_NAME = "item";
    public static String FLOW_CACHE_TABLE_NAME = "flowcache";
    public static String SINK_CACHE_TABLE_NAME = "sinkcache";
    private EntityManager entityManager;

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
     * Given: a jobstore without jobs
     * When : a job is added
     * Then : the job is persisted
     * And  : the auto generated fields are set in the resulting job entity
     * And  : more to come...
     */
    @Test
    public void addJob_newJobIsPersisted() throws JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final Flow flow = new FlowBuilder().build();
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(flow);
        final Sink sink = new SinkBuilder().build();
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(sink);

        // When...
        final JobEntity job = new JobEntity();
        job.setEoj(true);
        job.setSpecification(new JobSpecificationBuilder().build());
        job.setState(new State());
        job.setFlowName(flow.getContent().getName());
        job.setSinkName(sink.getContent().getName());
        job.setCachedFlow(flowCacheEntity);
        job.setCachedSink(sinkCacheEntity);

        entityManager.getTransaction().begin();
        final JobEntity addedJob = pgJobStore.addJob(job);
        entityManager.getTransaction().commit();

        // Then...
        assertThat("table size", getSizeOfTable(JOB_TABLE_NAME), is(1L));

        // And...
        assertThat("entity", addedJob, is(notNullValue()));
        assertThat("entity.id", addedJob.getId() > 0, is(true));
        assertThat("entity.timeOfCreation", addedJob.getTimeOfCreation(), is(notNullValue()));
        assertThat("entity.timeOfLastModification", addedJob.getTimeOfLastModification(), is(notNullValue()));
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
}
