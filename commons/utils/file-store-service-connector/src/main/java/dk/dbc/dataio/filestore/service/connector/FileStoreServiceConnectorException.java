package dk.dbc.dataio.filestore.service.connector;

public class FileStoreServiceConnectorException extends Exception {
    private static final long serialVersionUID = 599102586185659212L;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public FileStoreServiceConnectorException(String message) {
        super(message);
    }

    public FileStoreServiceConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
