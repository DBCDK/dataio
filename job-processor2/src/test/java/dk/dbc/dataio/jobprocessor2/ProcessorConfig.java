package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum ProcessorConfig implements EnvConfig {
    QUEUE("dummy::dummy"),
    FLOW_CACHE_SIZE("100"),
    FLOW_CACHE_EXPIRY("PT10m"),
    SHARE_FLOWS("false"),
    FLOWSTORE_URL;

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
