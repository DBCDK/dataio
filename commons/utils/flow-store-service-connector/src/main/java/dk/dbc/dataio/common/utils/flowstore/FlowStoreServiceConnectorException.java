package dk.dbc.dataio.common.utils.flowstore;

public class FlowStoreServiceConnectorException extends Exception {

    private static final long serialVersionUID = -5384112996352499012L;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public FlowStoreServiceConnectorException(String message) {
        super(message);
    }

    public FlowStoreServiceConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
