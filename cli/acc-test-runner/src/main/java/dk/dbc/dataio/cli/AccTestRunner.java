package dk.dbc.dataio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.DataPartitionerFactory;
import dk.dbc.dataio.commons.partioner.DataPartitionerResult;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.diff.MessageConsumerBean;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import picocli.CommandLine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@CommandLine.Command(name = "acc-test-runner.sh", mixinStandardHelpOptions = true, showDefaultValues = true, version = "1.0")
public class AccTestRunner implements Callable<Integer> {
    @CommandLine.Parameters(description = "Data Path", defaultValue = ".")
    private Path dataPath;
    @CommandLine.Option(names = "-f", description = "FlowStore url (prod flowstore is default)", defaultValue = "http://dataio-flowstore-service.metascrum-prod.svc.cloud.dbc.dk/dataio/flow-store-service")
    private FlowManager flowManager;
    @CommandLine.Option(names = "-s", description = "Path to local script", required = true)
    private Path nextScripts;
    @CommandLine.Option(names = "-d", description = "Search path for dependencies", required = true)
    private Path dependencies;
    @CommandLine.Option(names = "-r", description = "Report format <TEXT|XML>", defaultValue = "TEXT")
    private ReportFormat reportFormat;
    @CommandLine.Option(names = "-j", description = "Job specification file")
    private Path jobSpec;
    @CommandLine.Option(names = "-rs", description = "Record splitter <ADDI|ADDI_MARC_XML|CSV|DANMARC2_LINE_FORMAT|DANMARC2_LINE_FORMAT_COLLECTION|" +
            "DSD_CSV|ISO2709|ISO2709_COLLECTION|JSON|VIAF|VIP_CSV|XML|TARRED_XML|ZIPPED_XML>")
    private RecordSplitter recordSplitter;
    @CommandLine.Option(names = "-rp", description = "Report output path", defaultValue = "target/reports")
    private Path reportPath;
    @CommandLine.Option(names = "-c", description = "Commit tested revision")
    private Long revision;
    private JavaScriptProject project;

    public static void main(String[] args) {
        new CommandLine(new AccTestRunner())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .registerConverter(Path.class, Path::of)
                .registerConverter(FlowManager.class, AccTestRunner::flowManager)
                .execute(args);
    }

    @Override
    public Integer call() throws Exception {
        if(!Files.isRegularFile(nextScripts)) throw new IllegalArgumentException("Local script file " + nextScripts + " is not a readable file");
        if(!Files.isDirectory(dependencies)) throw new IllegalArgumentException("Path for dependencies " + dependencies + " is not valid");
        project = JavaScriptProject.of(nextScripts, dependencies);
        if(revision != null) {
            flowManager.commit(project, revision);
        } else runTest();
        return 0;
    }

    private void runTest() throws Exception {
        if(!Files.isReadable(dataPath)) throw new IllegalArgumentException("Datafile " + dataPath + " is not a readable file");
        List<AccTestSuite> testSuites = findSuites();
        if(testSuites.isEmpty()) {
            throw new IllegalArgumentException("No test suites where found");
        }
        runTest(testSuites);
    }

    private void runTest(List<AccTestSuite> testSuites) throws Exception {
        ServiceHub serviceHub = new ServiceHub.Builder().withJobStoreServiceConnector(null).build();
        Set<Flow> flows = new HashSet<>();
        for (AccTestSuite suite : testSuites) {
            Flow flow = flowManager.getFlow(suite.getJobSpecification());
            flows.add(flow);
            if(flows.size() > 1) throw new IllegalArgumentException("All test data, within an acceptance test, must address the same flow");
            Chunk processed = processSuite(suite, flow);
            Chunk diff = new MessageConsumerBean(serviceHub).handleChunk(processed);
            reportFormat.printDiff(suite, flow, diff);
        }
        flows.stream().findFirst().ifPresent(flowManager::createFlowCommitTmpFile);
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

    private List<AccTestSuite> findSuites() throws IOException {
        if(Files.isRegularFile(dataPath)) return List.of(new AccTestSuite(new ObjectMapper().readValue(jobSpec.toFile(), JobSpecification.class), recordSplitter));
        try(Stream<Path> accFiles =  Files.find(dataPath, 10, AccTestSuite::isAccTestSpec, FileVisitOption.FOLLOW_LINKS)) {
            return accFiles.map(f -> new AccTestSuite(f, reportPath)).collect(Collectors.toList());
        }
    }

    private Chunk processChunk(Flow flow, Chunk chunk, String additionalStuff) throws Exception {
        if(nextScripts != null) overwriteNext(flow);
        ChunkProcessor processor = new ChunkProcessor(null, id -> flow);
        return processor.process(chunk, flow.getId(), flow.getVersion(), additionalStuff);
    }

    private void overwriteNext(Flow flow) {
        FlowComponent component = flow.getContent().getComponents().get(0);
        FlowComponentContent next = component.getNext();
        component.withNext(new FlowComponentContent(next.getName(), next.getSvnProjectForInvocationJavascript(), 1, next.getInvocationJavascriptName(), project.getJavaScripts(), next.getInvocationMethod(), next.getDescription()));
    }

    private Chunk readChunk(Stream<DataPartitionerResult> stream) {
        AtomicLong id = new AtomicLong(0);
        List<ChunkItem> items = stream.map(DataPartitionerResult::getChunkItem).map(ci -> ci.withId(id.getAndIncrement())).collect(Collectors.toList());
        Chunk chunk = new Chunk(0, 0, Chunk.Type.PARTITIONED);
        chunk.addAllItems(items);
        return chunk;
    }

    private static FlowManager flowManager(String serviceUrl) {
        return new FlowManager(new FlowStoreServiceConnector(HttpClient.newClient(new ClientConfig().register(new JacksonFeature())), serviceUrl));
    }
}
