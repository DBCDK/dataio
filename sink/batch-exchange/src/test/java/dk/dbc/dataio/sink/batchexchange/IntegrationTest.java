package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.BatchExchangeDatabaseMigrator;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
    public static final DBCPostgreSQLContainer dbContainer = makeDBContainer();
    protected final EntityManager entityManager = JPAHelper.makeEntityManager("batchExchangeIT", dbContainer.entityManagerProperties());

    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        LOGGER.info("Postgres url is:{}", container.getDockerJdbcUrl());
        return container;
    }

    @BeforeClass
    public static void migrateDatabase() {
        BatchExchangeDatabaseMigrator dbMigrator = new BatchExchangeDatabaseMigrator(dbContainer.datasource());
        dbMigrator.migrate();
    }

    protected static void executeScriptResource(String resourcePath) {
        URL resource = IntegrationTest.class.getResource(resourcePath);
        try {
            executeScript(new File(resource.toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static void executeScript(File scriptFile) {
        try (Connection conn = dbContainer.datasource().getConnection()) {
            JDBCUtil.executeScript(conn, scriptFile, StandardCharsets.UTF_8.name());
        } catch (SQLException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = dbContainer.createConnection(); Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM entry");
            statement.executeUpdate("DELETE FROM batch");
            statement.executeUpdate("ALTER SEQUENCE entry_id_seq RESTART");
            statement.executeUpdate("ALTER SEQUENCE batch_id_seq RESTART");
        }
    }

    @Before
    public void clearEntityManagerCache() {
        entityManager.clear();
        entityManager.getEntityManagerFactory().getCache().evictAll();
    }
}
