package dk.dbc.dataio.dlq.errorhandler;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum DLQConfig implements EnvConfig {
    QUEUE("DLQ::DLQ");

    private final String defaultValue;

    DLQConfig(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
