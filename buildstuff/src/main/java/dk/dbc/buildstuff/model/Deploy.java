package dk.dbc.buildstuff.model;

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
import java.util.stream.Stream;

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
    private String include;
    @XmlAttribute
    private String exclude;
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

    public Path getPath() {
        try {
            Path path = namespace.getTargetPath();
            Files.createDirectories(path);
            return path.resolve(getFilename());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create target directory for " + namespace.getNamespace(), e);
        }
    }

    private void processTemplate(Configuration configuration, String templateDir) throws IOException {
        Template ftl = configuration.getTemplate((templateDir == null ? "" : templateDir + "/") + template);
        Map<String, Object> model = new HashMap<>();
        for (DynamicList dynamicList : getList()) {
            if(dynamicList.getProperties() != null) {
                Map<String, String> dm = dynamicList.getProperties().stream().collect(Collectors.toMap(e -> e.name, e -> dynamicList.map.get(e.name).getValue()));
                model.put(dynamicList.name, dm);
            }
        }
        model.putAll(map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue())));
        model = model.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> recastValue(e.getValue())));
        writeFile(ftl, model);
    }

    private void writeFile(Template ftl, Map<String, Object> model) throws IOException {
        Path file = getPath();
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
        return isEnabled() && isEmptyOrContains(deployNames, name) && getFilter(ns);
    }

    public Set<String> getInclude() {
        if(include == null || include.isEmpty()) return Set.of();
        return new HashSet<>(Arrays.asList(include.split(LIST_SPLITTER)));
    }

    public Set<String> getExclude() {
        if(exclude == null || exclude.isEmpty()) return Set.of();
        return new HashSet<>(Arrays.asList(exclude.split(LIST_SPLITTER)));
    }

    private Object recastValue(Object value) {
        return RECASTS.getOrDefault(value, value);
    }

    @Override
    public Stream<Deploy> getDeployments(Set<String> deployNames, Namespace namespace) {
        if(isEnabled(deployNames, namespace)) return Stream.of(this);
        return Stream.of();
    }
}
