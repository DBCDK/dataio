package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
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
    @XmlElementWrapper(name = "namespaces")
    @XmlElement(name = "map")
    private Set<Namespace> mappings;
    @XmlElement
    private Property version;
    @XmlElementRefs({@XmlElementRef(name = "deploy", type=Deploy.class), @XmlElementRef(name = "alert", type=Alert.class)})
    private List<Deploy> deployments;
    @XmlElementWrapper(name = "global")
    @XmlElement(name = "p")
    private List<Property> global;
    @XmlAttribute(name = "template-dir")
    private String templateDir;

    public void process(Set<String> deployNames, Map<String, ValueResolver> globalValues, Namespace namespace, Configuration configuration) throws TemplateException, IOException {
        List<Deploy> deps = deployNames.isEmpty() ? deployments : deployments.stream()
                .filter(d -> deployNames.contains(d.name))
                .collect(Collectors.toList());
        for (Deploy deployment : deps) deployment.process(namespace, globalValues, configuration, templateDir);
    }

    public Map<String, ValueResolver> getGlobalValues(Namespace ns) {
        version.name = "version";
        return Stream.concat(Stream.of(version), global.stream()).collect(Collectors.toMap(p -> p.name, p -> p.getValue(ns)));
    }

    public Set<Namespace> getMappings() {
        return mappings;
    }

    public List<Deploy> getDeployments() {
        return deployments;
    }

    public List<Property> getGlobal() {
        return global;
    }

    public Property getVersion() {
        return version;
    }
}
