package dk.dbc.dataio.commons.types.exceptions;

public abstract class ServiceException extends Exception {
    private static final long serialVersionUID = -6416715111557513548L;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Exception cause) {
        super(message, cause);
    }
}

