package dk.dbc.dataio.perftest;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
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
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.client.ClientConfig;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PerformanceIT {
    private static Logger LOGGER = LoggerFactory.getLogger(PerformanceIT.class);

    private static final String DATASET_FILE = "dataset.json";
    private static final String PACKAGING = "xml";
    private static final String FORMAT = "testdata";
    private static final String ENCODING = "utf8";
    private static final String DESTINATION = "dummysink";
    private static final String SINK_RESOURCE = "jdbc/dataio/dummy";
    private static final long SUBMITTER_NUMBER = 424242;

    private static final long RECORDS_PER_TEST = 10000;
    private static long lowContentTimingResult = 0; // to be set from low-content test
    private static long highContentTimingResult = 0; // to be set from high-content test
    private static long timestampForTestStart = 0;
    private static FlowStoreServiceConnector flowStoreServiceConnector;
    private static JobStoreServiceConnector jobStoreServiceConnector;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupClass() throws ClassNotFoundException {
        final Client httpClient = HttpClient.newClient(new ClientConfig()
                .register(new Jackson2xFeature()));

        flowStoreServiceConnector = new FlowStoreServiceConnector(httpClient, ITUtil.FLOW_STORE_BASE_URL);
        jobStoreServiceConnector = new JobStoreServiceConnector(httpClient, ITUtil.NEW_JOB_STORE_BASE_URL);
    }

    @BeforeClass
    public static void setStartTime() {
        timestampForTestStart = System.currentTimeMillis();
    }

    @After
    public void clearJobStore() {
        ITUtil.clearJobStore();
    }

    @After
    public void clearFlowStore() {
        ITUtil.clearFlowStore();
    }

    @AfterClass
    public static void updateDataPointsAndCreateGraphImage() throws IOException {
        Dataset dataset = updateDataset(timestampForTestStart, lowContentTimingResult, highContentTimingResult);
        createChart(dataset);
    }

    @Test
    public void lowContentPerformanceTest() throws IOException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Create test data
        File testdata = tmpFolder.newFile();
        createTemporaryFile(testdata, RECORDS_PER_TEST, "low");

        // Initialize flow-store
        initializeFlowStore(getJavaScriptsForSmallPerformanceTest());

        // Create JobSpec
        final JobSpecification jobSpec = new JobSpecificationBuilder()
                .setPackaging(PACKAGING)
                .setFormat(FORMAT)
                .setCharset(ENCODING)
                .setDestination(DESTINATION)
                .setSubmitterId(SUBMITTER_NUMBER)
                .setDataFile(testdata.getAbsolutePath())
                .build();

        // Insert Job
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(new JobInputStream(jobSpec, true, 0));

        // Wait for job-completion
        jobInfoSnapshot = waitForCompletion(jobInfoSnapshot.getJobId());

        lowContentTimingResult = jobInfoSnapshot.getTimeOfCompletion().getTime() - jobInfoSnapshot.getTimeOfCreation().getTime();
    }

    @Test
    public void highContentPerformanceTest() throws IOException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Create test data
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\n");
        }
        File testdata = tmpFolder.newFile();
        createTemporaryFile(testdata, RECORDS_PER_TEST, sb.toString());

        // Initialize flow-store
        initializeFlowStore(getJavaScriptsForLargePerformanceTest());

        // Create JobSpec
        final JobSpecification jobSpec = new JobSpecificationBuilder()
                .setPackaging(PACKAGING)
                .setFormat(FORMAT)
                .setCharset(ENCODING)
                .setDestination(DESTINATION)
                .setSubmitterId(SUBMITTER_NUMBER)
                .setDataFile(testdata.getAbsolutePath())
                .build();

        // Insert Job
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(new JobInputStream(jobSpec, true, 0));

        // Wait for job-completion
        jobInfoSnapshot = waitForCompletion(jobInfoSnapshot.getJobId());

        highContentTimingResult = jobInfoSnapshot.getTimeOfCompletion().getTime() - jobInfoSnapshot.getTimeOfCreation().getTime();
    }

    private JobInfoSnapshot waitForCompletion(long jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        boolean done = false;
        // Wait for Job-completion
        while (!done) {
            jobInfoSnapshot = jobStoreServiceConnector.listJobs(criteria).get(0);
            if (jobInfoSnapshot.getState().allPhasesAreDone()) {
                done = true;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
        return jobInfoSnapshot;
    }

    private void createTemporaryFile(File f, long numberOfElements, String data) throws IOException {
        final String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<container>\n";
        final String tail = "</container>\n";
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath(), Charset.forName("utf-8"))) {
            bw.write(head);
            for (long i = 0; i < numberOfElements; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("  <record>").append(data).append(i).append("</record>\n");
                bw.write(sb.toString());
            }
            bw.write(tail);
        }
    }

    private void initializeFlowStore(List<JavaScript> javaScripts) throws FlowStoreServiceConnectorException {
        // insert submitter:
        final SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setNumber(SUBMITTER_NUMBER)
                .build();
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);

        // insert flowcomponent with javascripts:
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setInvocationJavascriptName("invocationJavaScript")
                .setJavascripts(javaScripts)
                .setInvocationMethod("invocationFunction")
                .build();
        final FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        // insert flow:
        final FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Arrays.asList(flowComponent))
                .build();
        final Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // insert sink:
        final SinkContent sinkContent = new SinkContentBuilder()
                .setName("perftest-dummy-sink-" + UUID.randomUUID().toString())
                .setResource(SINK_RESOURCE)
                .build();
        final Sink sink = flowStoreServiceConnector.createSink(sinkContent);

        // insert flowbinder
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setPackaging(PACKAGING)
                .setFormat(FORMAT)
                .setCharset(ENCODING)
                .setDestination(DESTINATION)
                .setSequneceAnalysis(true)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId()))
                .build();
        flowStoreServiceConnector.createFlowBinder(flowBinderContent);
    }

    /*
     * Creates a the smallest possible javascripts for use in the performance-test.
     * In order to keep it small, "use" and "modules-info" are mocked.
     */
    private List<JavaScript> getJavaScriptsForSmallPerformanceTest() throws UnsupportedEncodingException {
        // This method must return a list of javascripts where the first javascript has a function called
        // invocationfunction for use as entrance to the javascripts.

        JavaScript js = new JavaScript(Base64.encodeBase64String(("function invocationFunction(record, supplementaryData) {\n"
                + "return \"Hello from javascript!\\n\";"
                + "}").getBytes("UTF-8")), "NoModule");
        JavaScript jsUse = new JavaScript(Base64.encodeBase64String("function use(module) {};".getBytes("UTF-8")), "Use");
        JavaScript jsModulesInfo = new JavaScript(Base64.encodeBase64String("var __ModulesInfo = function() { var that = {}; that.checkDepAlreadyLoaded = function( moduleName ) { return true; }; return that;}();".getBytes("UTF-8")), "ModulesInfo");
        return Arrays.asList(js, jsUse, jsModulesInfo);
    }

    /*
     * Creates realistic sized javascripts for use in the performance-test.
     */
    private List<JavaScript> getJavaScriptsForLargePerformanceTest() throws IOException {
        // This method must return a list of javascripts where the first javascript has a function called
        // invocationfunction for use as entrance to the javascripts.

        JavaScript js = new JavaScript(Base64.encodeBase64String(("use(\"MarcXchange\")\n\n"
                + "function invocationFunction(record, supplementaryData) {\n"
                + "  var instance = Packages.java.security.MessageDigest.getInstance(\"md5\");\n"
                + "  instance.update((new Packages.java.lang.String(record)).getBytes(\"UTF-8\"));\n"
                + "  var md5 = instance.digest();\n"
                + "  var res = String();\n"
                + "  for(var i=0; i<md5.length; i++) {\n"
                + "    res += (md5[i] & 0xFF).toString(16);\n"
                + "  }\n"
                + "  return res;\n"
                + "}").getBytes("UTF-8")), "NoModule");
        JavaScript jsUse = new JavaScript(Base64.encodeBase64String(readResourceFromClassPath("javascript/jscommon/system/Use.use.js").getBytes("UTF-8")), "Use");
        JavaScript jsModulesInfo = new JavaScript(Base64.encodeBase64String(readResourceFromClassPath("javascript/jscommon/system/ModulesInfo.use.js").getBytes("UTF-8")), "ModulesInfo");
        // The following resources are located in the test/resources folder in the project.
        // All files are copied from jscommon and the filename is prefixed with "TestResource_".
        // This is in order to ensure that they are not confused with the actual javascripts.
        // The purpose of these scripts are just to add bulk/volume to the chunk, and to ensure that a belivable
        // amount of javascript is read into the javascripts-environment during the test.
        List<String> testJSDependecies
                = Arrays.asList("Log",
                        "LogCore",
                        "Global",
                        "Marc",
                        "MarcClasses",
                        "MarcClassesCore",
                        "MarcMatchers",
                        "MarcXchange",
                        "Print",
                        "PrintCore",
                        "StringUtil",
                        "System",
                        "Underscore",
                        "UnitTest",
                        "Util",
                        "ValueCheck",
                        "XmlNamespaces",
                        "XmlUtil");

        List<JavaScript> javascripts = new ArrayList<>(Arrays.asList(js, jsUse, jsModulesInfo));
        for (String jsDependency : testJSDependecies) {
            javascripts.add(new JavaScript(Base64.encodeBase64String(readResourceFromClassPath("TestResource_" + jsDependency + ".use.js").getBytes("UTF-8")), jsDependency));
        }
        return javascripts;
    }

    private static Dataset updateDataset(long timestamp, long measurementLow, long measurementHigh) throws IOException {
        final Dataset dataset = Dataset.fromJsonFile(DATASET_FILE);
        Dataset.DatasetValue datasetValue = new Dataset.DatasetValue();
        datasetValue.lowContentTiming = measurementLow;
        datasetValue.highContentTiming = measurementHigh;
        datasetValue.timestamp = timestamp;
        dataset.addValue(datasetValue);
        Dataset.toJsonFile(DATASET_FILE, dataset);
        return dataset;
    }

    private static void createChart(Dataset ioDataset) {
        DefaultCategoryDataset lowContentDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset highContentDataset = new DefaultCategoryDataset();
        for (Dataset.DatasetValue value : ioDataset.getValues()) {
            lowContentDataset.addValue(value.lowContentTiming, "Low Content Test", value.timestamp);
            highContentDataset.addValue(value.highContentTiming, "High Content Test", value.timestamp);
        }
        JFreeChart lineChart = ChartFactory.createLineChart("DataIO performance test - A job with " + RECORDS_PER_TEST + " records", "Timestamp", "Total time in milliseconds", lowContentDataset, PlotOrientation.VERTICAL, true, false, false);
        CategoryPlot categoryPlot = lineChart.getCategoryPlot();
        categoryPlot.setDataset(1, highContentDataset);
        LineAndShapeRenderer highContentRenderer = new LineAndShapeRenderer(true, false);
        categoryPlot.setRenderer(1, highContentRenderer);
        categoryPlot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        try {
            ChartUtilities.saveChartAsPNG(new File("main.png"), lineChart, 1920, 960);
        } catch (IOException ex) {
            LOGGER.error("Could not create chart.", ex);
        }
    }

    private String readResourceFromClassPath(String resource) throws IOException {

        URL url = this.getClass().getClassLoader().getResource(resource);
        LOGGER.info("Reading resource '{}' from '{}'", resource, url.toString());
        final int byteArrayLength = 1024;
        byte[] buffer = new byte[byteArrayLength];
        StringBuilder sb = new StringBuilder();
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new AssertionError("The ressource '" + resource + "' could not be found on the classpath");
            }
            int sz = -1;
            do {
                sz = in.read(buffer, 0, buffer.length);
                if (sz > 0) {
                    sb.append(new String(buffer, 0, sz, "utf-8"));
                }
            } while (sz >= 0);
        }
        return sb.toString();
    }

}
