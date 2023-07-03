package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("sink::periodic-jobs"),
    FILESTORE_URL,
    FLOWSTORE_URL,
    WEEKRESOLVER_SERVICE_URL,
    PERIODIC_JOBS_DB_URL,
    MAIL_HOST("mailhost.dbc.dk"),
    MAIL_USER("mailuser"),
    MAIL_FROM("dataio@dbc.dk"),
    PROXY_HOSTNAME,
    PROXY_PORT("1080"),
    PROXY_USERNAME,
    PROXY_PASSWORD,
    NON_PROXY_HOSTS("dbc.dk");

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
