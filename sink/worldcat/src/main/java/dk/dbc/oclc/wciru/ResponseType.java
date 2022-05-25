package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

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
