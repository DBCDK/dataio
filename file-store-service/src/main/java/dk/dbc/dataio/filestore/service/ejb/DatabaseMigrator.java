package dk.dbc.dataio.filestore.service.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

@Singleton
@Startup
public class DatabaseMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DatabaseMigrator.class);

    @Resource(lookup = "jdbc/dataio/fileStore")
    DataSource dataSource;

    @PostConstruct
    public void onStartup() {
        FluentConfiguration config = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource);
        PostgreSQLConfigurationExtension configurationExtension = config.getPluginRegister().getPlugin(PostgreSQLConfigurationExtension.class);
        configurationExtension.setTransactionalLock(false);
        Flyway flyway = config.load();
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.info("db task {} : {} from file '{}'", i.getVersion(),
                    i.getDescription(), i.getScript());
        }
        flyway.migrate();
    }
}
