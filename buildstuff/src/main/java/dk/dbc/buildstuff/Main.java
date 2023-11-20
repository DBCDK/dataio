package dk.dbc.buildstuff;

import com.ongres.process.FluentProcess;
import dk.dbc.buildstuff.model.Application;
import dk.dbc.buildstuff.model.Deploy;
import dk.dbc.buildstuff.model.Namespace;
import freemarker.template.Configuration;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(name = "deploy", mixinStandardHelpOptions = true, showDefaultValues = true, version = "1.0")
public class Main implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static Path basePath;
    @CommandLine.Parameters(index = "0", description = "The action you want to perform. Valid options are: <APPLY | GENERATE | VERSION>")
    private Command command;
    @CommandLine.Parameters(index = "1", description = "The xml file containing your application config (template and output path is relative to this file)")
    private String filename;
    @CommandLine.Option(names = "-n", description = "namespace", defaultValue = "metascrum-staging")
    private String namespace;
    @CommandLine.Option(names = "-d", description = "deployment")
    private String deployment;
    @CommandLine.Option(names = "-t", description = "git token")
    private String token;
    @CommandLine.Option(names = "-v", description = "version")
    private String version;
    @CommandLine.Option(names = "-r", description = "git repository", defaultValue = "https://gitlab.dbc.dk/metascrum/dataio-secrets.git")
    private String repository;
    @CommandLine.Option(names = "-b", description = "git branch", defaultValue = "main")
    private String branch;

    public Main() {
    }

    public Main(Command command, String filename, String namespace, String deployment) {
        this.command = command;
        this.filename = filename;
        this.namespace = namespace;
        this.deployment = deployment;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        switch (command) {
            case GENERATE:
                generate();
                break;
            case VERSION:
                updateVersion();
                break;
            case APPLY:
                apply();
                break;
        }
        return 0;
    }

    public static Path getBasePath() {
        return basePath;
    }

    public Namespace generate() throws IOException, JAXBException, SAXException, GitAPIException {
        return generateAppConfig(readConfig(getConfigPath()));
    }

    private Path getConfigPath() {
        Path xmlConfig = Path.of(filename);
        basePath = xmlConfig.toAbsolutePath().getParent();
        return xmlConfig;
    }

    private void apply() throws IOException, JAXBException, SAXException, GitAPIException {
        Path xmlConfig = Path.of(filename);
        basePath = xmlConfig.toAbsolutePath().getParent();
        Application application = readConfig(getConfigPath());
        Namespace ns = generateAppConfig(application);
        Stream<String> args = Stream.of("apply", "-n", ns.getNamespace());
        if (getDeployments().isEmpty()) {
            args = Stream.concat(args, Stream.of("-f", basePath.resolve(ns.getNamespace()).toString()));
        } else {
            List<Deploy> enabledDeployments = application.getEnabledDeployments(ns, getDeployments());
            args = Stream.concat(args, enabledDeployments.stream().flatMap(d -> Stream.of("-f", d.getPath().toString())));
        }
        String[] param = args.toArray(String[]::new);
        LOGGER.info("Executing: kubectl " + String.join(" ", param));
        try (FluentProcess process = FluentProcess.start("kubectl", param)) {
            process.stream().forEach(l -> LOGGER.info("KUBECTL: " + l));
        }
    }

    private void updateVersion() throws GitAPIException, IOException, JAXBException, SAXException {
        Objects.requireNonNull(version, "Please specify a version with -v <version>");
        Path temp = Files.createTempDirectory("buildstuff_");
        Git git = GitHelper.checkOut(temp, repository, branch, token);
        Path xmlConfig = temp.resolve(filename);
        basePath = xmlConfig.toAbsolutePath().getParent();
        updateVersionDocument();
        generateAppConfig(readConfig(xmlConfig));
        GitHelper.checkIn(git, branch, token);
    }

    private void updateVersionDocument() throws IOException {
        Path versionFile = basePath.resolve("version.txt");
        Files.writeString(versionFile, version, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private Application readConfig(Path xmlConfig) throws JAXBException, SAXException {
        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getClassLoader().getResource("buildstuff.xsd"));
        Unmarshaller unmarshaller = JAXBContext.newInstance(Application.class).createUnmarshaller();
        unmarshaller.setSchema(schema);
        return unmarshaller.unmarshal(new StreamSource(xmlConfig.toFile()), Application.class).getValue();
    }

    private Namespace generateAppConfig(Application application) throws IOException, GitAPIException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
        cfg.setDirectoryForTemplateLoading(basePath.toFile());
        cfg.setFallbackOnNullLoopVariable(false);
        cfg.setLogTemplateExceptions(false);
        Namespace ns = application.getMappings().stream().filter(m -> namespace.equals(m.getShortName()) || namespace.equals(m.getNamespace())).findFirst().orElseThrow(() -> new IllegalArgumentException("I do not know the namespace " + namespace));
        LOGGER.info("Processing deployments for " + ns.getNamespace());
        Map<String, ValueResolver> globalValues = application.getGlobalValues(ns);
        Set<String> deployNames = getDeployments();
        application.setupResolvers(getDeployments(), ns, globalValues);
        Git git = ns.checkout(token);
        application.process(deployNames, ns, cfg);
        ns.checkin(git, token);
        return ns;
    }

    private Set<String> getDeployments() {
        return deployment == null || deployment.isBlank() ? Set.of() : Set.of(deployment.split("[, ]+"));
    }

    public enum Command {
        APPLY, GENERATE, VERSION
    }
}
