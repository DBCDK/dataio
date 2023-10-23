package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import freemarker.template.Configuration;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@XmlRootElement
@XmlType(propOrder = {"mappings", "version", "deployments", "global"})
public class Application {
    @XmlElementWrapper(name = "namespaces", required = true)
    @XmlElement(name = "map", required = true)
    private Set<Namespace> mappings;
    @XmlElement(required = true)
    private Version version;
    @XmlElementRefs({
            @XmlElementRef(name = "deploy", type=Deploy.class),
            @XmlElementRef(name = "alert", type=Alert.class),
            @XmlElementRef(name = "defaults", type=ScopedDefaults.class)
    })
    private List<ResolvingObject> deployments;
    @XmlElementWrapper(name = "global")
    @XmlElement(name = "p")
    private List<Property> global;
    @XmlAttribute(name = "template-dir")
    private String templateDir;

    public void setupResolvers(Set<String> deployNames, Namespace ns, Map<String, ValueResolver> scope) {
        for (ResolvingObject deployment : deployments) deployment.setupResolvers(deployNames, ns, scope);
    }

    public void process(Set<String> deployNames, Namespace namespace, Configuration configuration) throws IOException {
        for (ResolvingObject deployment : deployments) deployment.process(deployNames, namespace, null, configuration, templateDir);
    }

    public Map<String, ValueResolver> getGlobalValues(Namespace ns) {
        return Stream.concat(Stream.of(version), global.stream()).collect(Collectors.toMap(NamedBaseObject::getName, p -> p.getValue(ns, null)));
    }

    public Set<Namespace> getMappings() {
        return mappings;
    }

    public List<ResolvingObject> getDeployments() {
        return deployments;
    }

    public List<Property> getGlobal() {
        return global;
    }

    public Property getVersion() {
        return version;
    }

    public List<Deploy> getEnabledDeployments(Namespace namespace, Set<String> deployments) {
        return this.deployments.stream().flatMap(r -> r.getDeployments(deployments, namespace)).collect(Collectors.toList());
    }
}
