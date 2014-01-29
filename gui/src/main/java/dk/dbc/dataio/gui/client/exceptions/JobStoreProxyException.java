package dk.dbc.dataio.gui.client.exceptions;

import java.io.Serializable;

public class JobStoreProxyException extends Exception implements Serializable {
    private static final long serialVersionUID = -93191478556372061L;
    private /* final */ JobStoreProxyError errorCode;

    public JobStoreProxyException() {
        this.errorCode = JobStoreProxyError.INTERNAL_SERVER_ERROR;
    }

    public JobStoreProxyException(final JobStoreProxyError errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public JobStoreProxyException(final JobStoreProxyError errorCode, final String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public JobStoreProxyError getErrorCode() {
        return errorCode;
    }
}
