package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("sink::dummy"),
    MESSAGE_FILTER(null);

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
