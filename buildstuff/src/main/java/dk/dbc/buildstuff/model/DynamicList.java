package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import freemarker.template.Configuration;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynamicList extends ResolvingObject {
    @Override
    @XmlElement(name = "p")
    public List<Property> getProperties() {
        return properties;
    }
    @XmlAttribute
    private String include;
    @XmlAttribute
    private String exclude;

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

    public Set<String> getInclude() {
        if(include == null || include.isEmpty()) return Set.of();
        return new HashSet<>(Arrays.asList(include.split(LIST_SPLITTER)));
    }

    public Set<String> getExclude() {
        if(exclude == null || exclude.isEmpty()) return Set.of();
        return new HashSet<>(Arrays.asList(exclude.split(LIST_SPLITTER)));
    }

    @Override
    public boolean isEnabled(Set<String> deployNames, Namespace ns) {
        return getFilter(ns);
    }

    @Override
    public Stream<Deploy> getDeployments(Set<String> deployNames, Namespace namespace) {
        return Stream.of();
    }
}
