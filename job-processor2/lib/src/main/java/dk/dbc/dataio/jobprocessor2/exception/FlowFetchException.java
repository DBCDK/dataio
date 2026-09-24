package dk.dbc.dataio.jobprocessor2.exception;

/**
 * Thrown when the flow for a job could not be fetched from job-store.
 * <p>
 * Unchecked, because it is raised from inside a flow cache loader and the cache's own
 * signature cannot carry it.
 */
public class FlowFetchException extends RuntimeException {
    public FlowFetchException(String message, Exception cause) {
        super(message, cause);
    }
}
