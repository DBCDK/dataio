package dk.dbc.dataio.cli.diff;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum DiffConfig implements EnvConfig {
    USE_NATIVE_DIFF("true"),
    TOOL_PATH("/home/java/tools");

    private final String defaultValue;

    DiffConfig() {
        this(null);
    }

    DiffConfig(String defaultValue) {
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
