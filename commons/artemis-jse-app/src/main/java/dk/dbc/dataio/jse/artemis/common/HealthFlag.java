package dk.dbc.dataio.jse.artemis.common;

public enum HealthFlag implements Health {
    STALE(402, "Timed out waiting for a message");

    public final int statusCode;
    public final String message;

    HealthFlag(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}
