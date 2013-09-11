package dk.dbc.dataio.flowstore.entity;

public class Error {
    private final String message;
    private final StackTraceElement[] stackTrace;

    public Error(String message) {
        this(message, null);
    }

    public Error(String message, StackTraceElement[] stackTrace) {
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getMessage() {
        return message;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
}
