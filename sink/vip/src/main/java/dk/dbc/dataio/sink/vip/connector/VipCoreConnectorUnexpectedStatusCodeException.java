/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.vip.connector;

import java.util.Optional;

public class VipCoreConnectorUnexpectedStatusCodeException extends VipCoreConnectorException {
    private int statusCode;
    private VipCoreConnector.Error error;

    /**
     * Constructs a new exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     * @param statusCode the http statusCode code returned by the REST service
     */
    public VipCoreConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Optional<VipCoreConnector.Error> getError() {
        return Optional.ofNullable(error);
    }

    public void setError(VipCoreConnector.Error error) {
        this.error = error;
    }
}
