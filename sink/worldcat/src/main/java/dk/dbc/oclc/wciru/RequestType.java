package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "requestType", namespace = "http://www.loc.gov/zing/srw/", propOrder = {
        "version"
})
@XmlSeeAlso({
        UpdateRequestType.class,
        ExplainRequestType.class
})
public class RequestType {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String value) {
        this.version = value;
    }
}
