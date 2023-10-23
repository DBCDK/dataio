package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import freemarker.template.Configuration;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DynamicList extends ResolvingObject {
    @Override
    @XmlElement(name = "p")
    public List<Property> getProperties() {
        return properties;
    }

    public DynamicList() {}

    public DynamicList clone(ResolvingObject parent) {
        DynamicList dl = new DynamicList();
        dl.name = name;
        dl.properties = new ArrayList<>(properties);
        Map<String, ValueResolver> locals = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));
        dl.map.putAll(parent.map);
        dl.map.putAll(locals);
        dl.namespace = namespace;
        dl.parent = parent;
        return dl;
    }

    @Override
    public void process(Set<String> deployNames, Namespace namespace, ResolvingObject parent, Configuration configuration, String templateDir) throws IOException {

    }

    @Override
    public boolean isEnabled(Set<String> deployNames, Namespace ns) {
        return true;
    }

    @Override
    public boolean isResolving() {
        return parent.isResolving();
    }


}
