package dk.dbc.buildstuff.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

public class EnvValue {
    @XmlAttribute
    private String ns;
    @XmlValue
    private String value;

    public String getNs() {
        return ns;
    }

    public String getValue() {
        return value;
    }
}
