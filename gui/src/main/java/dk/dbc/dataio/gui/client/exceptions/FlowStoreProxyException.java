package dk.dbc.dataio.gui.client.exceptions;

import java.io.Serializable;

public class FlowStoreProxyException extends Exception implements Serializable {
    private static final long serialVersionUID = -93191478556372060L;
    private /* final */ FlowStoreProxyError errorCode;

    public FlowStoreProxyException() {
        this.errorCode = FlowStoreProxyError.UNKNOWN;
    }

    public FlowStoreProxyException(final FlowStoreProxyError errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public FlowStoreProxyException(final FlowStoreProxyError errorCode, final String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public FlowStoreProxyError getErrorCode() {
        return errorCode;
    }
}
