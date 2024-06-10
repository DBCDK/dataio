package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessorException;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.util.List;

@SuppressWarnings("rawtypes")
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

        for (Field<?> field : body.getFields()) {
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

    public void addField(Field<?> field) {
        body.addField(field);
    }

    public List<Field> getFields(String field) {
        return body.getFields(MarcRecord.hasTag(field));
    }

    public DataField getCatalogueCodeField() throws DpfRecordProcessorException {
        return (DataField) body.getField(MarcRecord.hasTag("032"))
                .orElseThrow(() -> new DpfRecordProcessorException("Record " + getBibliographicRecordId() + " has no catalogue code field"));
    }

    public List<Field> getFieldsWithContent(String fieldName, Character subfieldName) {
        MarcRecord mm = new MarcRecord();

        for (Field field : body.getFields()) {
            if (fieldName.equals(field.getTag())) {
                List<SubField> subfields = ((DataField) field).getSubFields();
                for (SubField subField : subfields) {
                    if (subfieldName.equals(subField.getCode())) {
                        mm.addField(field);
                        break;
                    }
                }
            }
        }
        return mm.getFields();
    }
}
