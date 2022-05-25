package dk.dbc.dataio.harvester.task.connector;

public class HarvesterTaskServiceConnectorException extends Exception {

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public HarvesterTaskServiceConnectorException(String message) {
        super(message);
    }

    public HarvesterTaskServiceConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
