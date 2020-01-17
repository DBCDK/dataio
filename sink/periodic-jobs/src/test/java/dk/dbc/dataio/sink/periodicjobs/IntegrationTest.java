/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class IntegrationTest extends JpaIntegrationTest {
    static final EmbeddedPostgres pg = pgStart();

    private static EmbeddedPostgres pgStart() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Connection connectToPeriodicJobsDB() {
        try {
            Class.forName("org.postgresql.Driver");
            final String dbUrl = String.format("jdbc:postgresql://localhost:%s/postgres", pg.getPort());
            final Connection connection = DriverManager.getConnection(dbUrl, "postgres", "");
            connection.setAutoCommit(true);
            return connection;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JpaTestEnvironment setup() {
        final DataSource dataSource = pg.getPostgresDatabase();
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

    private void migrateDatabase(DataSource datasource) {
        final DatabaseMigrator databaseMigrator = new DatabaseMigrator(datasource);
        databaseMigrator.migrate();
    }
}
