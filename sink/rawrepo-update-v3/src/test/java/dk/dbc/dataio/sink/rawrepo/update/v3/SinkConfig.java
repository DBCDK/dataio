package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("dummy::dummy"),
    MESSAGE_FILTER,
    FLOWSTORE_URL("http://dummy"),
    UPDATE_VALIDATE_ONLY_FLAG("false");

    private final String defaultValue;

    SinkConfig() {
        this(null);
    }

    SinkConfig(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
