/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore.types;

public class InvalidRecordException extends Exception {
    public InvalidRecordException(String message) {
        super(message);
    }

    public InvalidRecordException(Throwable cause) {
        super(cause);
    }

    public InvalidRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
