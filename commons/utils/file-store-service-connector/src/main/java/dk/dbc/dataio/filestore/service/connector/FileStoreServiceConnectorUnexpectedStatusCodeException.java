package dk.dbc.dataio.filestore.service.connector;

public class FileStoreServiceConnectorUnexpectedStatusCodeException extends FileStoreServiceConnectorException {
    private static final long serialVersionUID = -3210070082400224216L;

    private final int statusCode;

    /**
     * Constructs a new exception with the specified detail message
     * <p/>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public FileStoreServiceConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;

    }

    public int getStatusCode() {
        return statusCode;
    }

}
