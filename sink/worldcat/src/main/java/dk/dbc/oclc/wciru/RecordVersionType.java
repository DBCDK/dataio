package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

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
