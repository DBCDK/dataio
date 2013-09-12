package dk.dbc.dataio.gui.client.exceptions;

public enum JavaScriptProjectFetcherError {
    UNKNOWN,
    SCM_SERVER_ERROR,
    SCM_INVALID_URL,
    SCM_ILLEGAL_PROJECT_NAME,
    SCM_RESOURCE_NOT_FOUND,
    JAVASCRIPT_EVAL_ERROR,
    JAVASCRIPT_REFERENCE_ERROR,
    JAVASCRIPT_READ_ERROR,
}
