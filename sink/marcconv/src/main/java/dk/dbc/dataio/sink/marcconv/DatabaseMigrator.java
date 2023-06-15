package dk.dbc.dataio.sink.marcconv;

import org.flywaydb.core.Flyway;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

public class DatabaseMigrator {
    private final DataSource dataSource;

    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrate() {
        final Flyway flyway = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .load();
        flyway.migrate();
    }
}
