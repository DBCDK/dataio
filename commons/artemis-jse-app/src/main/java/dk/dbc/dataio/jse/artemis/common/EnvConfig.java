package dk.dbc.dataio.jse.artemis.common;

import java.net.URLEncoder;
import java.time.DateTimeException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("unused")
public interface EnvConfig {
    default Optional<String> asOptionalString() {
        return getProperty().or(() -> Optional.ofNullable(getDefaultValue())).map(String::trim).filter(s -> !s.isEmpty());
    }

    default Optional<Boolean> asOptionalBoolean() {
       return getProperty().or(() -> Optional.of("false"))
               .map(s -> List.of("TRUE", "ON", "1").contains(s.toUpperCase()));
    }

    default Optional<Integer> asOptionalInteger() {
        return asOptionalString().map(s -> parseNumber(s, Integer::valueOf));
    }

    default Optional<Duration> asOptionalDuration() {
        try {
            return asOptionalString().map(Duration::parse);
        } catch (DateTimeException dte) {
            throw new DateTimeException("Unable to parse key + " + name() + ", with value: " + asOptionalString().orElse("<empty>"));
        }
    }

    default Integer asInteger() {
        return asOptionalInteger().orElseThrow(this::missingConf);
    }

    default String asString() {
        return asOptionalString().orElseThrow(this::missingConf);
    }
    default Boolean asBoolean() { return asOptionalBoolean().orElseThrow(this::missingConf); }

    default Duration asDuration() {
        return asOptionalDuration().orElseThrow(this::missingConf);
    }

    default String fqnAsQueue() {
        String[] fqn = asString().split("::", 2);
        return fqn[fqn.length - 1];
    }

    default String fqnAsAddress() {
        String[] fqn = asString().split("::", 2);
        return fqn[0];
    }

    default Optional<String> getProperty() {
        return Optional.ofNullable(get()).map(String::trim);
    }

    default String get() {
        return System.getenv(getName());
    }

    private IllegalStateException missingConf() {
        return new IllegalStateException("Required configuration " + getName() + " is missing");
    }

    String name();

    default String getName() {
        return name();
    }

    private <T extends Number> T parseNumber(String s, Function<String, T> parser) {
        try {
            return parser.apply(s);
        } catch (NumberFormatException nfe) {
            throw new NumberFormatException("Unable to parse key + " + name() + ", with value: " + asOptionalString().orElse("<empty>"));
        }
    }

    default Map<DBProperty, String> asDBProperties() {
        return DBProperty.from(asString());
    }

    default String asPGJDBCUrl() {
        Map<DBProperty, String> map = asDBProperties();
        return "jdbc:postgresql://" + map.get(DBProperty.HOST) + ":" + map.get(DBProperty.PORT) + "/" + map.get(DBProperty.DATABASE)
                + "?user=" + URLEncoder.encode(map.get(DBProperty.USER), UTF_8) + "&password=" + URLEncoder.encode(map.get(DBProperty.PASSWORD), UTF_8);
    }

    default void setTestOverride(String value) {}
    default void clearAllTestOverrides() {}

    default String getDefaultValue() {
        //noinspection ReturnOfNull
        return null;
    }
}
