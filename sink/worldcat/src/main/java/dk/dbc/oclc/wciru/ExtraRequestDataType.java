package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "extraRequestDataType", propOrder = {
        "authenticationToken",
        "projectid",
        "editReplace"
})
public class ExtraRequestDataType {

    @XmlElement(required = true)
    protected String authenticationToken;
    @XmlElement(required = true)
    protected String projectid;
    @XmlElement(required = true, nillable = true)
    protected EditReplaceType editReplace;

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String value) {
        this.authenticationToken = value;
    }

    public String getProjectid() {
        return projectid;
    }

    public void setProjectid(String value) {
        this.projectid = value;
    }

    public EditReplaceType getEditReplace() {
        return editReplace;
    }

    public void setEditReplace(EditReplaceType value) {
        this.editReplace = value;
    }
}
