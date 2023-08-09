package dk.dbc.dataio.harvester.rr;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.harvester.task.TaskRepoDatabaseMigrator;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.util.Map;

public abstract class IntegrationTest extends JpaIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
    public final DBCPostgreSQLContainer dbContainer = makeDBContainer();

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
                dbContainer.entityManagerProperties());
        return jpaTestEnvironment;
    }

    @After
    public void clearTables() {
        if (jpaTestEnvironment.getEntityManager().getTransaction().isActive()) {
            jpaTestEnvironment.getEntityManager().getTransaction().rollback();
        }
        jpaTestEnvironment.getEntityManager().getTransaction().begin();
        jpaTestEnvironment.getEntityManager().createNativeQuery("DELETE FROM task").executeUpdate();
        jpaTestEnvironment.getEntityManager().getTransaction().commit();
    }

    protected void persist(Object entity) {
        jpaTestEnvironment.getPersistenceContext().run(() ->
                jpaTestEnvironment.getEntityManager().persist(entity));
    }

    private DataSource getDataSource() {
        return dbContainer.datasource();
    }

    private Map<String, String> getEntityManagerFactoryProperties() {
        return dbContainer.entityManagerProperties();
    }

    private void migrateDatabase(DataSource datasource) {
        TaskRepoDatabaseMigrator dbMigrator = new TaskRepoDatabaseMigrator(datasource);
        dbMigrator.migrate();
    }



}
