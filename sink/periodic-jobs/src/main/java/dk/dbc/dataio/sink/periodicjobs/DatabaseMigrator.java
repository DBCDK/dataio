package dk.dbc.dataio.sink.periodicjobs;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public class DatabaseMigrator {
    DataSource dataSource;

    public DatabaseMigrator() {
    }
    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        final Flyway flyway = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .load();
        flyway.migrate();
    }
}
