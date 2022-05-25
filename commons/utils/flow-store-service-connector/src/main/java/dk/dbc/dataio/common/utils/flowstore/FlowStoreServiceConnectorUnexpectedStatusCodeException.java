package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.FlowStoreError;

public class FlowStoreServiceConnectorUnexpectedStatusCodeException extends FlowStoreServiceConnectorException {

    private static final long serialVersionUID = -6137268696536418984L;
    private FlowStoreError flowStoreError;
    private int statusCode;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message    detail message saved for later retrieval by the
     *                   {@link #getMessage()} method. May be null.
     * @param statusCode the http statusCode code returned by the REST service
     */
    public FlowStoreServiceConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * @return the statusCode code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the flow store error
     */
    public FlowStoreError getFlowStoreError() {
        return flowStoreError;
    }

    /**
     * Sets the job error
     *
     * @param flowStoreError the flow store error to set
     */
    public void setFlowStoreError(FlowStoreError flowStoreError) {
        this.flowStoreError = flowStoreError;
    }
}
