package dk.dbc.dataio.jse.artemis.common.db;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_DATABASE_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TRANSACTION_TYPE;

public class JPAHelper {

    public static EntityManagerFactory makeEntityManagerFactory(String persistenceUnit, EnvConfig config) {
        return makeEntityManagerFactory(persistenceUnit, config.asPGJDBCUrl());
    }

    public static EntityManagerFactory makeEntityManagerFactory(String persistenceUnit, String jdbcUrl) {
        Map<String, String> config = Map.of(
                TRANSACTION_TYPE, "RESOURCE_LOCAL",
                "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                SCHEMA_GENERATION_DATABASE_ACTION, "none",
                JDBC_DRIVER, "org.postgresql.Driver",
                JDBC_URL, jdbcUrl);
        return makeEntityManagerFactory(persistenceUnit, config);
    }

    public static EntityManagerFactory makeEntityManagerFactory(String persistenceUnit, Map<String, String> config) {
        return Persistence.createEntityManagerFactory(persistenceUnit, config);
    }

    public static void migrate(EnvConfig config) {
        migrate(config.asPGJDBCUrl());
    }

    public static void migrate(String jdbcUrl) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(jdbcUrl);
        migrate(dataSource);
    }

    public static void migrate(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .load();
        flyway.migrate();
    }
}
