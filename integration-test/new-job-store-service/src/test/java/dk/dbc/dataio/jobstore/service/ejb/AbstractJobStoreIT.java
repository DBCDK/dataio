package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.mockito.Mockito.mock;

public class AbstractJobStoreIT {
    protected static final String DATABASE_NAME = "jobstore";
    protected static final String JOB_TABLE_NAME = "job";
    protected static final String CHUNK_TABLE_NAME = "chunk";
    protected static final String ITEM_TABLE_NAME = "item";
    protected static final String FLOW_CACHE_TABLE_NAME = "flowcache";
    protected static final String SINK_CACHE_TABLE_NAME = "sinkcache";
    protected static final String JOBQUEUE_TABLE_NAME = "jobqueue";
    protected static final String NOTIFICATION_TABLE_NAME = "notification";

    protected static final PGSimpleDataSource datasource;

    protected final FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    protected final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    protected final FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    protected final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    protected EntityManager entityManager;

    static {
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
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();

        try (final Connection connection = newConnection()) {
            for (String tableName : Arrays.asList(
                    JOB_TABLE_NAME, CHUNK_TABLE_NAME, ITEM_TABLE_NAME, FLOW_CACHE_TABLE_NAME, SINK_CACHE_TABLE_NAME,
                    JOBQUEUE_TABLE_NAME, NOTIFICATION_TABLE_NAME)) {
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

    protected Connection newConnection() throws SQLException {
        final Connection connection = datasource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }
}
