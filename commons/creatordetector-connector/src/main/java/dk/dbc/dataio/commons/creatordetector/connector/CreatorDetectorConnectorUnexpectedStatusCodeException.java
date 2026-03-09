package dk.dbc.dataio.commons.creatordetector.connector;

public class CreatorDetectorConnectorUnexpectedStatusCodeException extends CreatorDetectorConnectorException {
    private final int statusCode;

    public CreatorDetectorConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
