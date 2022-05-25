package dk.dbc.dataio.commons.javascript;

import java.io.Serializable;

public class JavaScriptProjectException extends Exception implements Serializable {
    private static final long serialVersionUID = 6907428727283491685L;
    private /* final */ JavaScriptProjectError errorCode;

    public JavaScriptProjectException() {
        this.errorCode = JavaScriptProjectError.UNKNOWN;
    }

    public JavaScriptProjectException(final JavaScriptProjectError errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public JavaScriptProjectException(final JavaScriptProjectError errorCode, final String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public JavaScriptProjectError getErrorCode() {
        return errorCode;
    }
}
