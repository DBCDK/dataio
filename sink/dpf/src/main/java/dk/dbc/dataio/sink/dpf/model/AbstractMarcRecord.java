package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.util.List;
import java.util.stream.Collectors;

public class AbstractMarcRecord {

    MarcRecord body;

    public String getBibliographicRecordId() {
        return body.getSubFieldValue("001", 'a').orElse(null);
    }

    public String getPeriodicaType() {
        return body.getSubFieldValue("008", 'h').orElse(null);
    }

    public List<String> getCatalogueCodes() {
        return body.getSubFieldValues("032", 'a');
    }

    public String getOtherBibliographicRecordId() {
        return body.getSubFieldValue("018", 'a').orElse(null);
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

    public List<DataField> getCatalogueCodeFields() {
        return body.getFields(MarcRecord.hasTag("032")).stream().map(f -> (DataField) f).collect(Collectors.toList());
    }

}
