package dk.dbc.dataio.gui.client.exceptions;

import java.io.Serializable;

public class JavaScriptProjectFetcherException extends Exception implements Serializable {
    private static final long serialVersionUID = 6907428727283491685L;

    public JavaScriptProjectFetcherException() { }

    public JavaScriptProjectFetcherException(final Throwable cause) {
        super(cause);
    }

    public JavaScriptProjectFetcherException(final String errorMsg) {
        super(errorMsg);
    }
}
