package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.pgqueue.common.DatabaseMigrator;
import dk.dbc.pgqueue.common.QueueStorageAbstraction;
import dk.dbc.pgqueue.consumer.BasicHarvester;
import dk.dbc.pgqueue.consumer.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class BatchFetcherTest {
    private static final DBCPostgreSQLContainer PG = makePG();
    public static final QueueStorageAbstraction<String> STORAGE_ABSTRACTION = new QueueStorageAbstraction<>() {
        String[] COLUMNS = new String[]{"job"};

        @Override
        public String[] columnList() {
            return COLUMNS;
        }

        @Override
        public String createJob(ResultSet resultSet, int startColumn) throws SQLException {
            return resultSet.getString(startColumn);
        }

        @Override
        public void saveJob(String job, PreparedStatement stmt, int startColumn) throws SQLException {
            stmt.setString(startColumn, job);
        }
    };

    @BeforeEach
    public void setUp() throws Exception {
        try (Connection connection = PG.createConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP SCHEMA public CASCADE");
            stmt.executeUpdate("CREATE SCHEMA public");
            stmt.executeUpdate("CREATE TABLE queue ( job TEXT NOT NULL )");
            stmt.executeUpdate("CREATE TABLE queue_error ( job TEXT NOT NULL )");
        }
        DatabaseMigrator.migrate(PG.datasource());
    }

    @Test
    public void testBatchFetchAll() throws SQLException {
        String[] values = {"one", "two", "three"};
        queue("test", values);
        BasicHarvester<String> harvester = new BasicHarvester<>(Settings.defaults(List.of("consumer1"), STORAGE_ABSTRACTION), PG.datasource());
        BatchFetcher<String> fetcher = new BatchFetcherImpl<>("test", harvester);
        int count = fetcher.batch(5, list -> {
            Assertions.assertEquals(List.of(values), list);
        });
        Assertions.assertEquals(values.length, count, "We should have gotten all queued elements");
        Assertions.assertEquals(0, fetcher.batch(1, list -> {}), "There should be no remaining elements");
    }

    @Test
    public void testBatchFetchRollback() throws SQLException {
        String[] values = {"one", "two", "three"};
        queue("test", values);
        BasicHarvester<String> harvester = new BasicHarvester<>(Settings.defaults(List.of("consumer1"), STORAGE_ABSTRACTION), PG.datasource());
        BatchFetcher<String> fetcher = new BatchFetcherImpl<>("test", harvester);
        int count = fetcher.batch(5, list -> {
            throw new IllegalStateException("Something is dying dramatically");
        });
        Assertions.assertEquals(0, count, "We should have gotten no elements");
        Assertions.assertEquals(values.length, fetcher.batch(5, list -> {}), "All elements should be remaining");
    }

    private void queue(String queueName, String... jobs) throws SQLException {
        try (Connection connection = PG.createConnection();
             PreparedStatement stmt = connection.prepareStatement("INSERT INTO queue(consumer, job) VALUES(?, ?)")) {
            stmt.setString(1, queueName);
            for (String job : jobs) {
                stmt.setString(2, job);
                stmt.executeUpdate();
            }
        }
    }

    private static DBCPostgreSQLContainer makePG() {
        DBCPostgreSQLContainer pg = new DBCPostgreSQLContainer();
        pg.start();
        return pg;
    }
}
