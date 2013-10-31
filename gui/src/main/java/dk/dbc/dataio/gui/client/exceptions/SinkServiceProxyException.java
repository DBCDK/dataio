package dk.dbc.dataio.gui.client.exceptions;

import java.io.Serializable;

public class SinkServiceProxyException extends Exception implements Serializable {
    private static final long serialVersionUID = 2016833836283253195L;
    private /* final */ SinkServiceProxyError errorCode;

    public SinkServiceProxyException() {
        this.errorCode = SinkServiceProxyError.INTERNAL_SERVER_ERROR;
    }

    public SinkServiceProxyException(final SinkServiceProxyError errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public SinkServiceProxyException(final SinkServiceProxyError errorCode, final String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public SinkServiceProxyError getErrorCode() {
        return errorCode;
    }
}
