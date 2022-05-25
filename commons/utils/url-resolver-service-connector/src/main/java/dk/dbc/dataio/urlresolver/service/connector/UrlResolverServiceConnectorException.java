package dk.dbc.dataio.urlresolver.service.connector;

/**
 * Thrown to indicate general error during job-store communication
 */
public class UrlResolverServiceConnectorException extends Exception {

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public UrlResolverServiceConnectorException(String message) {
        super(message);
    }

    public UrlResolverServiceConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
