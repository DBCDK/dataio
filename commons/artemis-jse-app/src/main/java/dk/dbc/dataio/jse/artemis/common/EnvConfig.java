package dk.dbc.dataio.jse.artemis.common;

import java.time.Duration;
import java.util.Optional;

public interface EnvConfig {
    default Optional<String> asOptionalString() {
        return getProperty(getName()).or(() -> Optional.ofNullable(getDefaultValue()));
    }

    default Optional<Integer> asOptionalInteger() {
        return asOptionalString().map(Integer::parseInt);
    }

    default Optional<Duration> asOptionalDuration() {
        return asOptionalString().map(Duration::parse);
    }

    default Integer asInteger() {
        return asOptionalInteger().orElseThrow(this::missingConf);
    }

    default String asString() {
        return asOptionalString().orElseThrow(this::missingConf);
    }

    default Duration asDuration() {
        return asOptionalDuration().orElseThrow(this::missingConf);
    }

    private static Optional<String> getProperty(String key) {
        return Optional.ofNullable(System.getenv(key));
    }

    private IllegalStateException missingConf() {
        return new IllegalStateException("Required configuration " + getName() + " is missing");
    }

    String name();

    default String getName() {
        return name();
    }

    public String getDefaultValue();
}
