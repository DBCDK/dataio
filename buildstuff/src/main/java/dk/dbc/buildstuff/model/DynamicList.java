package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.TokenResolver;
import dk.dbc.buildstuff.ValueResolver;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamicList extends NamedBaseObject implements TokenResolver {
    @XmlElement(name = "p")
    private List<Property> properties;
//    @XmlElement
//    private DynamicList list;

    public Map<String, ValueResolver> getValues(Namespace ns) {
        return properties.stream().collect(Collectors.toMap(p -> p.name, p -> p.getValue(ns)));
    }

    public List<Property> getProperties() {
        return properties;
    }

    @Override
    public boolean resolve(Map<String, ValueResolver> scope) {
        return false;
    }
}
