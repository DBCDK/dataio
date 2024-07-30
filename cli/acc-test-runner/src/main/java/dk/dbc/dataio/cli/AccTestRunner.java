package dk.dbc.dataio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.DataPartitionerFactory;
import dk.dbc.dataio.commons.partioner.DataPartitionerResult;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.diff.MessageConsumerBean;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@CommandLine.Command(name = "acc-test-runner", mixinStandardHelpOptions = true, showDefaultValues = true, version = "3.0")
public class AccTestRunner implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccTestRunner.class);

    @CommandLine.Parameters(description = "Action <TEST|UPLOAD|COMMIT>, can be either test for running tests or commit for committing previously run tests. " +
            "Upload allows the user to bypass tests, mostly for creating new flows, or for projects where tests are not desirable", defaultValue = "TEST")
    private Action action;
    @CommandLine.Parameters(description = "Path to local script as JavaScript ARchive file (.jsar)")
    private Path jsar;
    @CommandLine.Parameters(description = "Data path", defaultValue = ".")
    private Path dataPath;
    @Option(names = "-d", description = "Default project properties", defaultValue = "settings/default.properties")
    private String defaultProperties;
    @Option(names = "-f", description = "FlowStore url (prod flowstore is default)", defaultValue = "http://dataio-flowstore-service.metascrum-prod.svc.cloud.dbc.dk/dataio/flow-store-service")
    private FlowManager flowManager;
    @Option(names = "-r", description = "Report format <TEXT|XML>", defaultValue = "TEXT")
    private ReportFormat reportFormat;
    @Option(names = "-j", description = "Job specification file")
    private Path jobSpec;
    @Option(names = "-rs", description = "Record splitter <ADDI|ADDI_MARC_XML|CSV|DANMARC2_LINE_FORMAT|DANMARC2_LINE_FORMAT_COLLECTION|" +
            "DSD_CSV|ISO2709|ISO2709_COLLECTION|JSON|VIAF|VIP_CSV|XML|TARRED_XML|ZIPPED_XML>")
    private RecordSplitter recordSplitter;
    @Option(names = "-cp", description = "Directory for temporary commit file", defaultValue = "target")
    private Path commitPath;
    @Option(names = "-rp", description = "Report output path", defaultValue = "target/reports")
    private Path reportPath;
    @Option(names = "-v", description = "Version")
    private Long revision;

    private boolean foundFlowByName = false;

    public static void main(String[] args) {
        System.exit(runWith(args));
    }

    static int runWith(String... args) {
        return runWith(AccTestRunner::new, AccTestRunner::flowManager, args);
    }

    static int runWith(Function<String, FlowManager> f, String... args) {
        return runWith(AccTestRunner::new, f, args);
    }

    static int runWith(Supplier<AccTestRunner> constructor, Function<String, FlowManager> f, String... args) {
        final CommandLine cli = new CommandLine(constructor.get())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .registerConverter(Path.class, Path::of)
                .registerConverter(FlowManager.class, f::apply);
        return cli.execute(args);
    }

    @Override
    public Integer call() {
        try {
            if (action == Action.COMMIT) return flowManager.commit(commitPath);
            if (action == Action.UPLOAD) return flowManager.upload(jsar);
            if (action == Action.TEST) return runTest();
        } catch (Exception e) {
            LOGGER.error("Error during acctest", e);
        }
        return -255;
    }

    private int runTest() throws Exception {
        if(!Files.isRegularFile(jsar)) throw new IllegalArgumentException("JavaScript ARchive file " + jsar + " is not a readable file");
        Flow localFlow = flowManager.getFlow(jsar);
        foundFlowByName = flowManager.foundFlowByName();
        if(!Files.isReadable(dataPath)) throw new IllegalArgumentException("Datafile " + dataPath + " is not a readable file");
        if(revision == null) throw new IllegalStateException("Please state the version being tested using -v <version>");
        List<AccTestSuite> testSuites = findSuites();
        if(testSuites.isEmpty()) {
            throw new IllegalArgumentException("No test suites where found");
        }
        return runTest(localFlow, testSuites);
    }

    Integer runTest(Flow localFlow, List<AccTestSuite> testSuites) throws Exception {
        ServiceHub serviceHub = new ServiceHub.Builder().withJobStoreServiceConnector(null).build();
        Set<Long> flows = new HashSet<>();

        Flow resolvedRemotely = null;
        if (foundFlowByName) {
            LOGGER.info("found existing flow by name: id={} version={} name='{}'",
                    localFlow.getId(), localFlow.getVersion(), localFlow.getContent().getName());
            flows.add(localFlow.getId());
            resolvedRemotely = localFlow;
        }

        for (AccTestSuite suite : testSuites) {
            Flow remoteFlow = flowManager.getFlow(suite.getJobSpecification());
            if (resolvedRemotely == null && remoteFlow == null) {
                /* Not resolving flow by name or specification probably indicates a new flow
                   that is about to be created, so we allow the runner to complete its
                   course knowing full well that should the actual cause turn out to be an
                   invalid specification, it will be caught by the next acctest run. */
                remoteFlow = localFlow;
            } else {
                if (remoteFlow == null) {
                    throw new IllegalArgumentException("Testsuite " + suite.getName() + " failed to resolve a flow");
                }
                resolvedRemotely = remoteFlow;
            }

            flows.add(remoteFlow.getId());
            if(flows.size() > 1) throw new IllegalArgumentException("Stopped at testsuite " + suite.getName() + ". All test data, within an acceptance test, must address the same flowId. Flows: " + flows);

            LOGGER.info("running test suite with local flow");
            Chunk localOutputChunk = processSuite(suite, localFlow);

            LOGGER.info("running test suite with remote flow");
            Chunk remoteOutputChunk = processSuite(suite, remoteFlow);

            final Chunk processed = new Chunk(localOutputChunk.getJobId(), localOutputChunk.getChunkId(), localOutputChunk.getType());
            processed.addAllItems(remoteOutputChunk.getItems(), localOutputChunk.getItems());
            Chunk diff = new MessageConsumerBean(serviceHub).handleChunk(processed);
            reportFormat.printDiff(suite, remoteFlow, diff, revision);
        }

        if (!foundFlowByName && resolvedRemotely != null) {
            /* Existing flow could not be resolved by name,
               but could be resolved by testsuite specification.
               This indicates a name update via the MANIFEST.MF file. */
            localFlow = new Flow(resolvedRemotely.getId(), resolvedRemotely.getVersion(), localFlow.getContent());
        }

        LOGGER.info("resulting flow: id={} version={} name='{}'",
                localFlow.getId(), localFlow.getVersion(), localFlow.getContent().getName());

        flowManager.createFlowCommitTmpFile(commitPath, localFlow, resolvedRemotely == null ? FlowManager.CommitTempFile.Action.CREATE
                : FlowManager.CommitTempFile.Action.UPDATE);

        return 0;
    }

    private Chunk processSuite(AccTestSuite accTestSuite, Flow flow) {
        JobSpecification jobSpecification = accTestSuite.getJobSpecification();
        try(InputStream is = new BufferedInputStream(Files.newInputStream(accTestSuite.getDataFile()))) {
            DataPartitioner partitionerResults = DataPartitionerFactory.createNoReordering(accTestSuite.getRecordSplitter(), is, jobSpecification.getCharset());
            Stream<DataPartitionerResult> stream = StreamSupport.stream(partitionerResults.spliterator(), false);
            Chunk chunk = readChunk(stream);
            String additionalStuff = "{\"format\":\"" + jobSpecification.getFormat() + "\",\"submitter\":" + jobSpecification.getSubmitterId() + "}";
            return processChunk(flow, chunk, additionalStuff);
        } catch (Exception e) {
            throw new IllegalStateException("Failed processing " + accTestSuite.getName(), e);
        }
    }

    List<AccTestSuite> findSuites() throws IOException {
        if(Files.isRegularFile(dataPath)) return List.of(new AccTestSuite(new ObjectMapper().readValue(jobSpec.toFile(), JobSpecification.class), recordSplitter));
        try(Stream<Path> accFiles =  Files.find(dataPath, 10, AccTestSuite::isAccTestSpec, FileVisitOption.FOLLOW_LINKS)) {
            return accFiles.map(f -> new AccTestSuite(loadProperties(), f, reportPath)).toList();
        }
    }

    private Chunk processChunk(Flow flow, Chunk chunk, String additionalStuff) {
        ChunkProcessor processor = new ChunkProcessor(null, id -> flow);
        return processor.process(chunk, flow.getId(), flow.getVersion(), additionalStuff);
    }

    private Chunk readChunk(Stream<DataPartitionerResult> stream) {
        AtomicLong id = new AtomicLong(0);
        List<ChunkItem> items = stream.map(DataPartitionerResult::getChunkItem).filter(Objects::nonNull).map(ci -> ci.withId(id.getAndIncrement())).toList();
        Chunk chunk = new Chunk(0, 0, Chunk.Type.PARTITIONED);
        chunk.addAllItems(items);
        return chunk;
    }

    private static FlowManager flowManager(String serviceUrl) {
        Client httpClient = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(httpClient, new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 500 || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6));
        return new FlowManager(new FlowStoreServiceConnector(failSafeHttpClient, serviceUrl));
    }

    Properties loadProperties() {
        Properties properties = new Properties();
        if(defaultProperties == null) return properties;
        Path p = Path.of(defaultProperties);
        if(!Files.isRegularFile(p)) return properties;
        try(Reader reader = Files.newBufferedReader(p)) {
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Found default properties " + p.toAbsolutePath() + " file, but not able to read it", e);
        }
    }

    public enum Action {
        TEST, UPLOAD, COMMIT
    }
}
