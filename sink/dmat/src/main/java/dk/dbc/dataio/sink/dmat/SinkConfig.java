package dk.dbc.dataio.sink.dmat;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("sink::dmat"),
    MESSAGE_FILTER(null),
    DMAT_SERVICE_URL;

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
