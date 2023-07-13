package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("dummy::dummy"),
    MESSAGE_FILTER,
    BATCH_EXCHANGE_DB_URL;

    private final String defaultValue;

    SinkConfig() {
        this(null);
    }

    SinkConfig(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
