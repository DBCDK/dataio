package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

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

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
