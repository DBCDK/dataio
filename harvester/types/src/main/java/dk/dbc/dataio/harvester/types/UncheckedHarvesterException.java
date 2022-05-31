package dk.dbc.dataio.harvester.types;

public class UncheckedHarvesterException extends RuntimeException {
    public UncheckedHarvesterException(Exception cause) {
        super(cause);
    }

    public UncheckedHarvesterException(String message) {
        super(message);
    }

    public UncheckedHarvesterException(String message, Exception cause) {
        super(message, cause);
    }
}
