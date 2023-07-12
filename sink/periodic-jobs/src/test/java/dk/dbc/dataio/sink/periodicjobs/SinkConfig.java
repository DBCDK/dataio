package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.jse.artemis.common.EnvConfig;

public enum SinkConfig implements EnvConfig {
    QUEUE("sink::periodic-jobs"),
    FILESTORE_URL("filestore"),
    FLOWSTORE_URL("flowstore"),
    WEEKRESOLVER_SERVICE_URL("weekresolver"),
    PERIODIC_JOBS_DB_URL,
    MAIL_HOST("mailhost-in-test"),
    MAIL_USER("mailuser-in-test"),
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
