package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.dataio.sink.dpf.transform.MarcRecordFactory;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.Applicant;
import dk.dbc.lobby.ApplicantState;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.util.List;
import java.util.stream.Collectors;

public class DpfRecord extends AbstractMarcRecord {
    public enum State {
        MODIFIED, NEW, CLOSED, UNKNOWN
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

    public void setBibliographicRecordId(String bibliographicRecordId) {
        setSubfieldValue("001", 'a', bibliographicRecordId);
    }

    public void setOtherBibliographicRecordId(String bibliographicRecordId) {
        setSubfieldValue("018", 'a', bibliographicRecordId);
    }

    public String getPeriodicaType() {
        return body.getSubFieldValue("008", 'h').orElse(null);
    }

    public String getDPFHeadBibliographicRecordId() {
        for (Field field : body.getFields()) {
            if ("035".equals(field.getTag())) {
                DataField dataField = (DataField) field;
                for (SubField subField : dataField.getSubFields()) {
                    if ('a' == subField.getCode() && subField.getData().startsWith("(DPFHOVED)")) {
                        return subField.getData().substring(10);
                    }
                }
            }
        }

        return null;
    }

    public void addSystemControlNumber(String systemControlNumber) {
        addDataField("035", 'a', systemControlNumber);
    }

    public void setCatalogueCodeField(DataField dataField) {
        if (dataField != null) {
            body.removeField("032");
            body.addField(dataField);
        }
    }

    public void addError(DataField field) {
        processingInstructions.getErrors().add(field
                .getSubFields().stream()
                .map(SubField::getData)
                .collect(Collectors.joining(", ")));
        body.addField(field);
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
