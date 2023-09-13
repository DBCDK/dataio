package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "updateRequestType", propOrder = {
        "action",
        "recordIdentifier",
        "recordVersions",
        "record",
        "extraRequestData"
})
public class UpdateRequestType
        extends RequestType {

    @XmlElement(required = true)
    protected String action;
    protected String recordIdentifier;
    protected ArrayOfTns1RecordVersionType recordVersions;
    @XmlElement(required = true)
    protected RecordType record;
    @XmlElement(required = true)
    protected ExtraRequestDataType extraRequestData;

    public String getAction() {
        return action;
    }

    public void setAction(String value) {
        this.action = value;
    }

    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    public void setRecordIdentifier(String value) {
        this.recordIdentifier = value;
    }

    public ArrayOfTns1RecordVersionType getRecordVersions() {
        return recordVersions;
    }

    public void setRecordVersions(ArrayOfTns1RecordVersionType value) {
        this.recordVersions = value;
    }

    public RecordType getRecord() {
        return record;
    }

    public void setRecord(RecordType value) {
        this.record = value;
    }

    public ExtraRequestDataType getExtraRequestData() {
        return extraRequestData;
    }

    public void setExtraRequestData(ExtraRequestDataType value) {
        this.extraRequestData = value;
    }
}
