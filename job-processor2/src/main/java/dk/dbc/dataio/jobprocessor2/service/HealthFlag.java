package dk.dbc.dataio.jobprocessor2.service;

public enum HealthFlag {
    OUT_OF_MEMORY(400, "Out of memory"),
    TIMEOUT(401, "JavaScript exceeded its execution time"),
    STALE(402, "Timed out waiting for a message");

    public final int statusCode;
    public final String message;

    HealthFlag(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
