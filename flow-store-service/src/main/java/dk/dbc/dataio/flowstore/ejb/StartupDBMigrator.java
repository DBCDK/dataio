/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
