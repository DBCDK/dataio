package dk.dbc.dataio.harvester.rr;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.harvester.task.TaskRepoDatabaseMigrator;
import org.junit.After;
import org.postgresql.ds.PGSimpleDataSource;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

public abstract class IntegrationTest extends JpaIntegrationTest {
    @Override
    public JpaTestEnvironment setup() {
        final PGSimpleDataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        jpaTestEnvironment = new JpaTestEnvironment(dataSource, "taskrepoIT_PU",
                getEntityManagerFactoryProperties(dataSource));
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

    private PGSimpleDataSource getDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("taskrepo");
        datasource.setServerName("localhost");
        datasource.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port", "5432")));
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
        return datasource;
    }

    private Map<String, String> getEntityManagerFactoryProperties(PGSimpleDataSource datasource) {
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

}
