package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "diagnostic", namespace = "http://www.loc.gov/zing/srw/diagnostic/", propOrder = {
        "uri",
        "details",
        "message"
})
public class Diagnostic {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/diagnostic/", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String uri;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/diagnostic/")
    protected String details;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/diagnostic/")
    protected String message;

    public String getUri() {
        return uri;
    }

    public void setUri(String value) {
        this.uri = value;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String value) {
        this.details = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String value) {
        this.message = value;
    }
}
