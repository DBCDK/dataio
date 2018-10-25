/*
 * DataIO - Data IO
 *
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

package dk.dbc.dataio.harvester.task;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TaskRepoIT extends JpaIntegrationTest {
    @Override
    public JpaTestEnvironment setup() {
        final PGSimpleDataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        jpaTestEnvironment = new JpaTestEnvironment(dataSource, "taskrepoIT_PU",
                getEntityManagerFactoryProperties(dataSource));
        return jpaTestEnvironment;
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = jpaTestEnvironment.getDatasource().getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM task");
        }
    }

    @Test
    public void findNextHarvestTask_whenNoneFound_returnsEmpty() {
        final Optional<HarvestTask> task = taskRepo().findNextHarvestTask(42);
        assertThat(task.isPresent(), is(false));
    }

    @Test
    public void findNextHarvestTask_whenFound_returnsTask() {
        executeScriptResource("/populate.sql");
        final HarvestTask task = taskRepo().findNextHarvestTask(42).orElse(null);
        assertThat("task found", task, is(notNullValue()));
        assertThat("task based on job", task.getBasedOnJob(), is(999999));
        final List<AddiMetaData> records = task.getRecords();
        assertThat("task records", records, is(notNullValue()));
        assertThat("task number of records", records.size(), is(1));
        assertThat("task record", records.get(0),
                is(newAddiMetaData()
                        .withSubmitterNumber(123456)
                        .withBibliographicRecordId("a")));
    }
    
    @Test
    public void selectorConversion() {
        executeScriptResource("/populate.sql");
        final HarvestTask task = taskRepo().findNextHarvestTask(43).orElse(null);
        assertThat("task found", task, is(notNullValue()));
        assertThat("task selector", task.getSelector(), is(new HarvestTaskSelector("key", "value")));
    }

    @Ignore
    @Test(timeout = 5000)
    public void concurrency() {
        executeScriptResource("/populate.sql");
        final TaskThread taskThread = new TaskThread();

        // task threads finds first task (bibliographicRecordId=a)
        taskThread.start();

        try {
            // do not proceed until task thread has entered its waiting state
            while (!taskThread.isWaiting()) {}

            final HarvestTask taskFromThread = taskThread.getTask();
            assertThat("thread task record", taskFromThread.getRecords().get(0),
                    is(newAddiMetaData()
                            .withSubmitterNumber(123456)
                            .withBibliographicRecordId("a")));

            // main thread finds second task (bibliographicRecordId=b)
            // since task thread still has a lock on the first task
            final HarvestTask task = taskRepo().findNextHarvestTask(42).orElse(null);
            assertThat("task record", task.getRecords().get(0),
                    is(newAddiMetaData()
                            .withSubmitterNumber(123456)
                            .withBibliographicRecordId("b")));
        } finally {
            taskThread.halt();
        }
    }

    private static class TaskThread extends Thread {
        private final JpaTestEnvironment jpaTestEnvironment;
        private HarvestTask task;
        private boolean halt = false;
        private boolean waiting = false;

        TaskThread() {
            final PGSimpleDataSource dataSource = getDataSource();
            jpaTestEnvironment = new JpaTestEnvironment(dataSource, "taskrepoIT_PU",
                getEntityManagerFactoryProperties(dataSource));
        }

        public void run() {
            jpaTestEnvironment.reset();
            task = null;
            final TaskRepo taskRepo = new TaskRepo(jpaTestEnvironment.getEntityManager());
            // lock next task in task repo and keep it until halted
            // by waiting inside the persistence context
            jpaTestEnvironment.getPersistenceContext().run(() -> {
                task = taskRepo.findNextHarvestTask(42).orElse(null);
                waitUntilHalted();
            });
        }

        HarvestTask getTask() {
            return task;
        }

        boolean isWaiting() {
            return waiting;
        }

        /* signal halt */
        synchronized void halt() {
            halt = true;
            notifyAll();
        }

        /* wait until halt is signalled */
        private synchronized void waitUntilHalted() {
            while (!halt) {
                try {
                    waiting = true;
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static PGSimpleDataSource getDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("taskrepo");
        datasource.setServerName("localhost");
        datasource.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port", "5432")));
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
        return datasource;
    }

    private static Map<String, String> getEntityManagerFactoryProperties(PGSimpleDataSource datasource) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, datasource.getUser());
        properties.put(JDBC_PASSWORD, datasource.getPassword());
        properties.put(JDBC_URL, datasource.getUrl());
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");
        return properties;
    }

    private void migrateDatabase(PGSimpleDataSource datasource) {
        final TaskRepoDatabaseMigrator dbMigrator = new TaskRepoDatabaseMigrator(datasource);
        dbMigrator.migrate();
    }

    private TaskRepo taskRepo() {
        return new TaskRepo(jpaTestEnvironment.getEntityManager());
    }

    private AddiMetaData newAddiMetaData() {
        return new AddiMetaData().withLibraryRules(new AddiMetaData.LibraryRules());
    }
}