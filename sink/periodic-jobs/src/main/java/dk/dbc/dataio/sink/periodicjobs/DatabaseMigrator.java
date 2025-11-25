package dk.dbc.dataio.sink.periodicjobs;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;

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
        PostgreSQLConfigurationExtension configurationExtension = config.getPluginRegister().getPlugin(PostgreSQLConfigurationExtension.class);
        configurationExtension.setTransactionalLock(false);
        Flyway flyway = config.load();
        flyway.migrate();
    }
}
