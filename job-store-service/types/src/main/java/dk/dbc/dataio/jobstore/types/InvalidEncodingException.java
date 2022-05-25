package dk.dbc.dataio.jobstore.types;

public class InvalidEncodingException extends UnrecoverableDataException {
    private static final long serialVersionUID = 7489234313906179881L;

    public InvalidEncodingException(String message) {
        super(message);
    }

    public InvalidEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
