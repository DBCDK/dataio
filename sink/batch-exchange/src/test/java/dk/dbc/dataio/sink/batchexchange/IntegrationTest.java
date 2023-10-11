package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.BatchExchangeDatabaseMigrator;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class IntegrationTest implements PostgresContainerJPAUtils {
    protected final EntityManager entityManager = JPAHelper.makeEntityManager("batchExchangeIT", dbContainer.entityManagerProperties());

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
