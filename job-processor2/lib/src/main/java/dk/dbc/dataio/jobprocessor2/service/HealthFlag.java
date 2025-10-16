package dk.dbc.dataio.jobprocessor2.service;


import dk.dbc.dataio.jse.artemis.common.Health;

public enum HealthFlag implements Health {
    OUT_OF_MEMORY(400, "Out of memory"),
    TIMEOUT(401, "JavaScript exceeded its execution time"),
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
