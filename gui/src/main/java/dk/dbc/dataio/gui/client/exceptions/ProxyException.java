package dk.dbc.dataio.gui.client.exceptions;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ProxyException extends Exception implements IsSerializable {
    private static final long serialVersionUID = -93191478556372060L;
    private /* final */ ProxyError errorCode;

    public ProxyException() {
        this.errorCode = ProxyError.INTERNAL_SERVER_ERROR;
    }

    public ProxyException(final ProxyError errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public ProxyException(final ProxyError errorCode, final String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public ProxyException(final ProxyError errorCode) {
        this.errorCode = errorCode;
    }

    public ProxyError getErrorCode() {
        return errorCode;
    }
}
