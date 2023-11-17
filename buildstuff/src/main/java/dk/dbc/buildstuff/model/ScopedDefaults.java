package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;
import freemarker.template.Configuration;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@XmlRootElement(name = "defaults")
@XmlType(propOrder = {"deployments", "properties", "list"})
@XmlAccessorType(XmlAccessType.FIELD)
public class ScopedDefaults extends ResolvingObject {
    @XmlElementRefs({
            @XmlElementRef(name = "deploy", type=Deploy.class),
            @XmlElementRef(name = "alert", type=Alert.class),
            @XmlElementRef(name = "defaults", type=ScopedDefaults.class)
    })
    private List<ResolvingObject> deployments;

    @XmlElement
    @Override
    public List<DynamicList> getList() {
        return super.getList();
    }

    @XmlElement(name = "p")
    public List<Property> getProperties() {
        return properties;
    }

    @Override
    public void process(Set<String> deployNames, Namespace namespace, ResolvingObject parent, Configuration configuration, String templateDir) throws IOException {
        if(!isEnabled(deployNames, namespace)) return;

        for (ResolvingObject deployment : deployments) {
            deployment.process(deployNames, namespace, this, configuration, templateDir);
        }
    }

    @Override
    public boolean isEnabled(Set<String> deployNames, Namespace ns) {
        return deployments.stream().anyMatch(d -> d.isEnabled(deployNames, ns));
    }

    @Override
    public boolean setupResolvers(Set<String> deployNames, Namespace namespace, Map<String, ValueResolver> scope) {
        if(super.setupResolvers(deployNames, namespace, scope)) {
            deployments.forEach(d -> d.setupResolvers(deployNames, namespace, this));
            return true;
        }
        return false;
    }

    @Override
    public boolean setupResolvers(Set<String> deployNames, Namespace namespace, ResolvingObject parent) {
        if(super.setupResolvers(deployNames, namespace, parent)) {
            deployments.forEach(d -> d.setupResolvers(deployNames, namespace, this));
            return true;
        }
        return false;
    }

    @Override
    public Stream<Deploy> getDeployments(Set<String> deployNames, Namespace namespace) {
        if(deployments == null) return Stream.of();
        return deployments.stream().flatMap(r -> r.getDeployments(deployNames, namespace));
    }
}
