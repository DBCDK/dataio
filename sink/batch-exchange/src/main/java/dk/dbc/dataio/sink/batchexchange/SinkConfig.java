package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE,
    BATCH_EXCHANGE_DB_URL,
    FINALIZER_LIVENESS_THRESHOLD("PT10m");
    private final String defaultValue;

    SinkConfig() {
        this.defaultValue = null;
    }

    SinkConfig(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
