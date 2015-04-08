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
 * Created by ja7 on 04-04-15.
 *
 * Startup Single ton for creating the database.
 */

@Singleton
@Startup
public class StartupDBMigrator {
    private static final Logger log = LoggerFactory.getLogger(StartupDBMigrator.class);

    @Resource(lookup="jdbc/flowStoreDb")
   	private DataSource dataSource;

   	@PostConstruct
   	public void onStartup() {
		log.error("ja7 in onStartup");
    	if (dataSource == null) {
   			log.error("no datasource found to execute the db migrations!");
            throw new EJBException(
                    "no datasource found to execute the db migrations!");
        }

   		Flyway flyway = new Flyway();

        flyway.setTable("flowstore_schema_version");
        flyway.setBaselineOnMigrate(true);
   		flyway.setDataSource(dataSource);
   		for (MigrationInfo i : flyway.info().all()) {
   			log.info("migrate task: " + i.getVersion() + " : " + i.getDescription() + " from file: " + i.getScript());
			log.error("migrate task: " + i.getVersion() + " : " + i.getDescription() + " from file: " + i.getScript());
   		}
   		flyway.migrate();
   	}

}
