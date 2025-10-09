package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.harvester.task.TaskRepoDatabaseMigrator;
import org.junit.After;

import javax.sql.DataSource;

public abstract class IntegrationTest extends JpaIntegrationTest implements PostgresContainerJPAUtils {

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

    private void migrateDatabase(DataSource datasource) {
        TaskRepoDatabaseMigrator dbMigrator = new TaskRepoDatabaseMigrator(datasource);
        dbMigrator.migrate();
    }



}
