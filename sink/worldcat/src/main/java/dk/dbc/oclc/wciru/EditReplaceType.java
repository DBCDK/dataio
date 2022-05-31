package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "editReplaceType", propOrder = {
        "dataIdentifier",
        "oldValue",
        "newValue",
        "editReplaceType"
})
public class EditReplaceType {

    @XmlElement(required = true)
    protected String dataIdentifier;
    @XmlElement(required = true)
    protected String oldValue;
    @XmlElement(required = true)
    protected String newValue;
    @XmlElement(required = true)
    protected String editReplaceType;

    public String getDataIdentifier() {
        return dataIdentifier;
    }

    public void setDataIdentifier(String value) {
        this.dataIdentifier = value;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String value) {
        this.oldValue = value;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String value) {
        this.newValue = value;
    }

    public String getEditReplaceType() {
        return editReplaceType;
    }

    public void setEditReplaceType(String value) {
        this.editReplaceType = value;
    }
}
