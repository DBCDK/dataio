package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.persistence.MultiJpaIntegrationTest;
import dk.dbc.commons.persistence.MultiJpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.harvester.task.TaskRepoDatabaseMigrator;
import dk.dbc.ticklerepo.TickleRepoDatabaseMigrator;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

public abstract class IntegrationTest extends MultiJpaIntegrationTest implements PostgresContainerJPAUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

    @Override
    public MultiJpaTestEnvironment setup() {
        DataSource taskRepoDataSource = dbContainer.datasource();
        migrateTaskRepoDatabase(taskRepoDataSource);
        createTickleDB(taskRepoDataSource);
        DataSource tickleRepoDataSource = getTickleRepoDataSource();
        migrateTickleRepoDatabase(tickleRepoDataSource);
        this.environment = new MultiJpaTestEnvironment()
                .add("taskrepo", new JpaTestEnvironment(taskRepoDataSource, "taskrepoIT_PU",
                        dbContainer.entityManagerProperties()))
                .add("ticklerepo", new JpaTestEnvironment(tickleRepoDataSource, "tickleRepoIT",
                        getTickleRepoEntityManagerFactoryProperties()));
        try (Connection conn = tickleRepoDataSource.getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM record");
            statement.executeUpdate("DELETE FROM batch");
            statement.executeUpdate("DELETE FROM dataset");
            statement.executeUpdate("ALTER SEQUENCE record_id_seq RESTART WITH 1;");
            statement.executeUpdate("ALTER SEQUENCE batch_id_seq RESTART WITH 1;");
            statement.executeUpdate("ALTER SEQUENCE dataset_id_seq RESTART WITH 1;");
        } catch (SQLException e) {
        }
        executeScriptResource("ticklerepo", "/tickle-repo.sql");
        return environment;
    }

    @Before
    public void clearTaskRepo() {
        final JpaTestEnvironment taskEnvironment = environment.get("taskrepo");
        if (taskEnvironment.getEntityManager().getTransaction().isActive()) {
            taskEnvironment.getEntityManager().getTransaction().rollback();
        }
        taskEnvironment.getEntityManager().getTransaction().begin();
        taskEnvironment.getEntityManager().createNativeQuery("DELETE FROM task").executeUpdate();
        taskEnvironment.getEntityManager().getTransaction().commit();
    }

    private PGSimpleDataSource getTickleRepoDataSource() {
        PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("ticklerepo");
        datasource.setServerName("localhost");
        datasource.setPortNumber(dbContainer.getHostPort());
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
        return datasource;
    }

    private Map<String, String> getTickleRepoEntityManagerFactoryProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, dbContainer.getUsername());
        properties.put(JDBC_PASSWORD, dbContainer.getUsername());
        properties.put(JDBC_URL, String.format("jdbc:postgresql://localhost:%d/%s", dbContainer.getHostPort(),  "ticklerepo"));
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");
        return properties;
    }

    private void createTickleDB(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("CREATE DATABASE ticklerepo OWNER " + dbContainer.getUsername());
        } catch (SQLException e) {
            LOGGER.error("Error:", e);
        }
    }

    private void migrateTickleRepoDatabase(DataSource dataSource) {
        TickleRepoDatabaseMigrator tickleRepoDatabaseMigrator = new TickleRepoDatabaseMigrator(dataSource);
        tickleRepoDatabaseMigrator.migrate();
    }

    private void migrateTaskRepoDatabase(DataSource dataSource) {
        TaskRepoDatabaseMigrator taskRepoDatabaseMigrator = new TaskRepoDatabaseMigrator(dataSource);
        taskRepoDatabaseMigrator.migrate();
    }
}
