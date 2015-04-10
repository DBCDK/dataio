package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.jobstore.types.JobError;

public class JobStoreServiceConnectorUnexpectedStatusCodeException extends JobStoreServiceConnectorException{
    private static final long serialVersionUID = -6449911421903807835L;

    private JobError jobError;

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
    public JobStoreServiceConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public JobError getJobError() {
        return jobError;
    }

    public void setJobError(JobError jobError) {
        this.jobError = jobError;
    }
}
