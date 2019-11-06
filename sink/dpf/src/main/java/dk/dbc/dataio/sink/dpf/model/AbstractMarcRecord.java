package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

public class AbstractMarcRecord {

    MarcRecord body;

    public String getBibliographicRecordId() {
        return getSubfieldValue("001", 'a');
    }

    public String getPeriodicaType() {
        return getSubfieldValue("008", 'h');
    }

    public String getCatalogueCode() {
        return getSubfieldValue("032", 'a');
    }

    public String getOtherBibliographicRecordId() {
        return getSubfieldValue("018", 'a');
    }

    String getSubfieldValue(String tag, char code) {
        for (Field field : body.getFields()) {
            if (tag.equals(field.getTag())) {
                DataField dataField = (DataField) field;
                for (SubField subField : dataField.getSubFields()) {
                    if (code == subField.getCode()) {
                        return subField.getData();
                    }
                }
            }
        }

        return null;
    }

    void setSubfieldValue(String tag, char code, String value) {
        DataField dataField = null;

        for (Field field : body.getFields()) {
            if (tag.equals(field.getTag())) {
                dataField = (DataField) field;
            }
        }

        if (dataField == null) {
            dataField = new DataField(tag, "00");
            body.addField(dataField);
        }

        dataField.addOrReplaceFirstSubField(new SubField(code, value));
    }

    void addDataField(String tag, char code, String value) {
        final SubField subField = new SubField(code, value);
        final DataField dataField = new DataField(tag, "00");
        dataField.addSubField(subField);

        body.addField(dataField);
    }

    void removeSubfield(String tag, char code) {
        for (Field field : body.getFields()) {
            if (tag.equals(field.getTag())) {
                DataField dataField = (DataField) field;
                dataField.removeSubField(code);
            }
        }
    }

}
