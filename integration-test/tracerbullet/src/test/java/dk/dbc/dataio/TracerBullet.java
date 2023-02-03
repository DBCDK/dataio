package dk.dbc.dataio;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.httpclient.HttpClient;
import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.client.Client;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TracerBullet {
    private static final long RECORDS_PER_SHOT_FIRED = 11;
    private static final long SLEEP_INTERVAL_IN_MS = 250;
    private static final long MAX_WAIT_IN_MS = 60000;
    private static final long SUBMITTER_NUMBER = 424242L;
    private static final String PACKAGING = "xml";
    private static final String FORMAT = "testdata";
    private static final String CHARSET = "utf8";
    private static final String DESTINATION = "testbroend";

    private static final Properties ENV = new Properties();

    private static FileStoreServiceConnector fileStoreServiceConnector;
    private static FlowStoreServiceConnector flowStoreServiceConnector;
    private static JobStoreServiceConnector jobStoreServiceConnector;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void getEnvironmentProperties() throws IOException {
        try (InputStream is = new FileInputStream(System.getProperty("env.properties"))) {
            ENV.load(is);
        }
        final String filestoreBaseurl = ENV.getProperty("FILE_STORE_SERVICE_ENDPOINT");
        final String flowstoreBaseurl = ENV.getProperty("FLOW_STORE_SERVICE_ENDPOINT");
        final String jobstoreBaseurl = ENV.getProperty("JOB_STORE_SERVICE_ENDPOINT");
        System.out.println("file-store baseurl: " + filestoreBaseurl);
        System.out.println("flow-store baseurl: " + flowstoreBaseurl);
        System.out.println("job-store baseurl: " + jobstoreBaseurl);

        final Client httpClient = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));

        fileStoreServiceConnector = new FileStoreServiceConnector(httpClient, filestoreBaseurl);
        flowStoreServiceConnector = new FlowStoreServiceConnector(httpClient, flowstoreBaseurl);
        jobStoreServiceConnector = new JobStoreServiceConnector(httpClient, jobstoreBaseurl, null);
    }

    @Test
    public void fire() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException, JobStoreServiceConnectorException, IOException, URISyntaxException {
        initialiseFlowStore();
        final FileStoreUrn dataFileUrn = createDataFile();
        final JobInfoSnapshot jobInfoSnapshot = createJob(dataFileUrn);
        waitForJobCompletion(jobInfoSnapshot.getJobId());
    }

    private void initialiseFlowStore() throws FlowStoreServiceConnectorException, UnsupportedEncodingException {
        final Submitter submitter = createSubmitter();
        final FlowComponent flowComponent = createFlowComponent();
        final Flow flow = createFlow(flowComponent);
        final Sink sink = createSink();
        createFlowBinder(flow, submitter, sink);
    }

    /* Creates a tiny javascript for use in the tracer bullet. */
    private List<JavaScript> getTinyJavaScript() throws UnsupportedEncodingException {
        // This method must return a list of javascripts where the first javascript has a function called
        // invocationfunction for use as entrance to the javascripts.
        return new ArrayList<>(Arrays.asList(
                new JavaScript(Base64.encodeBase64String((
                        "function invocationFunction(record, supplementaryData) {\n"
                                + "    return \"Hello from javascript!\\n\";"
                                + "}").getBytes("UTF-8")), "NoModule"),
                new JavaScript(ResourceReader.getResourceAsBase64(TracerBullet.class, "javascript/jscommon/system/Use.use.js"), "Use"),
                new JavaScript(ResourceReader.getResourceAsBase64(TracerBullet.class, "javascript/jscommon/system/ModulesInfo.use.js"), "ModulesInfo"),
                new JavaScript(ResourceReader.getResourceAsBase64(TracerBullet.class, "javascript/jscommon/system/Use.RequiredModules.use.js"), "Use.RequiredModules"),
                new JavaScript(ResourceReader.getResourceAsBase64(TracerBullet.class, "javascript/jscommon/external/ES5.use.js"), "ES5"),
                new JavaScript(ResourceReader.getResourceAsBase64(TracerBullet.class, "javascript/jscommon/system/Engine.use.js"), "Engine")));
    }

    private Submitter createSubmitter() throws FlowStoreServiceConnectorException {
        final String submitterName = "tracer-bullet-submitter";
        final SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setName(submitterName)
                .setNumber(SUBMITTER_NUMBER)
                .setPriority(null)
                .build();

        try {
            return flowStoreServiceConnector.createSubmitter(submitterContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // Only necessary as long as flowstore service/connector has no support for deletion
            if (406 != e.getStatusCode()) {
                throw e;
            }
        }
        return null;
    }

    private FlowComponent createFlowComponent() throws UnsupportedEncodingException, FlowStoreServiceConnectorException {
        final String componentName = "tracer-bullet-component";
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(componentName)
                .setDescription("Minimalt script til basal system test")
                .setJavascripts(getTinyJavaScript())
                .setInvocationJavascriptName("tracer-bullet-javascript")
                .setInvocationMethod("invocationFunction")
                .build();
        try {
            return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // Only necessary as long as flowstore service/connector has no support for deletion
            if (406 != e.getStatusCode()) {
                throw e;
            }
        }
        return null;
    }

    private Flow createFlow(FlowComponent flowComponent) throws FlowStoreServiceConnectorException {
        if (flowComponent == null) {
            // Only necessary as long as flowstore service/connector has no support for deletion
            flowComponent = new FlowComponentBuilder().build();
        }
        final String flowName = "tracer-bullet-flow";
        final FlowContent flowContent = new FlowContentBuilder()
                .setName(flowName)
                .setComponents(Arrays.asList(flowComponent))
                .build();
        try {
            return flowStoreServiceConnector.createFlow(flowContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // Only necessary as long as flowstore service/connector has no support for deletion
            if (406 != e.getStatusCode()) {
                throw e;
            }
        }
        return null;
    }

    private Sink createSink() throws FlowStoreServiceConnectorException {
        final String sinkName = "tracer-bullet-sink";
        final String resource = "jdbc/dataio/dummy";
        final SinkContent sinkContent = new SinkContentBuilder()
                .setName(sinkName)
                .setResource(resource)
                .build();
        try {
            return flowStoreServiceConnector.createSink(sinkContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // Only necessary as long as flowstore service/connector has no support for deletion
            if (406 != e.getStatusCode()) {
                throw e;
            }
        }
        return null;
    }

    private void createFlowBinder(Flow flow, Submitter submitter, Sink sink) throws FlowStoreServiceConnectorException {
        if (flow != null && submitter != null && sink != null) {
            final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                    .setName("tracer-bullet-binder")
                    .setDescription("flowbinder for tracer-bullet")
                    .setPackaging(PACKAGING)
                    .setFormat(FORMAT)
                    .setCharset(CHARSET)
                    .setDestination(DESTINATION)
                    .setFlowId(flow.getId())
                    .setSinkId(sink.getId())
                    .setSubmitterIds(Arrays.asList(submitter.getId()))
                    .setPriority(null)
                    .build();
            try {
                flowStoreServiceConnector.createFlowBinder(flowBinderContent);
            } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
                if (406 != e.getStatusCode()) {
                    throw e;
                }
            }
        }
    }

    private FileStoreUrn createDataFile() throws IOException, FileStoreServiceConnectorException {
        final File bulletData = createTemporaryFile(RECORDS_PER_SHOT_FIRED, "data");
        try (InputStream is = new BufferedInputStream(new FileInputStream(bulletData))) {
            return FileStoreUrn.create(fileStoreServiceConnector.addFile(is));
        }
    }

    private JobInfoSnapshot createJob(FileStoreUrn fileStoreUrn) throws JobStoreServiceConnectorException {
        final JobSpecification jobSpecification = new JobSpecification()
                .withSubmitterId(SUBMITTER_NUMBER)
                .withPackaging(PACKAGING)
                .withFormat(FORMAT)
                .withCharset(CHARSET)
                .withDestination(DESTINATION)
                .withDataFile(fileStoreUrn.toString())
                .withType(JobSpecification.Type.TEST);
        try {
            return jobStoreServiceConnector.addJob(new JobInputStream(jobSpecification, true, 0));
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            throw new IllegalStateException(e.getJobError().getDescription() + "\n\n" + e.getJobError().getStacktrace());
        }
    }

    private File createTemporaryFile(long numberOfElements, String data) throws IOException {
        final File f = tmpFolder.newFile();
        final String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<container>\n";
        final String tail = "</container>\n";
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath(), Charset.forName("utf-8"))) {
            bw.write(head);
            for (long i = 0; i < numberOfElements; i++) {
                bw.write("  <record>" + data + i + "</record>\n");
            }
            bw.write(tail);
        }
        return f;
    }

    private JobInfoSnapshot waitForJobCompletion(long jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        // Wait for Job-completion
        long remainingWaitInMs = MAX_WAIT_IN_MS;

        while (remainingWaitInMs > 0) {
            jobInfoSnapshot = jobStoreServiceConnector.listJobs(criteria).get(0);
            if (allPhasesAreDoneSuccessfully(jobInfoSnapshot)) {
                break;
            } else {
                try {
                    Thread.sleep(SLEEP_INTERVAL_IN_MS);
                    remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        if (!allPhasesAreDoneSuccessfully(jobInfoSnapshot)) {
            throw new IllegalStateException(String.format("Job %d did not complete successfully in time",
                    jobInfoSnapshot.getJobId()));
        }

        return jobInfoSnapshot;
    }

    private boolean allPhasesAreDoneSuccessfully(JobInfoSnapshot jobInfoSnapshot) {
        final State state = jobInfoSnapshot.getState();
        return state.allPhasesAreDone() && state.getPhase(State.Phase.DELIVERING).getSucceeded() == RECORDS_PER_SHOT_FIRED;
    }
}
