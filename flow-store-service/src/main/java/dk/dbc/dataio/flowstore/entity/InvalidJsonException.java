package dk.dbc.dataio.flowstore.entity;

public class InvalidJsonException extends Exception {
    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public InvalidJsonException(String message) {
        super(message);
    }
}
