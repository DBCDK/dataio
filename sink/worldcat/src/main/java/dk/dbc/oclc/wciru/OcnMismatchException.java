package dk.dbc.oclc.wciru;

public class OcnMismatchException extends Exception {
    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public OcnMismatchException(String message) {
        super(message);
    }
}
