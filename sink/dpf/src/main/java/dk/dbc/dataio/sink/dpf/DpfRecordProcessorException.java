/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

class DpfRecordProcessorException extends Exception {
    DpfRecordProcessorException(String message, Exception cause) {
        super(message, cause);
    }
}
