package dk.dbc.dataio.perftest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.apache.commons.codec.binary.Base64;
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

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PerformanceIT {

    private static Logger LOGGER = LoggerFactory.getLogger(PerformanceIT.class);

    private static final String DATASET_FILE = "dataset.json";

    private static final long RECORDS_PER_TEST = 10000;
    private static long lowContentTimingResult = 0; // to be set from low-content test
    private static long highContentTimingResult = 0; // to be set from high-content test
    private static long timestampForTestStart = 0;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setStartTime() {
        timestampForTestStart = System.currentTimeMillis();
    }

    @After
    public void clearDb() throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.newDbConnection(ITUtil.FLOW_STORE_DATABASE_NAME)) {
            ITUtil.clearAllDbTables(connection);
        }
    }

    @After
    public void clearJobStore() {
        ITUtil.clearJobStore();
    }

    @AfterClass
    public static void updateDataPointsAndCreateGraphImage() throws IOException {
        Dataset dataset = updateDataset(timestampForTestStart, lowContentTimingResult, highContentTimingResult);
        createChart(dataset);
    }

    @Test
    public void lowContentPerformanceTest() throws JsonException, IOException, JobStoreServiceConnectorException {
        // Create test data
        File testdata = tmpFolder.newFile();
        createTemporaryFile(testdata, RECORDS_PER_TEST, "low");

        // Initialize flow-store
        JobStoreServiceConnector jobStoreServiceConnector = initializeFlowStore(getJavaScriptsForSmallPerformanceTest());

        // Create JobSpec
        JobSpecification jobSpec = new JobSpecification("xml", "testdata", "utf8", "dummysink", 424242L, "jda@dbc.dk", "jda@dbc.dk", "jda", testdata.getAbsolutePath());

        // Start timer
        long timer = System.currentTimeMillis();

        // Insert Job
        JobInfo jobInfo = jobStoreServiceConnector.createJob(jobSpec);

        // Wait for job-completion
        waitForCompletion(jobStoreServiceConnector, jobInfo.getJobId());

        // End Timer
        lowContentTimingResult = System.currentTimeMillis() - timer;
    }

    @Test
    public void highContentPerformanceTest() throws JsonException, IOException, JobStoreServiceConnectorException {
        // Create test data
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\n");
        }
        File testdata = tmpFolder.newFile();
        createTemporaryFile(testdata, RECORDS_PER_TEST, sb.toString());

        // Initialize flow-store
        JobStoreServiceConnector jobStoreServiceConnector = initializeFlowStore(getJavaScriptsForLargePerformanceTest());

        // Create JobSpec
        JobSpecification jobSpec = new JobSpecification("xml", "testdata", "utf8", "dummysink", 424242L, "jda@dbc.dk", "jda@dbc.dk", "jda", testdata.getAbsolutePath());

        // Start timer
        long timer = System.currentTimeMillis();

        // Insert Job
        JobInfo jobInfo = jobStoreServiceConnector.createJob(jobSpec);

        // Wait for job-completion
        waitForCompletion(jobStoreServiceConnector, jobInfo.getJobId());

        // End Timer
        highContentTimingResult = System.currentTimeMillis() - timer;
    }

    private void waitForCompletion(JobStoreServiceConnector jobStoreServiceConnector, long jobId) throws ProcessingException, JobStoreServiceConnectorException {
        boolean done = false;
        // Wait for Job-completion
        while (!done) {
            JobState jobState = jobStoreServiceConnector.getState(jobId);
            if (jobState.getLifeCycleStateFor(JobState.OperationalState.DELIVERING) == JobState.LifeCycleState.DONE) {
                done = true;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    private void createTemporaryFile(File f, long numberOfElements, String data) throws UnsupportedEncodingException, IOException {
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

    private JobStoreServiceConnector initializeFlowStore(List<JavaScript> javaScripts) throws JsonException, UnsupportedEncodingException {

        String flowStorebaseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        String jobStorebaseUrl = String.format("http://localhost:%s/job-store", System.getProperty("glassfish.port"));
        Client restClient = HttpClient.newClient();

        // insert submitter:
        SubmitterContent submitterContent = new SubmitterContent(424242L, "perftestbib", "Library for performancetest");
        long submitterId = insertObjectInFlowStore(restClient, flowStorebaseUrl, submitterContent, FlowStoreServiceConstants.SUBMITTERS);

        // insert flowcomponent with javascripts:
        FlowComponentContent flowComponentContent = new FlowComponentContent("perftest-flow-component", "No_SVN_project", 1L, "invocationJavaScript", javaScripts, "invocationFunction");
        long flowComponentId = insertObjectInFlowStore(restClient, flowStorebaseUrl, flowComponentContent, FlowStoreServiceConstants.FLOW_COMPONENTS);

        // get flowcomponent:
        // The flow needs the complete flowcomponent to be created - not just the flowcomponentcontent, hence we need to retrieve the flowcomponent.
        final Response response = HttpClient.doGet(restClient, flowStorebaseUrl, FlowStoreServiceConstants.FLOW_COMPONENTS);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));
        final String responseContent = response.readEntity(String.class);
        assertThat(responseContent, is(notNullValue()));
        final ArrayNode responseContentNode = (ArrayNode) JsonUtil.getJsonRoot(responseContent);
        assertThat(responseContentNode.get(0).get("id").longValue(), is(flowComponentId));
        String flowComponentJson = responseContentNode.get(0).toString();
        FlowComponent flowComponent = JsonUtil.fromJson(flowComponentJson, FlowComponent.class);

        System.out.println("response: " + flowComponentJson);

        // insert flow:
        FlowContent flowContent = new FlowContent("perftest-flow", "Flow for perftest", Arrays.asList(flowComponent));
        long flowId = insertObjectInFlowStore(restClient, flowStorebaseUrl, flowContent, FlowStoreServiceConstants.FLOWS);

        // insert sink:
        SinkContent sinkContent = new SinkContent("perftest-dummy-sink", "jdbc/dataio/dummy");
        long sinkId = insertObjectInFlowStore(restClient, flowStorebaseUrl, sinkContent, FlowStoreServiceConstants.SINKS);

        // insert flowbinder
        FlowBinderContent flowBinderContent = new FlowBinderContent("perftest-flowbinder", "flowbinder for perftest", "xml", "testdata", "utf8", "dummysink", "Default Record Splitter", flowId, Arrays.asList(Long.valueOf(submitterId)), sinkId);
        insertObjectInFlowStore(restClient, flowStorebaseUrl, flowBinderContent, FlowStoreServiceConstants.FLOW_BINDERS);

        return new JobStoreServiceConnector(restClient, jobStorebaseUrl);
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
    private List<JavaScript> getJavaScriptsForLargePerformanceTest() throws UnsupportedEncodingException, IOException {
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

    private <T> long insertObjectInFlowStore(Client restClient, String baseUrl, T type, String restEndPoint) throws JsonException {
        final String json = JsonUtil.toJson(type);
        final Response response = HttpClient.doPostWithJson(restClient, json, baseUrl, restEndPoint);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));
        long id = ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue(response);
        return id;
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
