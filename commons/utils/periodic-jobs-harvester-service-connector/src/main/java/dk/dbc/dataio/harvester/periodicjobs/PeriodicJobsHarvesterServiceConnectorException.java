package dk.dbc.dataio.harvester.periodicjobs;

public class PeriodicJobsHarvesterServiceConnectorException extends Exception {
    private static final long serialVersionUID = 599102586185659212L;

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public PeriodicJobsHarvesterServiceConnectorException(String message) {
        super(message);
    }

    public PeriodicJobsHarvesterServiceConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
