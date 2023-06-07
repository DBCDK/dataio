package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum ProcessorConfig implements EnvConfig {
    QUEUE,
    MESSAGE_FILTER(null);

    private final String defaultValue;

    ProcessorConfig() {
        this(null);
    }

    ProcessorConfig(String defaultValue) {
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

    @Override
    public String toString() {
        return asString();
    }
}
