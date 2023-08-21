package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE,
    BATCH_EXCHANGE_DB_URL
}
