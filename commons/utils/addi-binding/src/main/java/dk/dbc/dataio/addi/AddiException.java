package dk.dbc.dataio.addi;

public class AddiException extends Exception {
    private static final long serialVersionUID = -8182876691096952406L;

    /**
     * Constructs a new exception with the specified cause
     *
     * @param cause cause saved for later retrieval by the
     *              {@link #getCause()} method). (A null value is
     *              permitted, and indicates that the cause is nonexistent or
     *              unknown).
     */
    public AddiException(Exception cause) {
        super(cause.getMessage(), cause);
    }
}
