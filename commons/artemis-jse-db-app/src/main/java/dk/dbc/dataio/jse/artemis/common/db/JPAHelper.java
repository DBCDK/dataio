package dk.dbc.dataio.jse.artemis.common.db;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

    public static void migrate(EnvConfig config, Consumer<FluentConfiguration>... configurator) {
        migrate(config.asPGJDBCUrl(), configurator);
    }

    public static void migrate(String jdbcUrl, Consumer<FluentConfiguration>... configurator) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(jdbcUrl);
        migrateCustom(dataSource, configurator);
    }

    public static void migrate(DataSource dataSource) {
        migrateCustom(dataSource);
    }

    @SafeVarargs
    public static void migrateCustom(DataSource dataSource, Consumer<FluentConfiguration>... configurator) {
        FluentConfiguration configuration = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource);
        Stream.of(configurator).forEach(c -> c.accept(configuration));
        PostgreSQLConfigurationExtension configurationExtension = configuration.getPluginRegister().getPlugin(PostgreSQLConfigurationExtension.class);
        if(configurationExtension != null) configurationExtension.setTransactionalLock(false);
        Flyway flyway = configuration.load();
        flyway.migrate();
    }
}
