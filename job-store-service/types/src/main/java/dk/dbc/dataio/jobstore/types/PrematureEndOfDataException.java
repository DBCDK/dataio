package dk.dbc.dataio.jobstore.types;

public class PrematureEndOfDataException extends RuntimeException {
    public PrematureEndOfDataException() {
    }

    public PrematureEndOfDataException(Throwable cause) {
        super(cause);
    }
}
