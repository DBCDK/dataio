package dk.dbc.dataio.jobstore.types;

public class DataException extends RuntimeException {
    private static final long serialVersionUID = -7490724186211549653L;

    public DataException(String message) {
        super(message);
    }

    public DataException(Throwable cause) {
        super(cause);
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
