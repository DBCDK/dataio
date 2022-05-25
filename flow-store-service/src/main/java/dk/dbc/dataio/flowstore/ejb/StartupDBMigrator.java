package dk.dbc.dataio.flowstore.ejb;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

/**
 *
 * Startup Singleton for creating the database.
 */
@Singleton
@Startup
public class StartupDBMigrator {
    private static final Logger log = LoggerFactory.getLogger(StartupDBMigrator.class);

    @Resource(lookup="jdbc/flowStoreDb")
   	private DataSource dataSource;

   	@PostConstruct
   	public void onStartup() {
    	if (dataSource == null) {
   			log.error("no datasource found to execute the db migrations!");
            throw new EJBException("no datasource found to execute the db migrations!");
        }

   		final Flyway flyway = Flyway.configure()
				.table("schema_version_2")
				.baselineOnMigrate(true)
				.baselineVersion("1")
				.dataSource(dataSource)
				.load();
   		for (MigrationInfo i : flyway.info().all()) {
   			log.info("migrate task: " + i.getVersion() + " : " + i.getDescription() + " from file: " + i.getScript());
   		}
   		flyway.migrate();
   	}


	/**
	 * For Integration test only
	 * @param dataSource dataSource to use
	 * @return this
	 */
	public StartupDBMigrator withDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}
}
