package dk.dbc.dataio.flowstore.entity;

public class Error {
    private final String message;
    private final String details;

    public Error(String message, String details) {
        this.message = message;
        this.details = details;
    }

    public Error(String message) {
        this(message, "");
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}
