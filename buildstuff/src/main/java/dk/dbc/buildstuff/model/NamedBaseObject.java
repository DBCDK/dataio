package dk.dbc.buildstuff.model;

import jakarta.xml.bind.annotation.XmlAttribute;

public class NamedBaseObject {
    @XmlAttribute
    protected String name;

    public String getName() {
        return name;
    }
}
