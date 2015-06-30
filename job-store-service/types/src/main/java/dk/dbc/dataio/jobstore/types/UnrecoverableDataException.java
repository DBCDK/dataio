package dk.dbc.dataio.jobstore.types;

public class UnrecoverableDataException extends RuntimeException {
    private static final long serialVersionUID = -7490724186211549653L;

    public UnrecoverableDataException(String message) {
        super(message);
    }

    public UnrecoverableDataException(Throwable cause) {
        super(cause);
    }

    public UnrecoverableDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
