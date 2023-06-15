package dk.dbc.dataio.jse.artemis.common;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum DBProperty {
    USER,
    PASSWORD,
    HOST,
    PORT,
    DATABASE;

    private static final Pattern pattern = Pattern.compile("(?<USER>[^:]+):(?<PASSWORD>[^@]+)@(?<HOST>[^:]+):(?<PORT>\\d+)/(?<DATABASE>.+)");

    public static Map<DBProperty, String> from(String s) {
        Matcher matcher = pattern.matcher(s);
        if(!matcher.matches()) return Map.of();
        return Arrays.stream(values()).collect(Collectors.toMap(e -> e, e -> matcher.group(e.name())));
    }
}
