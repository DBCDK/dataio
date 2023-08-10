package dk.dbc.dataio.jse.artemis.common.db;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.util.Map;

public class JPAHelper {

    public static EntityManager makeEntityManager(String persistenceUnit, EnvConfig config) {
        return makeEntityManager(persistenceUnit, config.asPGJDBCUrl());
    }

    public static EntityManager makeEntityManager(String persistenceUnit, String jdbcUrl) {
        Map<String, String> config = Map.of(
                "javax.persistence.transactionType", "RESOURCE_LOCAL",
                "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                "javax.persistence.schema-generation.database.action", "none",
                "javax.persistence.jdbc.driver", "org.postgresql.Driver",
                "javax.persistence.jdbc.url", jdbcUrl);
        return makeEntityManager(persistenceUnit, config);
    }

    public static EntityManager makeEntityManager(String persistenceUnit, Map<String, String> config) {
        return Persistence.createEntityManagerFactory(persistenceUnit, config).createEntityManager();
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
