package dk.dbc.oclc.wciru;

/**
 * WciruClientException
 */
public class WciruClientException extends Exception {
    private Diagnostic diagnostic = null;

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public WciruClientException(String message) {
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
    public WciruClientException(String message, Exception cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * SRW diagnostic
     *
     * @param  message detail message saved for later retrieval
     *                 by the {@link #getMessage()} method). May be null.
     * @param diagnostic SRW diagnostic saved for later retrieval by the
     *                   {@link #getDiagnostic()} method). (A null value is
     *                   permitted).
     */
    public WciruClientException(String message, Diagnostic diagnostic) {
        super(message);
        this.diagnostic = diagnostic;
    }

    public Diagnostic getDiagnostic() {
        return diagnostic;
    }

    @Override
    public String toString() {
        String newline = System.getProperty("line.separator");
        String message = super.toString();
        return (diagnostic != null) ? (message + newline + diagnostic.toString()) : message;
    }
}
