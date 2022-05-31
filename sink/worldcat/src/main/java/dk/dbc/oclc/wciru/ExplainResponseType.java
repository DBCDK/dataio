package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "explainResponseType", propOrder = {
        "record",
        "diagnostics",
        "extraResponseData"
})
public class ExplainResponseType
        extends ResponseType {

    @XmlElement(required = true)
    protected RecordType record;
    protected DiagnosticsType diagnostics;
    protected ExtraDataType extraResponseData;

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
