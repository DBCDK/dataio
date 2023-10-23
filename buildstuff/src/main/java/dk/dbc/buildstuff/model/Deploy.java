package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.Main;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@XmlRootElement
@XmlSeeAlso({Alert.class})
@XmlType(propOrder = {"properties", "list"})
public class Deploy extends ResolvingObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(Deploy.class);
    @XmlAttribute(required = true)
    private String template;
    @XmlAttribute
    private Boolean enabled;
    @XmlAttribute
    private String filter;
    private static final Map<Object, Object> RECASTS = Map.of("true", true, "false", false);
    @Override
    @XmlElement(name = "p")
    public List<Property> getProperties() {
        return properties;
    }

    @Override
    @XmlElement
    public List<DynamicList> getList() {
        return super.getList();
    }

    public void process(Set<String> deployNames, Namespace namespace, ResolvingObject parent, Configuration configuration, String templateDir) throws IOException {
        if(isEnabled(deployNames, namespace)) {
            resolveTokens(name);
            list = getListsInScope().map(l -> l.clone(this)).collect(Collectors.toList());
            list.forEach(l -> l.resolveTokens(name + "." + l.name));
            processTemplate(configuration, templateDir);
        }
    }

    public String getFilename() {
        return "deploy-" + name + ".yml";
    }

    private void processTemplate(Configuration configuration, String templateDir) throws IOException {
        Template ftl = configuration.getTemplate((templateDir == null ? "" : templateDir + "/") + template);
        Path targetDirectory = Files.createDirectories(Main.getBasePath().resolve(namespace.getNamespace()));
        Path file = targetDirectory.resolve(getFilename());
        Map<String, Object> model = new HashMap<>();
        for (DynamicList dynamicList : getList()) {
            if(dynamicList.getProperties() != null) {
                Map<String, String> dm = dynamicList.getProperties().stream().collect(Collectors.toMap(e -> e.name, e -> dynamicList.map.get(e.name).getValue()));
                model.put(dynamicList.name, dm);
            }
        }
        model.putAll(map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue())));
        model = model.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> recastValue(e.getValue())));
        try (FileWriter writer = new FileWriter(file.toFile())) {
            ftl.process(model, writer);
        } catch (TemplateException te) {
            if(Files.isRegularFile(file)) Files.move(file, Path.of(file + ".failed"), StandardCopyOption.REPLACE_EXISTING);
            throw new IllegalStateException("Failed to write file for " + name + " caused by error while processing template.\n\n" + te.getMessage());
        }
        LOGGER.info("Deploy " + name + ": Wrote file " + file);
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    @Override
    public boolean isEnabled(Set<String> deployNames, Namespace ns) {
        return isEnabled() && isEmptyOrContains(deployNames, name) && isEmptyOrContains(getFilter(), ns.getShortName());
    }

    private boolean isEmptyOrContains(Set<String> set, String s) {
        return set.isEmpty() || set.contains(s);
    }

    @Override
    public boolean isResolving() {
        return true;
    }

    public Set<String> getFilter() {
        if(filter == null || filter.isEmpty()) return Set.of();
        return new HashSet<>(Arrays.asList(filter.split("[ ,]+")));
    }

    private Object recastValue(Object value) {
        return RECASTS.getOrDefault(value, value);
    }
}
