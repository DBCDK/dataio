package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE,
    MESSAGE_FILTER(null),
    FLOWSTORE_URL,
    LOBBY_SERVICE_URL,
    OPENNUMBERROLL_SERVICE_URL,
    RAWREPO_RECORD_SERVICE_URL,
    UPDATE_SERVICE_WS_URL,
    WEEKRESOLVER_SERVICE_URL;

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
