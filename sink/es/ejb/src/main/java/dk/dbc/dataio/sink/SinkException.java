package dk.dbc.dataio.sink;

public class SinkException extends Exception {
    private static final long serialVersionUID = -3723466835549379446L;

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public SinkException(String message) {
        super(message);
    }
}
