/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.dataio.sink.dpf.MarcRecordFactory;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.Applicant;
import dk.dbc.lobby.ApplicantState;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.util.List;

public class DpfRecord extends AbstractMarcRecord {
    public enum State {
        MODIFIED, NEW, UNKNOWN
    }

    private final ProcessingInstructions processingInstructions;

    public DpfRecord(ProcessingInstructions processingInstructions, MarcRecord body) {
        this.processingInstructions = processingInstructions;
        this.body = body;
        processingInstructions.getErrors().forEach(this::addErrorToBody);
    }

    public String getId() {
        return processingInstructions.getId();
    }

    public List<String> getErrors() {
        return processingInstructions.getErrors();
    }

    public boolean hasErrors() {
        return !processingInstructions.getErrors().isEmpty();
    }

    public ProcessingInstructions getProcessingInstructions() {
        return processingInstructions;
    }

    public MarcRecord getBody() {
        return body;
    }

    public Applicant toLobbyApplicant() throws JSONBException {
        final Applicant applicant = new Applicant();
        applicant.setId(getId());
        applicant.setCategory("dpf");
        applicant.setMimetype("application/xml");
        applicant.setBody(MarcRecordFactory.toMarcXchange(body));
        applicant.setState(ApplicantState.PENDING);
        applicant.setAdditionalInfo(processingInstructions);
        return applicant;
    }

    public String getDPFCode() {
        return getSubfieldValue("032", 'b');
    }

    public void removeDPFCode() {
        removeSubfield("032", 'b');
        removeSubfield("032", 'c');
    }

    public void setCatalogueCode(String value) {
        setSubfieldValue("032", 'a', value);
    }

    public void addError(String errorMessage) {
        processingInstructions.getErrors().add(errorMessage);
        addErrorToBody(errorMessage);
    }

    private void addErrorToBody(String error) {
        body.addField(new DataField("e99", "00")
                .addSubField(new SubField()
                        .setCode('b').setData(error)));
    }
}
