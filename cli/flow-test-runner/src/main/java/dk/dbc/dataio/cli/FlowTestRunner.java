package dk.dbc.dataio.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.diff.MessageConsumerBean;
import jakarta.xml.bind.JAXB;
import junit.testsuite.Testcase;
import junit.testsuite.Testsuite;
import junit.testsuite.Testsuites;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("java:S106") //Standard outputs should not be used directly to log anything
@CommandLine.Command(name = "flow-test-runner", mixinStandardHelpOptions = true, showDefaultValues = true, version = "1.0")
public class FlowTestRunner implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowTestRunner.class);

    @CommandLine.Parameters(description = "Path to local script as JavaScript ARchive file (.jsar)")
    private Path jsar;
    @CommandLine.Parameters(description = "Path to testsuites", defaultValue = ".")
    private Path dataPath;

    @Option(names = "-rp", description = "Report output path", defaultValue = "target/flow-test-runner-reports")
    private Path reportPath;
    @Option(names = "--packageName", description = "Used for grouping test results in report")
    private String packageName;

    public static void main(String[] args) {
        System.exit(runWith(args));
    }

    static int runWith(String... args) {
        return runWith(FlowTestRunner::new, args);
    }

    static int runWith(Supplier<FlowTestRunner> constructor, String... args) {
        final CommandLine cli = new CommandLine(constructor.get())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .registerConverter(Path.class, Path::of);
        return cli.execute(args);
    }

    @Override
    public Integer call() {
        try {
            if (runTests()) {
                System.out.println("\nSUCCESS - all tests passed");
                return 0;
            } else {
                System.out.println("\nFAILURE - there were test failures");
                return -1;
            }
        } catch (Exception e) {
            System.out.println("\nError during flow test processing: " + e.getMessage());
        }
        return -255;
    }

    private boolean runTests() throws IOException {
        final ChunkProcessor chunkProcessor = getChunkProcessor(jsar);
        boolean allFeaturesAreSuccessful = true;
        for (FlowTestSuite suite: FlowTestSuite.findFlowTestSuites(dataPath)) {
           allFeaturesAreSuccessful &= runSuite(chunkProcessor, suite);
        }
        return allFeaturesAreSuccessful;
    }

    private boolean runSuite(ChunkProcessor chunkProcessor, FlowTestSuite suite) {
        System.out.println("\nTesting feature: " + suite.getFeatureName());
        System.out.println(suite.getFeatureDescription());
        setupLogging(suite);
        final List<Testcase> testcases = new ArrayList<>();
        boolean allScenariosAreSuccessful = true;
        for (FlowTestSuite.Scenario scenario : suite.getScenarios()) {
            allScenariosAreSuccessful &= runScenario(chunkProcessor, suite, scenario, testcases::add);
        }
        writeReport(suite, testcases);
        return allScenariosAreSuccessful;
    }

    private boolean runScenario(ChunkProcessor chunkProcessor, FlowTestSuite suite, FlowTestSuite.Scenario scenario,
                                Consumer<Testcase> testcaseConsumer) {
        System.out.println("Testing scenario: " + scenario.getName());
        System.out.println(scenario.getDescription());
        final String additionalArgs = String.format("{\"format\":\"%s\",\"submitter\":%s}", scenario.getFormat(), scenario.getAgency());
        final Path inputFilePath = suite.getInputDir().resolve(scenario.getInputFile());
        final Path outputFilePath = suite.getActualStateDir().resolve(scenario.getOutputFile());
        try {
            final Chunk actualChunk = chunkProcessor.process(getChunk(inputFilePath, Chunk.Type.PARTITIONED), 1, 1, additionalArgs);
            writeActualState(actualChunk, outputFilePath);
            final Chunk expectedChunk = getChunk(suite.getExpectedStateDir().resolve(scenario.getOutputFile()), Chunk.Type.PROCESSED);
            final Chunk diff = compare(expectedChunk, actualChunk);
            testcaseConsumer.accept(Testcase.from(scenario, diff.getItems().get(0)));
            if (!diff.getItems().get(0).getStatus().equals(ChunkItem.Status.SUCCESS)) {
                System.out.println("FAILED in comparison of actual and expected state\n");
                return false;
            }
            System.out.println("OK\n");
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ChunkProcessor getChunkProcessor(Path jsar) throws IOException {
        final FlowContent flowContent = new FlowContent(Files.readAllBytes(jsar), new Date());
        final Flow flow = new Flow(1, 1, flowContent);
        return new ChunkProcessor(null, id -> flow);
    }

    private Chunk getChunk(Path data, Chunk.Type type) throws IOException {
        final Chunk chunk = new Chunk(0, 0, type);
        chunk.insertItem(getChunkItem(data));
        return chunk;
    }

    private ChunkItem getChunkItem(Path data) throws IOException {
        final ChunkItem chunkItem = new ChunkItem()
                .withId(0)
                .withData(Files.readAllBytes(data))
                .withStatus(ChunkItem.Status.SUCCESS);
        if (data.endsWith(".addi")) {
            chunkItem.withType(ChunkItem.Type.ADDI);
        }
        return chunkItem;
    }

    private void writeActualState(Chunk outputChunk, Path outputFilePath) throws IOException {
        Files.createDirectories(outputFilePath.getParent());
        Files.write(outputFilePath, outputChunk.getItems().get(0).getData());
        System.out.println("Actual state written to: " + outputFilePath);
    }

    private Chunk compare(Chunk expectedChunk, Chunk actualChunk) {
        final ServiceHub serviceHub = new ServiceHub.Builder().withJobStoreServiceConnector(null).build();
        final Chunk compareChunk = new Chunk(actualChunk.getJobId(), actualChunk.getChunkId(), actualChunk.getType());
        compareChunk.addAllItems(expectedChunk.getItems(), actualChunk.getItems());
        try {
            return new MessageConsumerBean(serviceHub).handleChunk(compareChunk);
        } catch (InvalidMessageException e) {
            throw new IllegalStateException(e);
        }
    }

    private void writeReport(FlowTestSuite suite, List<Testcase> testcases) {
        final String classnameSuffix = suite.getFeatureName().replace('.', '_');
        if (packageName != null) {
            testcases.forEach(tc -> tc.setClassname(packageName + ".flowtest." + classnameSuffix));
        }
        final ArrayList<Object> testcaseObjects = testcases.stream().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        final Testsuite testsuite = new Testsuite()
                .withName(suite.getFeatureName())
                .withTests(Integer.toString(testcases.size()))
                .withTestsuiteOrPropertiesOrTestcase(testcaseObjects);
        final Testsuites testsuites = new Testsuites().withTestsuite(List.of(testsuite));
        try {
            Files.createDirectories(reportPath);
            final Path reportFile = reportPath.resolve("TEST-" + suite.getFeatureName() + ".xml");
            JAXB.marshal(testsuites, reportFile.toFile());
            System.out.println("Report written to: " + reportFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void setupLogging(FlowTestSuite suite) {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        try {
            Files.createDirectories(suite.getLogsDir().getParent());

            final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%d{ISO8601} %5p %c{1} : %m%n");
            encoder.start();

            final FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setContext(loggerContext);
            fileAppender.setName("FILE");
            fileAppender.setFile(suite.getLogsDir().resolve(suite.getFeatureName() + ".log").toString());
            fileAppender.setAppend(false);
            fileAppender.setEncoder(encoder);
            fileAppender.start();

            final ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            logger.addAppender(fileAppender);
            logger.setLevel(Level.ERROR);

            final ch.qos.logback.classic.Logger javascriptLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("JavaScript.Logger");
            javascriptLogger.setLevel(Level.TRACE);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
