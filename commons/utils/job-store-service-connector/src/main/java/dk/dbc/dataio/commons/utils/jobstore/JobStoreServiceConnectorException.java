package dk.dbc.dataio.commons.utils.jobstore;

/**
 * Thrown to indicate general error during job-store communication
 */
public class JobStoreServiceConnectorException extends Exception {
    private static final long serialVersionUID = 6110485229279206241L;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public JobStoreServiceConnectorException(String message) {
        super(message);
    }

    public JobStoreServiceConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
