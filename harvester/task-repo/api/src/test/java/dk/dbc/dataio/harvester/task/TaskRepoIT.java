package dk.dbc.dataio.harvester.task;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TaskRepoIT extends JpaIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRepoIT.class);
    private static final DBCPostgreSQLContainer dbContainer = makeDBContainer();

    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        LOGGER.info("Postgres url is:{}", container.getDockerJdbcUrl());
        return container;
    }

    @Override
    public JpaTestEnvironment setup() {
        DataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        jpaTestEnvironment = new JpaTestEnvironment(dataSource, "taskrepoIT_PU",
                getEntityManagerFactoryProperties());
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
            while (!taskThread.isWaiting()) {
            }

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
            DataSource dataSource = getDataSource();
            jpaTestEnvironment = new JpaTestEnvironment(dataSource, "taskrepoIT_PU",
                    getEntityManagerFactoryProperties());
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

    private static DataSource getDataSource() {
        return dbContainer.datasource();
    }

    private static Map<String, String> getEntityManagerFactoryProperties() {
        return dbContainer.entityManagerProperties();
    }

    private void migrateDatabase(DataSource datasource) {
        TaskRepoDatabaseMigrator dbMigrator = new TaskRepoDatabaseMigrator(datasource);
        dbMigrator.migrate();
    }

    private TaskRepo taskRepo() {
        return new TaskRepo(jpaTestEnvironment.getEntityManager());
    }

    private AddiMetaData newAddiMetaData() {
        return new AddiMetaData().withLibraryRules(new AddiMetaData.LibraryRules());
    }
}
