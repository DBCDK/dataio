package dk.dbc.dataio.gui.client.exceptions;

import java.io.Serializable;

public class JavaScriptProjectFetcherException extends Exception implements Serializable {
    private static final long serialVersionUID = 6907428727283491685L;
    private /* final */ JavaScriptProjectFetcherError errorCode;

    public JavaScriptProjectFetcherException() {
        this.errorCode = JavaScriptProjectFetcherError.UNKNOWN;
    }

    public JavaScriptProjectFetcherException(final JavaScriptProjectFetcherError errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public JavaScriptProjectFetcherException(final JavaScriptProjectFetcherError errorCode, final String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public JavaScriptProjectFetcherError getErrorCode() {
        return errorCode;
    }
}
