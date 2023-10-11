package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "responseType", namespace = "http://www.loc.gov/zing/srw/", propOrder = {
        "version"
})
@XmlSeeAlso({
        ExplainResponseType.class,
        UpdateResponseType.class
})
public class ResponseType {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String value) {
        this.version = value;
    }
}
