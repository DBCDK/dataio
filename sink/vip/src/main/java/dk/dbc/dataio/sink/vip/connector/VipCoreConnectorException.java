/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.vip.connector;

public class VipCoreConnectorException extends Exception {
    public VipCoreConnectorException(String message) {
        super(message);
    }

    public VipCoreConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
