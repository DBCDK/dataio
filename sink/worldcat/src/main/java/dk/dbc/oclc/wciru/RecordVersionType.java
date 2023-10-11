package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "recordVersionType", propOrder = {
        "versionType",
        "versionValue"
})
public class RecordVersionType {

    @XmlElement(required = true)
    protected String versionType;
    @XmlElement(required = true)
    protected String versionValue;

    public String getVersionType() {
        return versionType;
    }

    public void setVersionType(String value) {
        this.versionType = value;
    }

    public String getVersionValue() {
        return versionValue;
    }

    public void setVersionValue(String value) {
        this.versionValue = value;
    }
}
