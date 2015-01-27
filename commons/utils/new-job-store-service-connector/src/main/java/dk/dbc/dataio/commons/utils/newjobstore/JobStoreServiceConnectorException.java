package dk.dbc.dataio.commons.utils.newjobstore;

/**
 * Thrown to indicate general error during job-store communication
 */
public class JobStoreServiceConnectorException extends Exception {

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public JobStoreServiceConnectorException(String message) {
        super(message);
    }
}
