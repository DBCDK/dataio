package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "explainRequestType", propOrder = {
        "recordPacking",
        "extraRequestData"
})
public class ExplainRequestType
        extends RequestType {

    protected String recordPacking;
    protected ExtraDataType extraRequestData;

    public String getRecordPacking() {
        return recordPacking;
    }

    public void setRecordPacking(String value) {
        this.recordPacking = value;
    }

    public ExtraDataType getExtraRequestData() {
        return extraRequestData;
    }

    public void setExtraRequestData(ExtraDataType value) {
        this.extraRequestData = value;
    }
}
