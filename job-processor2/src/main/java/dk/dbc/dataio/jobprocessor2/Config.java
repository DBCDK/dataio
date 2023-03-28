package dk.dbc.dataio.jobprocessor2;

import java.util.Optional;

public enum Config {
    WEB_PORT("0"),
    ARTEMIS_MQ_HOST,
    ARTEMIS_JMS_PORT("61616"),
    ARTEMIS_ADMIN_PORT,
    ARTEMIS_USER,
    ARTEMIS_PASSWORD,
    QUEUE("jmsDataioProcessor"),
    RECONNECT_DELAY("PT10s"),
    MESSAGE_FILTER,
    JOBSTORE_URL;

    private final String defaultValue;

    Config() {
        this(null);
    }

    Config(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Optional<String> asOptionalString() {
        return getProperty(name()).or(() -> Optional.ofNullable(defaultValue));
    }

    public Optional<Integer> asOptionalInteger() {
        return asOptionalString().map(Integer::parseInt);
    }

    public Integer asInteger() {
        return asOptionalInteger().orElseThrow(this::missingConf);
    }

    public String toString() {
        return asOptionalString().orElseThrow(this::missingConf);
    }

    private static Optional<String> getProperty(String key) {
        return Optional.ofNullable(System.getenv(key));
    }

    private IllegalStateException missingConf() {
        return new IllegalStateException("Required configuration " + name() + " is missing");
    }
}
