/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.jpa.TransactionScopedPersistenceContext;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.ejb.DatabaseMigrator;
import dk.dbc.dataio.jobstore.service.ejb.JobQueueRepository;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.service.ejb.RerunsRepository;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.jvnet.mock_javamail.Mailbox;
import org.postgresql.ds.PGSimpleDataSource;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.mockito.Mockito.mock;

public class AbstractJobStoreIT {
    protected static String DATABASE_NAME = "testdb";
    protected static int DATABASE_PORT = 5432;
    protected static final String JOB_TABLE_NAME = "job";
    protected static final String CHUNK_TABLE_NAME = "chunk";
    protected static final String ITEM_TABLE_NAME = "item";
    protected static final String FLOW_CACHE_TABLE_NAME = "flowcache";
    protected static final String SINK_CACHE_TABLE_NAME = "sinkcache";
    protected static final String JOBQUEUE_TABLE_NAME = "jobqueue";
    protected static final String DEPENDENCYTRACKING_TABLE_NAME = "dependencyTracking";
    protected static final String NOTIFICATION_TABLE_NAME = "notification";
    protected static final String REORDERED_ITEM_TABLE_NAME = "reordereditem";
    protected static final String RERUN_TABLE_NAME = "rerun";

    private static final long SUBMITTERID = 123456;

    protected static final PGSimpleDataSource datasource;

    protected final FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    protected final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    protected final FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    protected final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    protected final SessionContext mockedSessionContext = mock(SessionContext.class);

    protected EntityManager entityManager;
    protected TransactionScopedPersistenceContext persistenceContext;
    protected JSONBContext jsonbContext = new JSONBContext();

    static {
        if (System.getProperty("postgresql.port") != null &&
                System.getProperty("postgresql.port").length() > 1) {
            DATABASE_PORT = Integer.parseInt(System.getProperty("postgresql.port"));
        } else {
            DATABASE_NAME=System.getProperty("user.name");
        }

        datasource = new PGSimpleDataSource();
        datasource.setDatabaseName( DATABASE_NAME );
        datasource.setServerName("localhost");
        datasource.setPortNumber( DATABASE_PORT );
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
    }

    @BeforeClass
    public static void createDb() {
        final DatabaseMigrator databaseMigrator = new DatabaseMigrator()
                .withDataSource(datasource);
        databaseMigrator.onStartup();
    }

    @Before
    public void initialiseEntityManager() {
        entityManager = JPATestUtils.getIntegrationTestEntityManager("jobstoreIT");

        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, System.getProperty("user.name"));
        properties.put(JDBC_PASSWORD, System.getProperty("user.name"));
        properties.put(JDBC_URL, String.format("jdbc:postgresql://localhost:%s/%s",DATABASE_PORT, DATABASE_NAME));
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");

