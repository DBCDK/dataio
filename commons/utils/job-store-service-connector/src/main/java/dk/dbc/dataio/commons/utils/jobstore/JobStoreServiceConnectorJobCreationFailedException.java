package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.JobErrorCode;

/**
 * Thrown to indicate that a job-store job failed during creation
 * due to input data errors
 */
public class JobStoreServiceConnectorJobCreationFailedException extends JobStoreServiceConnectorException {
    private static final long serialVersionUID = -8358703988152539362L;

    private final JobErrorCode jobErrorCode;

    /**
     * Constructs a new exception with the specified detail message
     * <p/>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     * @param jobErrorCode job error code
     */
    public JobStoreServiceConnectorJobCreationFailedException(String message, JobErrorCode jobErrorCode) {
        super(message);
        this.jobErrorCode = jobErrorCode;
    }

    public JobErrorCode getJobErrorCode() {
        return jobErrorCode;
    }
}
