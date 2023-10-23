package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;
import java.util.stream.Collectors;

public class Property extends NamedBaseObject {
    @XmlAttribute(name = "default")
    private String defaultValue;
    @XmlElement
    private List<EnvValue> env;

    public ValueResolver getValue(Namespace ns, ValueResolver parent) {
        if(env == null) return new ValueResolver(defaultValue);
        List<String> list = env.stream().filter(e -> ns.getShortName().equals(e.getNs())).map(EnvValue::getValue).collect(Collectors.toList());
        if(list.size() > 1) throw new IllegalStateException("Property " + name + " must be unique in its scope");
        String value = list.stream().findFirst().orElse(defaultValue);
        if(value == null && parent != null) return parent.copy();
        return new ValueResolver(value);
    }

    @Override
    public String toString() {
        return "Property{" +
                "name='" + name + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                "} ";
    }
}
