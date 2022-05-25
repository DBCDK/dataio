package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import java.math.BigInteger;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "recordType", namespace = "http://www.loc.gov/zing/srw/", propOrder = {
        "recordSchema",
        "recordPacking",
        "recordData",
        "recordPosition",
        "extraRecordData"
})
public class RecordType {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected String recordSchema;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected String recordPacking;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected StringOrXmlFragment recordData;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger recordPosition;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    protected ExtraDataType extraRecordData;

    public String getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(String value) {
        this.recordSchema = value;
    }

    public String getRecordPacking() {
        return recordPacking;
    }

    public void setRecordPacking(String value) {
        this.recordPacking = value;
    }

    public StringOrXmlFragment getRecordData() {
        return recordData;
    }

    public void setRecordData(StringOrXmlFragment value) {
        this.recordData = value;
    }

    public BigInteger getRecordPosition() {
        return recordPosition;
    }

    public void setRecordPosition(BigInteger value) {
        this.recordPosition = value;
    }

    public ExtraDataType getExtraRecordData() {
        return extraRecordData;
    }

    public void setExtraRecordData(ExtraDataType value) {
        this.extraRecordData = value;
    }
}
