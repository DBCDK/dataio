package dk.dbc.dataio.sink.rawrepo.update.v3.connector;

public class UpdateServiceConnectorException extends Exception {
    public UpdateServiceConnectorException(String message) {
        super(message);
    }

    public UpdateServiceConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
