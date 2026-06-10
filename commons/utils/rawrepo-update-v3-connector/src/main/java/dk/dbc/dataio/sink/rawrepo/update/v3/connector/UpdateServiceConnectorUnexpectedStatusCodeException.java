package dk.dbc.dataio.sink.rawrepo.update.v3.connector;

public class UpdateServiceConnectorUnexpectedStatusCodeException extends UpdateServiceConnectorException {
    private final int statusCode;

    public UpdateServiceConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
