package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("dummy::dummy"),
    MESSAGE_FILTER(null),
    FLOWSTORE_URL("http://localhost"),
    LOBBY_SERVICE_URL("http://localhost"),
    OPENNUMBERROLL_SERVICE_URL("http://localhost"),
    RAWREPO_RECORD_SERVICE_URL("http://localhost"),
    UPDATE_SERVICE_URL("http://localhost"),
    UPDATE_SERVICE_WS_URL("http://localhost"),
    WEEKRESOLVER_SERVICE_URL("http://localhost");

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
