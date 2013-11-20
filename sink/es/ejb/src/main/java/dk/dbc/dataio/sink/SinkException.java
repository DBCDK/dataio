package dk.dbc.dataio.sink;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class SinkException extends Exception {
    private static final long serialVersionUID = -3723466835549379446L;

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public SinkException(String message) {
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
    public SinkException(String message, Exception cause) {
        super(message, cause);
    }
}
