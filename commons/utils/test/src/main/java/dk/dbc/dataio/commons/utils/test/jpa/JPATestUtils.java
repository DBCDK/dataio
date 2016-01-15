package dk.dbc.dataio.commons.utils.test.jpa;

import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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


    public static final String POSTGRESQL_DBNAME = "postgresql.dbname";
    public static final String POSTGRESQL_HOST = "postgresql.host";
    public static final String POSTGRESQL_PORT = "postgresql.port";

    // Static Utility class
    private JPATestUtils() {
    }

    /**
     * @param entityManagers list of entityManagers to clear
     */
    public static void clearEntityManagerCache(EntityManager... entityManagers) {
        for (EntityManager entityManager : entityManagers) {
            entityManager.clear();
            entityManager.getEntityManagerFactory().getCache().evictAll();
        }
    }

    /**
     * Create a Entity Manager for testing
     *
     * @param persistenceUnitName Name of the Persistence Manager
     * @return Returns a Configured for tests Persistence manager
     */
    public static EntityManager createEntityManagerForIntegrationTest(String persistenceUnitName) {
        Map<String, String> properties = new HashMap<>();

        GetTestConnectInfo getTestConnectInfo = new GetTestConnectInfo().invoke();
        String jdbc = getTestConnectInfo.getJdbc();
        String password = getTestConnectInfo.getPassword();

        properties.put(JDBC_URL, jdbc);
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put(JDBC_USER, getTestConnectInfo.getLogin());
        if( password != null ) {
            properties.put(JDBC_PASSWORD, password);
        }
        properties.put("eclipselink.logging.level", "FINE");

        EntityManagerFactory EmfLoad = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
        return EmfLoad.createEntityManager(properties);
    }

    public static Connection getConnection(  ) throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        GetTestConnectInfo getTestConnectInfo = new GetTestConnectInfo().invoke();
        return DriverManager.getConnection(getTestConnectInfo.getJdbc(), getTestConnectInfo.getLogin(), getTestConnectInfo.getPassword());
    }

    public static DataSource getTestDataSource( String dataBaseName ) {
        if ( System.getProperty(POSTGRESQL_PORT) == null) {
            // Hack
            dataBaseName = System.getenv("USER");
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName(dataBaseName);
        dataSource.setServerName("localhost");
        dataSource.setPortNumber(Integer.parseInt(System.getProperty(POSTGRESQL_PORT, "5432")));
        dataSource.setUser(System.getProperty("user.name"));
        dataSource.setPassword(System.getProperty("user.name"));
        return dataSource;
    }

    /**
     *
     * Removed all Tables, functions, indexes types from the tatebase.
     * @param entityManager The entity Manager to clean the database for.
     * @throws IOException when Unable to load drop_all_pg.sql script
     * @throws URISyntaxException Shut not happen.
     *
     */
    public static void clearDatabase( EntityManager entityManager ) throws IOException, URISyntaxException {
        JPATestUtils.runSqlFromResource(entityManager,new JPATestUtils(),"drop_all_pg.sql");
    }

    /**
     * @param manager EntityManager to use.
     * @param testClass use this for executing test from test/resources
     * @param resouceName Resource sql
     * @throws IOException when Unable to load drop_all_pg.sql script
     * @throws URISyntaxException with errors in resourceName
     *
     * Example:
     *
     *  JPATestUtils.runSqlFromResource(em, this, "load_test_data.sql");
     *
     */
    public static void runSqlFromResource(EntityManager manager, Object testClass, String resouceName) throws IOException, URISyntaxException {
        String sql= readResource(testClass, resouceName);
        manager.getTransaction().begin();
        Query q = manager.createNativeQuery(sql);
        q.executeUpdate();
        manager.getTransaction().commit();
    }

    static String readResource(Object testClass, String resourceName) throws IOException, URISyntaxException {
        final StringBuilder buffer = new StringBuilder();
        final int buffSize=1024;
        final char[] buff=new char[buffSize];
        try (
                final InputStreamReader isr = new InputStreamReader(
                        testClass.getClass().getResourceAsStream("/" + resourceName), StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr)) {
            for (int length = br.read(buff,0,buffSize); length != -1; length = br.read(buff,0,buffSize) )
                buffer.append(buff,0,length);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return buffer.toString();
    }


    private static class GetTestConnectInfo {
        private String login;
        private String password;
        private String jdbc;

        public String getPassword() {
            return password;
        }

        public String getJdbc() {
            return jdbc;
        }

        public String getLogin() {
            return login;
        }

        public GetTestConnectInfo invoke() {
            password = null;
            String dataBaseName = "testdb";
            String dataBaseHost = "localhost";
            String port = System.getProperty(POSTGRESQL_PORT);
            if (port == null) {
                port = "5432";
                password = System.getenv("USER");
                dataBaseName = System.getenv("USER");

                if (System.getProperty(POSTGRESQL_HOST) != null ) {
                    dataBaseHost =  System.getProperty(POSTGRESQL_HOST);
                }

                if (System.getProperty(POSTGRESQL_DBNAME) != null ) {
                    dataBaseName =  System.getProperty(POSTGRESQL_DBNAME);
                }
            }

            jdbc = "jdbc:postgresql://" + dataBaseHost + ":" + port + "/" + dataBaseName;

            login = System.getenv("USER");
            return this;
        }
    }

}
