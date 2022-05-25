package dk.dbc.dataio.logstore.service.connector;

public class LogStoreServiceConnectorException extends Exception {
    private static final long serialVersionUID = 5031796787148644466L;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public LogStoreServiceConnectorException(String message) {
        super(message);
    }
}
