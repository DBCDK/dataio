package dk.dbc.dataio.jobprocessor.exception;

public class JobProcessorException extends RuntimeException {
    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public JobProcessorException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause
     *
     * Note that the detail message associated with cause is not
     * automatically incorporated in this exception's detail message.
     *
     * @param  message detail message saved for later retrieval
     *                 by the {@link #getMessage()} method). May be null.
     * @param  cause cause saved for later retrieval by the
     *               {@link #getCause()} method). (A null value is
     *               permitted, and indicates that the cause is nonexistent or
     *               unknown).
     */
    public JobProcessorException(String message, Exception cause) {
        super(message, cause);
    }
}