        final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("jobstoreIT", properties);
        entityManager = entityManagerFactory.createEntityManager(properties);
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
    }

    @Before
    public void clearMailBoxes() {
        Mailbox.clearAll();
    }

    @Before
    public void clearJobStoreBefore() throws SQLException {
        clearJobStore();
    }

    @After
    public void clearJobStore() throws SQLException {
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();

        try (final Connection connection = newConnection()) {
            for (String tableName : Arrays.asList(
                    CHUNK_TABLE_NAME, ITEM_TABLE_NAME, JOBQUEUE_TABLE_NAME, NOTIFICATION_TABLE_NAME, RERUN_TABLE_NAME,
                    JOB_TABLE_NAME, FLOW_CACHE_TABLE_NAME, SINK_CACHE_TABLE_NAME, DEPENDENCYTRACKING_TABLE_NAME,
                    REORDERED_ITEM_TABLE_NAME)) {
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

    protected Connection newConnection() throws SQLException {
        final Connection connection = datasource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    protected void persist(Object entity) {
        persistenceContext.run(() ->
            entityManager.persist(entity));
    }

    protected JobEntity newJobEntity() {
        return newJobEntity(SUBMITTERID);
    }

    protected JobEntity newJobEntity(long submitterId) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(createJobSpecification(submitterId));
        jobEntity.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
        jobEntity.setState(new State());
        return jobEntity;
    }

    protected JobEntity newPersistedJobEntity() {
        return newPersistedJobEntity(SUBMITTERID);
    }

    protected JobEntity newPersistedJobEntity(long submitterId) {
        final JobEntity jobEntity = newJobEntity(submitterId);
        persist(jobEntity);
        return jobEntity;
    }

    protected ChunkEntity newChunkEntity(ChunkEntity.Key key) {
        final ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(key);
        chunkEntity.setState(new State());
        chunkEntity.setSequenceAnalysisData(new SequenceAnalysisData(Collections.<String>emptySet()));
        chunkEntity.setDataFileId("");
        return chunkEntity;
    }

    protected ChunkEntity newPersistedChunkEntity(ChunkEntity.Key key) {
        final ChunkEntity chunkEntity = newChunkEntity(key);
        persist(chunkEntity);
        return chunkEntity;
    }

    protected ItemEntity newItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(key);
        itemEntity.setState(new State());
        return itemEntity;
    }

    protected ItemEntity newPersistedItemEntity(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntity(key);
        persist(itemEntity);
        return itemEntity;
    }

    protected JobQueueEntity newJobQueueEntity(JobEntity job) {
        return new JobQueueEntity()
            .withJob(job)
            .withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter.XML)
            .withSinkId(0)
            .withState(JobQueueEntity.State.WAITING);
    }

    protected FlowCacheEntity newPersistedFlowCacheEntity() {
        final Flow flow = new FlowBuilder().build();
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        try {
            return pgJobStoreRepository.cacheFlow(jsonbContext.marshall(flow));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    protected SinkCacheEntity newPersistedSinkCacheEntity() {
        final Sink sink = new SinkBuilder().build();
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        try {
            return pgJobStoreRepository.cacheSink(jsonbContext.marshall(sink));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    protected SinkCacheEntity newPersistedSinkCacheEntity(Sink sink) {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreRepository();
        try {
            return pgJobStoreRepository.cacheSink(jsonbContext.marshall(sink));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    protected JobQueueEntity newPersistedJobQueueEntity(JobEntity job) {
        final JobQueueEntity jobQueueEntity = newJobQueueEntity(job);
        persist(jobQueueEntity);
        return jobQueueEntity;
    }

    protected DependencyTrackingEntity newDependencyTrackingEntity(DependencyTrackingEntity.Key key) {
        final DependencyTrackingEntity dependencyTrackingEntity = new DependencyTrackingEntity();
        dependencyTrackingEntity.setKey(key);
        dependencyTrackingEntity.setSinkid(1);
        dependencyTrackingEntity.setStatus(DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_PROCESSING);
        return dependencyTrackingEntity;
    }

    protected DependencyTrackingEntity newPersistedDependencyTrackingEntity(DependencyTrackingEntity.Key key) {
        final DependencyTrackingEntity dependencyTrackingEntity = newDependencyTrackingEntity(key);
        persist(dependencyTrackingEntity);
        return dependencyTrackingEntity;
    }

    protected JobQueueRepository newJobQueueRepository() {
        return new JobQueueRepository()
                .withEntityManager(entityManager);
    }

    protected JobSchedulerBean newJobSchedulerBean() {
        return new JobSchedulerBean()
                .withEntityManager(entityManager);
    }


    protected PgJobStoreRepository newPgJobStoreRepository() {
        return new PgJobStoreRepository()
                .withEntityManager(entityManager);
    }

    protected RerunsRepository newRerunsRepository() {
        return new RerunsRepository()
                .withEntityManager(entityManager);
    }

    protected RerunEntity newPersistedRerunEntity(JobEntity job) {
        final RerunEntity rerunEntity = newRerunEntity(job);
        persist(rerunEntity);
        return rerunEntity;
    }

    protected RerunEntity newRerunEntity(JobEntity job) {
        return new RerunEntity()
            .withJob(job)
            .withState(RerunEntity.State.WAITING);
    }

    protected List<ChunkEntity> findAllChunks() {
        final Query query = entityManager.createQuery("SELECT e FROM ChunkEntity e");
        return (List<ChunkEntity>) query.getResultList();
    }

    protected List<ItemEntity> findAllItems() {
        final Query query = entityManager.createQuery("SELECT e FROM ItemEntity e");
        return (List<ItemEntity>) query.getResultList();
    }

    // Why does this return long instead of int?
    protected long getSizeOfTable(String tableName) throws SQLException {
        try (final Connection connection = newConnection()) {
            final List<List<Object>> rs = JDBCUtil.queryForRowLists(connection,
                    String.format("SELECT COUNT(*) FROM %s", tableName));
            return ((long) rs.get(0).get(0));
        }
    }

    protected JobSpecification createJobSpecification() {
        return createJobSpecification(SUBMITTERID);
    }

    protected JobSpecification createJobSpecification(long submitterId) {
        final FileStoreUrn fileStoreUrn = FileStoreUrn.create("42");
        return new JobSpecification()
                .withPackaging("packaging")
                .withFormat("format")
                .withCharset("utf8")
                .withDestination("destinaion")
                .withSubmitterId(submitterId)
                .withMailForNotificationAboutVerification(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION)
                .withMailForNotificationAboutProcessing(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING)
                .withResultmailInitials(JobSpecification.EMPTY_RESULT_MAIL_INITIALS)
                .withDataFile(fileStoreUrn.toString())
                .withType(JobSpecification.Type.TEST);
    }
}
