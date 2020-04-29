/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.cli;

public class CliException extends RuntimeException {
    public CliException(Throwable cause) {
        super(cause);
    }

    public CliException(String message, Throwable cause) {
        super(message, cause);
    }
}
