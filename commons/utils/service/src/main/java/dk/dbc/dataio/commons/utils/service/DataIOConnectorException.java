package dk.dbc.dataio.commons.utils.service;

public class DataIOConnectorException extends Exception {
    private static final long serialVersionUID = 1L;

    public DataIOConnectorException(String message) {
        super(message);
    }

    public DataIOConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
