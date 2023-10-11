package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "updateResponseType", propOrder = {
        "operationStatus",
        "recordIdentifier",
        "recordVersions",
        "record",
        "diagnostics",
        "extraResponseData"
})
public class UpdateResponseType
        extends ResponseType {
    @XmlElement(required = true, namespace = "http://www.loc.gov/zing/srw/update/")
    protected OperationStatusType operationStatus;
    @XmlElement(required = false, namespace = "http://www.loc.gov/zing/srw/update/")
    protected String recordIdentifier;
    protected ArrayOfTns1RecordVersionType recordVersions;
    @XmlElement(required = true, namespace = "http://www.loc.gov/zing/srw/")
    protected RecordType record;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    protected DiagnosticsType diagnostics;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    protected ExtraDataType extraResponseData;

    public OperationStatusType getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatusType value) {
        this.operationStatus = value;
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

    public DiagnosticsType getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(DiagnosticsType value) {
        this.diagnostics = value;
    }

    public ExtraDataType getExtraResponseData() {
        return extraResponseData;
    }

    public void setExtraResponseData(ExtraDataType value) {
        this.extraResponseData = value;
    }
}
