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

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.persistence.MultiJpaIntegrationTest;
import dk.dbc.commons.persistence.MultiJpaTestEnvironment;
import dk.dbc.dataio.harvester.task.TaskRepoDatabaseMigrator;
import dk.dbc.ticklerepo.TickleRepoDatabaseMigrator;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

public abstract class IntegrationTest extends MultiJpaIntegrationTest {
    @Override
    public MultiJpaTestEnvironment setup() {
        final PGSimpleDataSource taskRepoDataSource = getTaskRepoDataSource();
        migrateTaskRepoDatabase(taskRepoDataSource);
        final PGSimpleDataSource tickleRepoDataSource = getTickleRepoDataSource();
        migrateTickleRepoDatabase(tickleRepoDataSource);
        this.environment = new MultiJpaTestEnvironment()
                .add("taskrepo", new JpaTestEnvironment(taskRepoDataSource, "taskrepoIT_PU",
                        getTaskRepoEntityManagerFactoryProperties(taskRepoDataSource)))
                .add("ticklerepo", new JpaTestEnvironment(tickleRepoDataSource, "tickleRepoIT",
                        getTickleRepoEntityManagerFactoryProperties(tickleRepoDataSource)));
        try (Connection conn = tickleRepoDataSource.getConnection();
                Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM record");
            statement.executeUpdate("DELETE FROM batch");
            statement.executeUpdate("DELETE FROM dataset");
            statement.executeUpdate("ALTER SEQUENCE record_id_seq RESTART WITH 1;");
            statement.executeUpdate("ALTER SEQUENCE batch_id_seq RESTART WITH 1;");
            statement.executeUpdate("ALTER SEQUENCE dataset_id_seq RESTART WITH 1;");
        } catch (SQLException e) {
        }
        executeScriptResource("ticklerepo", "/tickle-repo.sql");
        return environment;
    }
    
    @Before
    public void clearTaskRepo() {
        final JpaTestEnvironment taskEnvironment = environment.get("taskrepo");
        if (taskEnvironment.getEntityManager().getTransaction().isActive()) {
            taskEnvironment.getEntityManager().getTransaction().rollback();
        }
        taskEnvironment.getEntityManager().getTransaction().begin();
        taskEnvironment.getEntityManager().createNativeQuery("DELETE FROM task").executeUpdate();
        taskEnvironment.getEntityManager().getTransaction().commit();
    }

    private PGSimpleDataSource getTaskRepoDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("taskrepo");
        datasource.setServerName("localhost");
        datasource.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port", "5432")));
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
        return datasource;
    }

    private Map<String, String> getTaskRepoEntityManagerFactoryProperties(PGSimpleDataSource datasource) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, datasource.getUser());
        properties.put(JDBC_PASSWORD, datasource.getPassword());
        properties.put(JDBC_URL, datasource.getUrl());
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");
        return properties;
    }

    private PGSimpleDataSource getTickleRepoDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("ticklerepo");
        datasource.setServerName("localhost");
        datasource.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port", "5432")));
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
        return datasource;
    }

    private Map<String, String> getTickleRepoEntityManagerFactoryProperties(PGSimpleDataSource datasource) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, datasource.getUser());
        properties.put(JDBC_PASSWORD, datasource.getPassword());
        properties.put(JDBC_URL, datasource.getUrl());
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");
        return properties;
    }

    private void migrateTickleRepoDatabase(DataSource dataSource) {
        final TickleRepoDatabaseMigrator tickleRepoDatabaseMigrator = new TickleRepoDatabaseMigrator(dataSource);
        tickleRepoDatabaseMigrator.migrate();
    }

    private void migrateTaskRepoDatabase(DataSource dataSource) {
        final TaskRepoDatabaseMigrator taskRepoDatabaseMigrator = new TaskRepoDatabaseMigrator(dataSource);
        taskRepoDatabaseMigrator.migrate();
    }
}
