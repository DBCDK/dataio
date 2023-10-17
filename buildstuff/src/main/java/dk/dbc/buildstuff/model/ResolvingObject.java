package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ResolvingObject extends NamedBaseObject {
    @XmlElement(name = "p")
    protected List<Property> properties = new ArrayList<>();
    Map<String, ValueResolver> map;
    private Set<String> unresolved;
    protected Namespace namespace;

    public List<Property> getProperties() {
        return properties;
    }

    protected void resolve(int loopMax) {
        if (loopMax < 1)
            throw new IllegalStateException("Token resolver exceeded its maximum attempts while resolving deployment " + name + ". Please ensure that you have no looping references in these variables " + unresolved);
        Set<String> done = new HashSet<>();
        for (String s : unresolved) {
            ValueResolver valueResolver = map.get(s);
            if (valueResolver.resolve(map)) done.add(s);
        }
        unresolved.removeAll(done);
        if (!unresolved.isEmpty()) resolve(loopMax - 1);
    }

    protected void resolveTokens(String context, Namespace namespace, Map<String, ValueResolver> scope) {
        this.namespace = namespace;
        try {
            Map<String, ValueResolver> localValues = properties.stream()
                    .filter(l -> l.getValue(namespace).getValue() != null)
                    .collect(Collectors.toMap(l -> l.name, l -> l.getValue(namespace)));
            map = new HashMap<>();
            scope.forEach((key, value) -> map.put(key, value.copy()));
            map.putAll(localValues);
            map.put("name", new ValueResolver(name));
            unresolved = map.entrySet().stream()
                    .filter(e -> e.getValue().hasTokens(name, e.getKey(), namespace))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            resolve(20);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Caught exception while resolving " + context, e);
        }
    }
}
