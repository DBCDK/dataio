package dk.dbc.dataio.commons.utils.json;

public class JsonException extends Exception {
    private static final long serialVersionUID = -6429142830690189808L;

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public JsonException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause
     *
     * @param  cause cause saved for later retrieval by the
     *               {@link #getCause()} method). (A null value is
     *               permitted, and indicates that the cause is nonexistent or
     *               unknown).
     */
    public JsonException(Exception cause) {
        super(cause.getMessage(), cause);
    }

    public JsonException(String message, Exception cause) {
        super(message, cause);
    }
}
