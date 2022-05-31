package dk.dbc.dataio.querylanguage;

public class Identifier {
    private final String resource;
    private final String field;

    public static Identifier of(Token token) {
        final String[] parts = token.image.split(":");
        return new Identifier(parts[0], parts[1]);
    }

    public String getResource() {
        return resource;
    }

    public String getField() {
        return field;
    }

    private Identifier(String resource, String field) {
        this.resource = resource;
        this.field = field;
    }
}
