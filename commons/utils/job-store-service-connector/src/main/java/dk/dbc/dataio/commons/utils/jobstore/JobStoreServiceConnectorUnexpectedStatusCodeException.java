package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.jobstore.types.JobError;

public class JobStoreServiceConnectorUnexpectedStatusCodeException extends JobStoreServiceConnectorException {
    private static final long serialVersionUID = -6449911421903807835L;

    private JobError jobError;

    private final int statusCode;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message    detail message saved for later retrieval by the
     *                   {@link #getMessage()} method. May be null.
     * @param statusCode the http status code returned by the REST service
     */
    public JobStoreServiceConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the job error
     */
    public JobError getJobError() {
        return jobError;
    }

    /**
     * Sets the job error
     *
     * @param jobError the job error to set
     */
    public void setJobError(JobError jobError) {
        this.jobError = jobError;
    }
}
