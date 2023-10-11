package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.ocnrepo.OcnRepoDatabaseMigrator;
import org.junit.Before;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public abstract class IntegrationTest extends JpaIntegrationTest implements PostgresContainerJPAUtils {
    @Override
    public JpaTestEnvironment setup() {
        DataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        System.setProperty("OCN_REPO_DB_URL", dbContainer.getJdbcUrl());
        jpaTestEnvironment = new JpaTestEnvironment(dataSource, "marcconvIT_PU", getEntityManagerFactoryProperties());
        return jpaTestEnvironment;
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = jpaTestEnvironment.getDatasource().getConnection(); Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM block");
        }
    }

    private DataSource getDataSource() {
        return dbContainer.datasource();
    }

    private Map<String, String> getEntityManagerFactoryProperties() {
        return dbContainer.entityManagerProperties();
    }

    void migrateDatabase(DataSource datasource) {
        OcnRepoDatabaseMigrator databaseMigrator = new OcnRepoDatabaseMigrator(datasource);
        databaseMigrator.migrate();
    }
}
