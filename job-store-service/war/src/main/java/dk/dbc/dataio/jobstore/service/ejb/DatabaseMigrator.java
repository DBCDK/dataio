package dk.dbc.dataio.jobstore.service.ejb;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrator.class);

    @Resource(lookup = "jdbc/dataio/jobstore")
    DataSource dataSource;

    @PostConstruct
    public void init() {
        onStartup();
    }


    public DatabaseMigrator onStartup() {
        FluentConfiguration configure = Flyway.configure();
        final PostgreSQLConfigurationExtension configurationExtension = configure.getPluginRegister()
                .getPlugin(PostgreSQLConfigurationExtension.class);
        configurationExtension.setTransactionalLock(false);
        Flyway flyway = configure
                .table("schema_version_2")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .dataSource(dataSource)
                .load();
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.info("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();
        return this;
    }


    /**
     * For Integration test only
     *
     * @param dataSource .
     * @return this
     */
    public DatabaseMigrator withDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }
}
