package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import freemarker.template.Configuration;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XmlRootElement
@XmlSeeAlso({Alert.class, Deploy.class, ScopedDefaults.class})
public abstract class ResolvingObject extends NamedBaseObject {
    protected List<Property> properties = new ArrayList<>();
    protected Map<String, ValueResolver> map = new HashMap<>();
    private Set<String> unresolved;
    protected Namespace namespace;
    protected List<DynamicList> list = new ArrayList<>();
    protected ResolvingObject parent;

    public ResolvingObject() {}

    public List<DynamicList> getList() {
        if(list == null) return List.of();
        return list;
    }

    protected Stream<DynamicList> getListsInScope() {
        if(parent == null) return list.stream();
        return Stream.concat(list.stream(), parent.getListsInScope());
    }

    public List<Property> getProperties() {
        return properties;
    }

    public abstract void process(Set<String> deployNames, Namespace namespace, ResolvingObject parent, Configuration configuration, String templateDir) throws IOException;

    public abstract boolean isEnabled(Set<String> deployNames, Namespace ns);

    protected void resolve(int loopMax) {
        if (loopMax < 1) throw new IllegalStateException("Token resolver exceeded its maximum attempts while resolving deployment "
                + name + ". Please ensure that you have no looping references in these variables " + unresolved);
        Set<String> done = new HashSet<>();
        for (String s : unresolved) {
            ValueResolver valueResolver = map.get(s);
            if (valueResolver.resolve(map)) done.add(s);
        }
        unresolved.removeAll(done);
        if (!unresolved.isEmpty()) resolve(loopMax - 1);
    }

    protected Map<String, ValueResolver> getResolverMap(Namespace namespace, Map<String, ValueResolver> scope) {
        Map<String, ValueResolver> resolverMap = properties.stream().collect(Collectors.toMap(l -> l.name, l -> l.getValue(namespace, scope.get(l.name))));
        HashMap<String, ValueResolver> locals = new HashMap<>();
        scope.forEach((key, value) -> locals.put(key, value.copy()));
        locals.putAll(resolverMap);
        locals.put("name", new ValueResolver(name));
        return locals;
    }

    protected void resolveTokens(String context) {
        try {
            unresolved = map.entrySet().stream()
                    .filter(e -> e.getValue().hasTokens(name, e.getKey(), namespace))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            resolve(20);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Caught exception while resolving " + context, e);
        }
    }

    public boolean setupResolvers(Set<String> deployNames, Namespace namespace, Map<String, ValueResolver> scope) {
        this.namespace = namespace;
        if(!isEnabled(deployNames, namespace)) return false;
        map = getResolverMap(namespace, scope);
        list.forEach(l -> l.setupResolvers(deployNames, namespace, this));
        return true;
    }

    public boolean setupResolvers(Set<String> deployNames, Namespace namespace, ResolvingObject parent) {
        this.parent = parent;
        return setupResolvers(deployNames, namespace, parent.map);
    }

    public abstract Stream<Deploy> getDeployments(Set<String> deployNames, Namespace namespace);
}
