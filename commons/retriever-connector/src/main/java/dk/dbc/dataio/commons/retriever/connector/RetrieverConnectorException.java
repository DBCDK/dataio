package dk.dbc.dataio.commons.retriever.connector;

public class RetrieverConnectorException extends Exception {
    public RetrieverConnectorException(String message) {
        super(message);
    }

    public RetrieverConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
