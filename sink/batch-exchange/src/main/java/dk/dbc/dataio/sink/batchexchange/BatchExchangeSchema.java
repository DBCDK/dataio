package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import javax.sql.DataSource;
import java.util.function.Consumer;

/**
 * Applies the migrations this sink owns in the batch exchange database.
 * <p>
 * Kept apart from the migrations of the batch-exchange-api artifact, and under a history
 * table of its own, so that a version number added on either side cannot collide with one
 * added on the other.
 */
public class BatchExchangeSchema {
    private static final String HISTORY_TABLE = "dataio_schema_version";
    private static final String LOCATION = "classpath:dk/dbc/dataio/sink/batchexchange/db/migration";

    private BatchExchangeSchema() {
    }

    public static void migrate(EnvConfig jdbcUrl) {
        JPAHelper.migrate(jdbcUrl, configuration());
    }

    public static void migrate(DataSource dataSource) {
        JPAHelper.migrateCustom(dataSource, configuration());
    }

    /**
     * Baselines at version 0 rather than at Flyway's default of 1
     * <p>
     * The batch exchange database is never empty when these migrations first run, since the
     * batch-exchange-api migrations have already created its tables, and baselining a
     * non-empty schema at version 1 would record V1 as already applied and skip it.
     */
    private static Consumer<FluentConfiguration> configuration() {
        return c -> c.table(HISTORY_TABLE)
                .locations(LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion("0");
    }
}
