package dk.dbc.dataio.jobstore.service.ejb;

public class JobAborted extends RuntimeException {
    public JobAborted(int jobId) {
        super("Job " + jobId + " has been aborted");
    }

    public JobAborted(String message) {
        super(message);
    }

    public JobAborted(String message, Throwable cause) {
        super(message, cause);
    }

    public JobAborted(Throwable cause) {
        super(cause);
    }

    public JobAborted(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
