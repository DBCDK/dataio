package dk.dbc.dataio.sink.es;

import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.io.IOException;
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
 * Created by ja7 on 05-10-15.
 * Utills for creating test entity Manager
 */
public class JPATestUtils {


    public static final String POSTGRESQL_DBNAME = "postgresql.dbname";
    public static final String POSTGRESQL_HOST = "postgresql.host";

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

    public static Connection getEsConnection(  ) throws ClassNotFoundException, SQLException {
        //Class.forName("oracle.jdbc.driver.OracleDriver");
        Class.forName("org.postgresql.Driver");
        GetTestConnectInfo getTestConnectInfo = new GetTestConnectInfo().invoke();
        return DriverManager.getConnection(getTestConnectInfo.getJdbc(), getTestConnectInfo.getLogin(), getTestConnectInfo.getPassword());
    };

    public static DataSource getTestDataSource( String dataBaseName ) {
        if ( System.getProperty("postgresql.port") == null) {
            // Hack
            dataBaseName = System.getenv("USER");
        }
        PGSimpleDataSource ES_INFLIGHT_DATASOURCE = new PGSimpleDataSource();
        ES_INFLIGHT_DATASOURCE.setDatabaseName(dataBaseName);
        ES_INFLIGHT_DATASOURCE.setServerName("localhost");
        ES_INFLIGHT_DATASOURCE.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port", "5432")));
        ES_INFLIGHT_DATASOURCE.setUser(System.getProperty("user.name"));
        ES_INFLIGHT_DATASOURCE.setPassword(System.getProperty("user.name"));
        return ES_INFLIGHT_DATASOURCE;
    }

    /**
     * @param manager EntityManager to use.
     * @param resouceName Resource sql
     */
    public static void runSqlFromResource(EntityManager manager, String resouceName) throws IOException, URISyntaxException {
        String sql= readResouce(resouceName);
        manager.getTransaction().begin();
        Query q = manager.createNativeQuery(sql);
        q.executeUpdate();
        manager.getTransaction().commit();
    }

    static String readResouce(String resourceName) throws IOException, URISyntaxException {
        final java.net.URL url = JPATestUtils.class.getResource("/" + resourceName);
        final java.nio.file.Path resPath;
        resPath = java.nio.file.Paths.get(url.toURI());
        return new String(java.nio.file.Files.readAllBytes(resPath), StandardCharsets.UTF_8);
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
            String dataBaseName = "es";
            String dataBaseHost = "localhost";
            String port = System.getProperty("postgresql.port");
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
