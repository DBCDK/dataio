package dk.dbc.dataio.jobstore.types;

import java.util.NoSuchElementException;

public class PrematureEndOfDataException extends NoSuchElementException {
    public PrematureEndOfDataException() {
    }

    public PrematureEndOfDataException(Throwable cause) {
        initCause(cause);
    }
}
