package dk.dbc.dataio.jobstore.service.ejb;

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
public class DatabaseMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrator.class);

    @Resource(lookup = "jdbc/dataio/jobstore")
   	DataSource dataSource;

   	@PostConstruct
   	public void onStartup() {
   		final Flyway flyway = Flyway.configure()
				.table("schema_version_2")
				.baselineOnMigrate(true)
				.baselineVersion("1")
				.dataSource(dataSource)
				.load();
   		for (MigrationInfo i : flyway.info().all()) {
   			LOGGER.info("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
   		}
   		flyway.migrate();
   	}


	/**
	 * For Integration test only
	 * @param dataSource .
	 * @return this
	 */
	public DatabaseMigrator withDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}
}
