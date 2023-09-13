package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.mock;

public abstract class IntegrationTest extends JpaIntegrationTest implements PostgresContainerJPAUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    protected JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    protected FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    protected static Connection connectToPeriodicJobsDB() throws SQLException {
        return dbContainer.datasource().getConnection();
    }

    @Override
    public JpaTestEnvironment setup() {
        DataSource dataSource = dbContainer.datasource();
        migrateDatabase(dataSource);
        jpaTestEnvironment = new JpaTestEnvironment((PGSimpleDataSource) dataSource, "periodic-jobsIT_PU");
        return jpaTestEnvironment;
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = connectToPeriodicJobsDB();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM datablock");
            statement.executeUpdate("DELETE FROM delivery");
        }
    }

    @Before
    public void setTimeZone() {
        environmentVariables.set("TZ", "Europe/Copenhagen");
    }

    private void migrateDatabase(DataSource datasource) {
        final DatabaseMigrator databaseMigrator = new DatabaseMigrator(datasource);
        databaseMigrator.migrate();
    }
}
