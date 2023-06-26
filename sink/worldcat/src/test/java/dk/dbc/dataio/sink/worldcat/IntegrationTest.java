package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.ocnrepo.OcnRepoDatabaseMigrator;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public abstract class IntegrationTest extends JpaIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
    public static final DBCPostgreSQLContainer dbContainer = makeDBContainer();

    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        LOGGER.info("Postgres url is:{}", container.getDockerJdbcUrl());
        return container;
    }

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
