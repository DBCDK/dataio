package dk.dbc.dataio.commons.types;

import java.io.Serializable;

public class ServiceError implements Serializable {
    private static final long serialVersionUID = -7949904926077016654L;

    private /* final */ String message;
    private /* final */ String details;

    private ServiceError() { }

    public ServiceError(String message, String details) {
        this.message = message;
        this.details = details;
    }

    public ServiceError(String message) {
        this(message, "");
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}
