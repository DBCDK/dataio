package dk.dbc.dataio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.DataPartitionerFactory;
import dk.dbc.dataio.commons.partioner.DataPartitionerResult;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.diff.MessageConsumerBean;
import dk.dbc.httpclient.HttpClient;
import jakarta.xml.bind.JAXB;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import picocli.CommandLine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@CommandLine.Command(name = "acc-test-runner.sh", mixinStandardHelpOptions = true, showDefaultValues = true, version = "1.0")
public class AccTestRunner implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Job specification file")
    private Path jobSpec;
    @CommandLine.Parameters(index = "1", description = "Record splitter <ADDI|ADDI_MARC_XML|CSV|DANMARC2_LINE_FORMAT|DANMARC2_LINE_FORMAT_COLLECTION|" +
            "DSD_CSV|ISO2709|ISO2709_COLLECTION|JSON|VIAF|VIP_CSV|XML|TARRED_XML|ZIPPED_XML>")
    private RecordSplitter recordSplitter;
    @CommandLine.Parameters(index = "2", description = "Data file")
    private Path datafile;
    @CommandLine.Option(names = "-f", description = "Flowstore url (prod flowstore is default)", defaultValue = "http://dataio-flowstore-service.metascrum-prod.svc.cloud.dbc.dk/dataio/flow-store-service")
    private FlowStoreServiceConnector flowstore;
    @CommandLine.Option(names = "-s", description = "Path to local script", required = true)
    private Path nextScripts;
    @CommandLine.Option(names = "-d", description = "Search path for dependencies", required = true)
    private Path dependencies;
    @CommandLine.Option(names = "-r", description = "Report format <TEXT|XML>", defaultValue = "TEXT")
    private ReportFormat reportFormat;


    public static void main(String[] args) {
        new CommandLine(new AccTestRunner())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .registerConverter(Path.class, Path::of)
                .registerConverter(FlowStoreServiceConnector.class, AccTestRunner::flowStoreServiceConnector)
                .execute(args);
    }

    @Override
    public Integer call() throws Exception {
        if(!Files.isReadable(jobSpec)) throw new IllegalArgumentException("Job specification " + jobSpec + " is not a readable file");
        if(!Files.isReadable(datafile)) throw new IllegalArgumentException("Datafile " + datafile + " is not a readable file");
        if(!Files.isReadable(nextScripts)) throw new IllegalArgumentException("Local script file " + nextScripts + " is not a readable file");
        if(!Files.isDirectory(dependencies)) throw new IllegalArgumentException("Path for dependencies " + dependencies + " is not valid");
        JobSpecification jobSpecification = new ObjectMapper().readValue(jobSpec.toFile(), JobSpecification.class);
        Flow flow = getFlow(jobSpecification);
        Chunk chunk = readChunk(jobSpecification);
        Chunk processed = processChunk(flow, chunk);
        ServiceHub serviceHub = new ServiceHub.Builder().withJobStoreServiceConnector(null).build();
        Chunk diff = new MessageConsumerBean(serviceHub).handleChunk(processed);
        reportFormat.printDiff(flow, diff);
        return 0;
    }

    private Flow getFlow(JobSpecification jobSpecification) throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = flowstore.getFlowBinder(jobSpecification.getPackaging(), jobSpecification.getFormat(), jobSpecification.getCharset(), jobSpecification.getSubmitterId(), jobSpecification.getDestination());
        return flowstore.getFlow(flowBinder.getContent().getFlowId());
    }

    private Chunk processChunk(Flow flow, Chunk chunk) throws Exception {
        if(nextScripts != null) overwriteNext(flow);
        ChunkProcessor processor = new ChunkProcessor(null, id -> flow);
        return processor.process(chunk, flow.getId(), flow.getVersion(), "");
    }

    private void overwriteNext(Flow flow) throws Exception {
        JavaScriptProject project = JavaScriptProject.of(nextScripts, dependencies);
        FlowComponent component = flow.getContent().getComponents().get(0);
        FlowComponentContent next = component.getNext();
        component.withNext(new FlowComponentContent(next.getName(), next.getSvnProjectForInvocationJavascript(), 1, next.getInvocationJavascriptName(), project.getJavaScripts(), next.getInvocationMethod(), next.getDescription()));
    }

    private Chunk readChunk(JobSpecification jobSpecification) throws IOException {
        try(InputStream is = new BufferedInputStream(Files.newInputStream(datafile))) {
            DataPartitioner partitionerResults = DataPartitionerFactory.createNoReordering(recordSplitter, is, jobSpecification.getCharset());
            Stream<DataPartitionerResult> stream = StreamSupport.stream(partitionerResults.spliterator(), false);
            AtomicLong id = new AtomicLong(0);
            List<ChunkItem> items = stream.map(DataPartitionerResult::getChunkItem).map(ci -> ci.withId(id.getAndIncrement())).collect(Collectors.toList());
            Chunk chunk = new Chunk(0, 0, Chunk.Type.PARTITIONED);
            chunk.addAllItems(items);
            return chunk;
        }
    }

    private static FlowStoreServiceConnector flowStoreServiceConnector(String serviceUrl) {
        return new FlowStoreServiceConnector(HttpClient.newClient(new ClientConfig().register(new JacksonFeature())), serviceUrl);
    }

    public enum ReportFormat {
        TEXT {
            public void printDiff(Flow flow, Chunk chunk) {
                chunk.getItems().stream().filter(ci -> ci.getStatus() != ChunkItem.Status.SUCCESS).forEach(ci -> {
                    System.out.println(ci.getStatus() + " - ChunkItem: " + ci.getId());
                    System.out.println(new String(ci.getData(), ci.getEncoding()));
                });
            }
        },
        XML {
            public void printDiff(Flow flow, Chunk chunk) {
                List<Testsuite.TestCase> cases = chunk.getItems().stream().map(Testsuite.TestCase::from).collect(Collectors.toList());
                String name = flow.getContent().getName();
                Testsuite testsuite = new Testsuite(name, cases);
                File file = new File("TESTS-dbc_" + flow.getId() + "-" + name + ".xml");
                JAXB.marshal(testsuite, file);
                System.out.println("Wrote report to " + file.getAbsolutePath());
            }
        };

        public abstract void printDiff(Flow flow, Chunk chunk);
    }


}
