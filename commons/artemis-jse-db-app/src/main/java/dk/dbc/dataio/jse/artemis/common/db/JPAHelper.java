package dk.dbc.dataio.jse.artemis.common.db;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.util.Map;

public class JPAHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(JPAHelper.class);

    public static EntityManager makeEntityManager(String persistenceUnit, EnvConfig config) {
        return makeEntityManager(persistenceUnit, config.asPGJDBCUrl());
    }

    public static EntityManager makeEntityManager(String persistenceUnit, String jdbcUrl) {
        try {
            Map<String, String> config = Map.of(
                    "javax.persistence.transactionType", "RESOURCE_LOCAL",
                    "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                    "javax.persistence.schema-generation.database.action", "none",
                    "javax.persistence.jdbc.driver", "org.postgresql.Driver",
                    "javax.persistence.jdbc.url", jdbcUrl);
            return makeEntityManager(persistenceUnit, config);
        } catch (RuntimeException e) {
            LOGGER.info("Unable to create an entity manager for {}", persistenceUnit, e);
            throw e;
        }
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
