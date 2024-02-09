package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

import java.util.HashMap;
import java.util.Map;

public enum SinkConfig implements EnvConfig {
    QUEUE("sink::tickle-repo"),
    FILESTORE_URL("filestore"),
    FLOWSTORE_URL("flowstore"),
    WEEKRESOLVER_SERVICE_URL("weekresolver"),
    TICKLE_REPO_DB_URL,
    TICKLE_BEHAVIOUR("incremental");

    private final String defaultValue;

    SinkConfig() {
        this(null);
    }

    SinkConfig(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    private static final Map<SinkConfig, String> TEST_OVERRIDES = new HashMap<>();

    @Override
    public String get() {
        return TEST_OVERRIDES.getOrDefault(this, EnvConfig.super.get());
    }

    @Override
    public void setTestOverride(String value) {
        TEST_OVERRIDES.put(this, value);
    }

    @Override
    public void clearAllTestOverrides() {
        TEST_OVERRIDES.clear();
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }


}
