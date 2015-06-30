package dk.dbc.dataio.jobstore.types;

public class InvalidDataException extends UnrecoverableDataException {
    private static final long serialVersionUID = 6406566155381131155L;

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(Throwable cause) {
        super(cause);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
