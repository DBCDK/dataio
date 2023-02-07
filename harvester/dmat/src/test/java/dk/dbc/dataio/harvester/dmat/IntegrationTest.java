package dk.dbc.dataio.harvester.dmat;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

public abstract class IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

    static final DBCPostgreSQLContainer tickleRepoDBContainer;
    protected static EntityManager entityManager;
    protected static TransactionScopedPersistenceContext persistenceContext;

    private static boolean setupDone;

    static {
        tickleRepoDBContainer = new DBCPostgreSQLContainer().withReuse(false);
        tickleRepoDBContainer.start();
        tickleRepoDBContainer.exposeHostPort();

        LOGGER.info("Postgres url is:{}", tickleRepoDBContainer.getDockerJdbcUrl());
    }

    @BeforeAll
    public static void setUp() throws SQLException, IOException, URISyntaxException {
        if (!setupDone) {
            LOGGER.info("Doing various setup stuff");
            LOGGER.info("..Populating database for test");
            Connection connection = tickleRepoDBContainer.createConnection();
            LOGGER.info("..Connection created.");
            DataSource dataSource = getDataSource(tickleRepoDBContainer);

            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/dmat/db/tickle.sql"));
            entityManager = createEntityManager((PGSimpleDataSource) dataSource,
                    "tickleRepoITPU");
            persistenceContext = new TransactionScopedPersistenceContext(entityManager);
            LOGGER.info("..Populating database tables done");
            LOGGER.info("..Done");
            LOGGER.info("Setup done!");
            setupDone = true;
        } else {
            LOGGER.info("No setup stuff to do. Already done.");
        }
    }

    private static PGSimpleDataSource getDataSource(DBCPostgreSQLContainer tickleRepoDBContainer) {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName(tickleRepoDBContainer.getDatabaseName());
        datasource.setServerName(tickleRepoDBContainer.getContainerIpAddress());
        datasource.setPortNumber(tickleRepoDBContainer.getHostPort());
        datasource.setUser(tickleRepoDBContainer.getUsername());
        datasource.setPassword(tickleRepoDBContainer.getPassword());
        return datasource;
    }

    private static EntityManager createEntityManager(
            PGSimpleDataSource dataSource, String persistenceUnitName) {
        Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put(JDBC_USER, dataSource.getUser());
        entityManagerProperties.put(JDBC_PASSWORD, dataSource.getPassword());
        entityManagerProperties.put(JDBC_URL, dataSource.getUrl());
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put("eclipselink.logging.level", "FINE");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName,
                entityManagerProperties);
        return factory.createEntityManager(entityManagerProperties);
    }

    protected static void executeScript(Connection connection, URL script) throws IOException, SQLException, URISyntaxException {
        JDBCUtil.executeScript(connection, new File(script.toURI()), StandardCharsets.UTF_8.name());
    }
}
