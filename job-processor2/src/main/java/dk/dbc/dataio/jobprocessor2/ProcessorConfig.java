package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum ProcessorConfig implements EnvConfig {
    QUEUE,
    MESSAGE_FILTER(null),
    FLOW_CACHE_SIZE("100"),
    FLOW_CACHE_EXPIRY("PT10m");

    private final String defaultValue;

    ProcessorConfig() {
        this(null);
    }

    ProcessorConfig(String defaultValue) {
        this.defaultValue = defaultValue;
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
