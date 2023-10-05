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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XmlRootElement
@XmlSeeAlso({Alert.class})
public class Deploy extends NamedBaseObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(Deploy.class);
    @XmlAttribute
    private String template;
    @XmlAttribute
    private Boolean enabled;
    @XmlAttribute
    private String filter;
    @XmlElement(name = "p")
    private List<Property> properties = new ArrayList<>();
    @XmlElement
    private List<DynamicList> list = new ArrayList<>();
    private Map<String, ValueResolver> map;
    private Set<String> unresolved;
    private Namespace namespace;
    private static final Map<Object, Object> RECASTS = Map.of("true", true, "false", false);

    public void process(Namespace namespace, Map<String, ValueResolver> globalValues, Configuration configuration, String templateDir) throws IOException {
        if(isEnabled() && (getFilter().isEmpty() || getFilter().contains(namespace.getShortName()))) {
            resolveTokens(namespace, globalValues);
            processTemplate(configuration, templateDir);
        }
    }

    public String getFilename() {
        return "deploy-" + name + ".yml";
    }

    private void resolveTokens(Namespace namespace, Map<String, ValueResolver> globalValues) {
        this.namespace = namespace;
        Map<String, ValueResolver> localValues = Stream.of(properties.stream(), list.stream().map(DynamicList::getProperties).flatMap(Collection::stream))
                .flatMap(l -> l)
                .collect(Collectors.toMap(l -> l.name, l -> l.getValue(namespace)));
        map = new HashMap<>(globalValues);
        map.putAll(localValues);
        map.put("name", new ValueResolver(name));
        unresolved = map.entrySet().stream().filter(e -> e.getValue().hasTokens(name, e.getKey(), namespace)).map(Map.Entry::getKey).collect(Collectors.toSet());
        resolve(20);
    }

    private void processTemplate(Configuration configuration, String templateDir) throws IOException {
        Template ftl = configuration.getTemplate((templateDir == null ? "" : templateDir + "/") + template);
        Path targetDirectory = Files.createDirectories(Main.getBasePath().resolve(namespace.getNamespace()));
        Path file = targetDirectory.resolve(getFilename());
        Map<String, Object> model = new HashMap<>();
        for (DynamicList dynamicList : list) {
            Map<String, Object> freeMarkerProperties = dynamicList.getProperties().stream().collect(Collectors.toMap(p -> p.name, p -> map.get(p.name).getValue()));
            model.put(dynamicList.name, freeMarkerProperties);
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

    private void resolve(int loopMax) {
        if(loopMax < 1) throw new IllegalStateException("Token resolver exceeded its maximum attempts while resolving deployment " + name + ". Please ensure that you have no looping references in these variables " + unresolved);
        Set<String> done = new HashSet<>();
        for (String s : unresolved) {
            ValueResolver valueResolver = map.get(s);
            if (valueResolver.resolve(map)) done.add(s);
        }
        unresolved.removeAll(done);
        if (!unresolved.isEmpty()) resolve(loopMax - 1);
    }
}
