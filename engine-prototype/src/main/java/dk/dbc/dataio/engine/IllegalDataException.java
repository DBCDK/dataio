package dk.dbc.dataio.engine;

/**
 * Exception for signalling that the input data contained errors. This is a
 * {@link RuntimeException} for use in the {@link DefaultXMLRecordSplitter}s
 * Iterator, since the {@link Iterable} interface does not allow checked
 * exceptions to be thrown. This Exception will therefore always be a wrapper
 * for another exception.
 */
public class IllegalDataException extends RuntimeException {

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
