/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.lobby.Applicant;

public class DpfRecord {
    public enum State {
        MODIFIED, NEW, UNKNOWN
    }

    private final ProcessingInstructions processingInstructions;
    private final byte[] body;

    public DpfRecord(ProcessingInstructions processingInstructions, byte[] body) {
        this.processingInstructions = processingInstructions;
        this.body = body;
    }

    public Applicant toLobbyApplicant() {
        return null;
    }
}
