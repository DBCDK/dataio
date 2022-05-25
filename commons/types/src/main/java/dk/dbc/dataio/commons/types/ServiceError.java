package dk.dbc.dataio.commons.types;

import java.io.Serializable;

public class ServiceError implements Serializable {
    private static final long serialVersionUID = -7949904926077016654L;

    private /* final */ String message;
    private /* final */ String details;
    private /* final */ String stacktrace;

    public ServiceError() {
    }

    public ServiceError withMessage(String message) {
        this.message = message;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ServiceError withDetails(String details) {
        this.details = details;
        return this;
    }

    public String getDetails() {
        return details;
    }

    public ServiceError withStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
        return this;
    }

    public String getStacktrace() {
        return stacktrace;
    }

}
