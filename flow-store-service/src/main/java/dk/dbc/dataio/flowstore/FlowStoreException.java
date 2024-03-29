package dk.dbc.dataio.flowstore;

import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class FlowStoreException extends ServiceException {
    private static final long serialVersionUID = -7107160322333302235L;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public FlowStoreException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause
     * <p>
     * Note that the detail message associated with cause is not
     * automatically incorporated in this exception's detail message.
     *
     * @param message detail message saved for later retrieval
     *                by the {@link #getMessage()} method). May be null.
     * @param cause   cause saved for later retrieval by the
     *                {@link #getCause()} method). (A null value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown).
     */
    public FlowStoreException(String message, Exception cause) {
        super(message, cause);
    }
}
