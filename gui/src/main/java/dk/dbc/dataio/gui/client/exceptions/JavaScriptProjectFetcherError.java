package dk.dbc.dataio.gui.client.exceptions;

public enum JavaScriptProjectFetcherError {
    UNKNOWN(0),
    SCM_SERVER_ERROR(1),
    SCM_INVALID_URL(2),
    SCM_ILLEGAL_PROJECT_NAME(3),
    SCM_RESOURCE_NOT_FOUND(4),
    JAVASCRIPT_EVAL_ERROR(11),
    JAVASCRIPT_FAKE_USE_REFERENCE_ERROR(12),
    JAVASCRIPT_READ_ERROR(13);

    private /* final */ int number;

    private JavaScriptProjectFetcherError(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}
