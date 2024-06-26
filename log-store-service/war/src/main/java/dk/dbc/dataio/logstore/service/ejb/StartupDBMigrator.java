package dk.dbc.dataio.logstore.service.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

@Singleton
@Startup
public class StartupDBMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupDBMigrator.class);

    @Resource(lookup = "jdbc/dataio/logstore")
    DataSource dataSource;

    @PostConstruct
    public void onStartup() {
        final Flyway flyway = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .load();
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.info("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();
    }
}
