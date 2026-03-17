package dk.dbc.dataio.commons.retriever.connector;

public class RetrieverConnectorUnexpectedStatusCodeException extends RetrieverConnectorException {
    private final int statusCode;

    public RetrieverConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
