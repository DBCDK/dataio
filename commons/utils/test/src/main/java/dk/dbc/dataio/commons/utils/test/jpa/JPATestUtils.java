package dk.dbc.dataio.commons.utils.test.jpa;

import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

/**
 * Utils for creating test entity Manager
 */
public class JPATestUtils {
    static final String PERSISTENCE_UNIT_NAME = "persistence.unit.name";
    static final String POSTGRESQL_DBNAME = "postgresql.dbname";
    static final String POSTGRESQL_HOST = "postgresql.host";
    static final String POSTGRESQL_PORT = "postgresql.port";

    // Static Utility class
    private JPATestUtils() {
    }

    /**
     * @param entityManagers list of {@link EntityManager} for which to clear cache
     */
    public static void clearEntityManagerCache(EntityManager... entityManagers) {
        for (EntityManager entityManager : entityManagers) {
            entityManager.clear();
            entityManager.getEntityManagerFactory().getCache().evictAll();
        }
    }

    /**
     * @return integration test {@link EntityManager} instance for persistence unit named
     * by system property {@value JPATestUtils#PERSISTENCE_UNIT_NAME}
     */
    public static EntityManager getIntegrationTestEntityManager() {
        return getIntegrationTestEntityManager(System.getProperty(PERSISTENCE_UNIT_NAME));
    }

    /**
     * Creates integration test {@link EntityManager} instance for named persistence unit
     *
     * @param persistenceUnitName name of persistence unit
     * @return Returns a Configured for tests Persistence manager
     */
    public static EntityManager getIntegrationTestEntityManager(String persistenceUnitName) {
        final ConnectionProperties connectionProperties = new ConnectionProperties();

        final Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put(JDBC_URL, connectionProperties.getJdbcUrl());
        entityManagerProperties.put(JDBC_USER, connectionProperties.getUser());
        if (connectionProperties.getPassword() != null) {
            entityManagerProperties.put(JDBC_PASSWORD, connectionProperties.getPassword());
        }
        entityManagerProperties.put("eclipselink.logging.level", "FINE");

        final EntityManagerFactory entityManagerFactory = Persistence
                .createEntityManagerFactory(persistenceUnitName, entityManagerProperties);
        return entityManagerFactory.createEntityManager(entityManagerProperties);
    }

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        final ConnectionProperties connectionProperties = new ConnectionProperties();
        return DriverManager.getConnection(connectionProperties.getJdbcUrl(), connectionProperties.getUser(),
                connectionProperties.getPassword());
    }

    /**
     * @return integration test {@link DataSource} instance for database named
     * by system property {@value JPATestUtils#POSTGRESQL_DBNAME}
     * @throws SQLException on failure to create a working {@link DataSource}
     */
    public static DataSource getIntegrationTestDataSource() throws SQLException {
        return getIntegrationTestDataSource(System.getProperty(POSTGRESQL_DBNAME));
    }

    public static DataSource getIntegrationTestDataSource(String dataBaseName) throws SQLException {
        final ConnectionProperties connectionProperties = new ConnectionProperties();
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName(dataBaseName);
        dataSource.setUrl(connectionProperties.getJdbcUrl());
        dataSource.setUser(System.getProperty("user.name"));
        dataSource.setPassword(System.getProperty("user.name"));
        // Fail early if unable to open connection
        try {
            final Connection c = dataSource.getConnection();
            c.close();
        } catch (SQLException e) {
            System.out.println("Error getting connection to Port " + dataSource.getPortNumber());
            throw e;
        }
        return dataSource;
    }

    /**
     * Removes all tables, functions and indexes from the database for the persistence unit managed by the
     * given {@link EntityManager}
     *
     * @param entityManager controlling {@link EntityManager}
     */
    public static void clearDatabase(EntityManager entityManager) {
        JPATestUtils.runSqlFromResource(entityManager, new JPATestUtils(), "drop_all_pg.sql");
    }

    /**
     * Executes SQL commands found in named resource using given {@link EntityManager}
     * Example:
     * JPATestUtils.runSqlFromResource(em, this, "load_test_data.sql");
     *
     * @param entityManager {@link EntityManager} instance
     * @param object        object which runtime class is used to resolve resource location
     * @param resourceName  name of resource containing SQL commands
     */
    public static void runSqlFromResource(EntityManager entityManager, Object object, String resourceName) {
        final String sql = ResourceReader.getResourceAsString(object.getClass(), resourceName);
        entityManager.getTransaction().begin();
        entityManager.createNativeQuery(sql).executeUpdate();
        entityManager.getTransaction().commit();
    }

    private static class ConnectionProperties {
        private String user;
        private String password;
        private String jdbcUrl;

        ConnectionProperties() {
            password = null;
            String dataBaseName = "testdb";
            String dataBaseHost = "localhost";
            String port = System.getProperty(POSTGRESQL_PORT);
            if (port == null || port.length() < 1) {
                port = "5432";
                password = System.getenv("USER");
                dataBaseName = System.getenv("USER");

                if (System.getProperty(POSTGRESQL_HOST) != null) {
                    dataBaseHost = System.getProperty(POSTGRESQL_HOST);
                }

                if (System.getProperty(POSTGRESQL_DBNAME) != null) {
                    dataBaseName = System.getProperty(POSTGRESQL_DBNAME);
                }
            }

            jdbcUrl = "jdbc:postgresql://" + dataBaseHost + ":" + port + "/" + dataBaseName;

            user = System.getenv("USER");
        }

        String getPassword() {
            return password;
        }

        String getJdbcUrl() {
            return jdbcUrl;
        }

        String getUser() {
            return user;
        }
    }
}
