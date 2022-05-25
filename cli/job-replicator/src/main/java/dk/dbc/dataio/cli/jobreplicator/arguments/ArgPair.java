package dk.dbc.dataio.cli.jobreplicator.arguments;

public class ArgPair {
    private String key, value;

    private ArgPair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static ArgPair fromString(String keyValuePair) throws ArgParseException {
        String[] parts = keyValuePair.split("=");
        if (parts.length != 2) {
            throw new ArgParseException(String.format(
                    "couldn't parse argument %s", keyValuePair));
        }
        return new ArgPair(parts[0], parts[1]);
    }
}
