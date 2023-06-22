package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("sink::worldcat"),
    MESSAGE_FILTER,
    OCN_REPO_DB_URL,
    FLOWSTORE_URL,
    USE_PROXY;

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
