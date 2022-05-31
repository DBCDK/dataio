package dk.dbc.dataio.commons.types.exceptions;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ReferencedEntityNotFoundException extends Exception {
    private static final long serialVersionUID = 8207867940250219288L;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public ReferencedEntityNotFoundException(String message) {
        super(message);
    }
}
