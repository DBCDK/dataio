package dk.dbc.buildstuff.model;

import jakarta.xml.bind.annotation.XmlAttribute;

public class Namespace {
    @XmlAttribute(name = "short")
    private String shortName;
    @XmlAttribute
    private String namespace;

    public String getShortName() {
        return shortName;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return "Namespace{" +
                "shortName='" + shortName + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
