package dk.dbc.dataio.engine;

public class IllegalDataException extends RuntimeException {

    public IllegalDataException() {
    }

    public IllegalDataException(String message) {
        super(message);
    }

    public IllegalDataException(Throwable cause) {
        super(cause);
    }

    public IllegalDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
