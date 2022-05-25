package dk.dbc.dataio.jobprocessor.exception;

public class JobProcessorCapacityExceededException extends JobProcessorException {
    /**
     * Constructs a new exception with the specified detail message
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public JobProcessorCapacityExceededException(String message) {
        super(message);
    }
}
