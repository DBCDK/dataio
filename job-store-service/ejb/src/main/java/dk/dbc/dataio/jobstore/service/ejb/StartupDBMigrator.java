package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

@Singleton
@Startup
public class StartupDBMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupDBMigrator.class);

    @Resource(lookup = JndiConstants.JDBC_RESOURCE_JOBSTORE)
   	DataSource dataSource;

   	@PostConstruct
   	public void onStartup() {
   		final Flyway flyway = new Flyway();
        flyway.setTable("schema_version");
        flyway.setBaselineOnMigrate(true);
   		flyway.setDataSource(dataSource);
   		for (MigrationInfo i : flyway.info().all()) {
   			LOGGER.info("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
   		}
   		flyway.migrate();
   	}
}
