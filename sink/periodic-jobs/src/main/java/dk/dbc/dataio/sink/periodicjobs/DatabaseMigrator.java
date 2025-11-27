package dk.dbc.dataio.sink.periodicjobs;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import javax.sql.DataSource;

public class DatabaseMigrator {
    DataSource dataSource;

    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        FluentConfiguration config = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource);
        Flyway flyway = config.load();
        flyway.migrate();
    }
}
