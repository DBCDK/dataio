package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.Main;
import dk.dbc.buildstuff.ValueResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@XmlRootElement
@XmlSeeAlso({Alert.class})
public class Deploy extends ResolvingObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(Deploy.class);
    @XmlAttribute
    private String template;
    @XmlAttribute
    private Boolean enabled;
    @XmlAttribute
    private String filter;
    @XmlElement
    private List<DynamicList> list = new ArrayList<>();
    private static final Map<Object, Object> RECASTS = Map.of("true", true, "false", false);

    public void process(Namespace namespace, Map<String, ValueResolver> globalValues, Configuration configuration, String templateDir) throws IOException {
        if(isEnabled() && (getFilter().isEmpty() || getFilter().contains(namespace.getShortName()))) {
            resolveTokens(name, namespace, globalValues);
            for (DynamicList dynamicList : list) dynamicList.resolveTokens(name + "." + dynamicList.name, namespace, map);
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
        for (DynamicList dynamicList : list) {
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

    public Set<String> getFilter() {
        if(filter == null || filter.isEmpty()) return Set.of();
        return new HashSet<>(Arrays.asList(filter.split("[ ,]+")));
    }

    private Object recastValue(Object value) {
        return RECASTS.getOrDefault(value, value);
    }
}
