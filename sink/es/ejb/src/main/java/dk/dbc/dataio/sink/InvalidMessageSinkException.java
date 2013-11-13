package dk.dbc.dataio.sink;

public class InvalidMessageSinkException extends SinkException {
    private static final long serialVersionUID = -3472257595510419494L;

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public InvalidMessageSinkException(String message) {
        super(message);
    }
}
