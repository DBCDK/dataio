package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.ticklerepo.TickleRepoDatabaseMigrator;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
public abstract class IntegrationTest implements PostgresContainerJPAUtils {
    protected static EntityManagerFactory entityManagerFactory;

    private static DataSource datasource = dbContainer.datasource();

    @BeforeClass
    public static void migrateDatabase() {
        TickleRepoDatabaseMigrator dbMigrator = new TickleRepoDatabaseMigrator(datasource);
        dbMigrator.migrate();
    }

    @BeforeClass
    public static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory("tickleRepoIT",
                dbContainer.entityManagerProperties());
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
        try (Connection conn = datasource.getConnection()) {
            JDBCUtil.executeScript(conn, scriptFile, StandardCharsets.UTF_8.name());
        } catch (SQLException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = datasource.getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM record");
            statement.executeUpdate("DELETE FROM batch");
            statement.executeUpdate("DELETE FROM dataset");
            statement.executeUpdate("ALTER SEQUENCE record_id_seq RESTART");
            statement.executeUpdate("ALTER SEQUENCE batch_id_seq RESTART");
            statement.executeUpdate("ALTER SEQUENCE dataset_id_seq RESTART");
        }
    }

//    @Before
//    public void createEntityManager() {
//        entityManager = entityManagerFactory.createEntityManager(dbContainer.entityManagerProperties());
//        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
//    }

    @Before
    public void clearEntityManagerCache() {
        entityManagerFactory.getCache().evictAll();
    }
}
