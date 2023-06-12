package dk.dbc.dataio.sink.marcconv;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("sink::marcconv"),
    MESSAGE_FILTER(null),
    FILESTORE_URL;

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
