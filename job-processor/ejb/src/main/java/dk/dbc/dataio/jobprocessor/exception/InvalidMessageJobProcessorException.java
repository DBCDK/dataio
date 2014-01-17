package dk.dbc.dataio.jobprocessor.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class InvalidMessageJobProcessorException extends JobProcessorException {
    private static final long serialVersionUID = 2550554342946863031L;

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public InvalidMessageJobProcessorException(String message) {
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
    public InvalidMessageJobProcessorException(String message, Exception cause) {
        super(message, cause);
    }
}
