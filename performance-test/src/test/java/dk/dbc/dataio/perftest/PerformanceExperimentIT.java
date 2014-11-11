package dk.dbc.dataio.perftest;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Ignore
public class PerformanceExperimentIT {
    private static Logger LOGGER = LoggerFactory.getLogger(PerformanceExperimentIT.class);

    private static final String DATASET_FILE = "dataset.json";

    private static long lowContentTimingResult = 0; // to be set from low-content test
    private static long highContentTimingResult = 0; // to be set from high-content test
    private static long timestampForTestStart = 0;

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
    public void test() throws JsonException, IOException, JobStoreServiceConnectorException {
        // Create test data
        File testdata = new File("/home/jbn/1200");

        // Initialize flow-store
        JobStoreServiceConnector jobStoreServiceConnector = initializeFlowStore();

        // Create JobSpec
        JobSpecification jobSpec = new JobSpecification("xml", "basis", "utf8", "dummysink", 870970L, "jda@dbc.dk", "jda@dbc.dk", "jda", testdata.getAbsolutePath());

        // Start timer
        long timer = System.currentTimeMillis();

        // Insert Job
        JobInfo jobInfo = jobStoreServiceConnector.createJob(jobSpec);

        // Wait for job-completion
        waitForCompletion(jobStoreServiceConnector, jobInfo.getJobId());

        // End Timer
        lowContentTimingResult = System.currentTimeMillis() - timer;
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

    private JobStoreServiceConnector initializeFlowStore() throws JsonException, IOException {

        String flowStorebaseUrl = ITUtil.FLOW_STORE_BASE_URL;
        String jobStorebaseUrl = ITUtil.JOB_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();

        // insert submitter:
        SubmitterContent submitterContent = new SubmitterContent(870970L, "perftestbib", "Library for performancetest");
        long submitterId = insertObjectInFlowStore(restClient, flowStorebaseUrl, submitterContent, FlowStoreServiceConstants.SUBMITTERS);

        // insert flow:
        final String flowString = readResourceFromClassPath("flow.json");
        final Flow flow = JsonUtil.fromJson(flowString, Flow.class, MixIns.getMixIns());
        long flowId = insertObjectInFlowStore(restClient, flowStorebaseUrl, flow.getContent(), FlowStoreServiceConstants.FLOWS);

        // insert sink:
        SinkContent sinkContent = new SinkContent("perftest-dummy-sink-" + UUID.randomUUID().toString(), "jdbc/dataio/dummy");
        long sinkId = insertObjectInFlowStore(restClient, flowStorebaseUrl, sinkContent, FlowStoreServiceConstants.SINKS);

        // insert flowbinder
        FlowBinderContent flowBinderContent = new FlowBinderContent("perftest-flowbinder", "flowbinder for perftest", "xml", "basis", "utf8", "dummysink", "Default Record Splitter", false, flowId, Arrays.asList(Long.valueOf(submitterId)), sinkId);
        insertObjectInFlowStore(restClient, flowStorebaseUrl, flowBinderContent, FlowStoreServiceConstants.FLOW_BINDERS);

        return new JobStoreServiceConnector(restClient, jobStorebaseUrl);
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
        JFreeChart lineChart = ChartFactory.createLineChart("DataIO performance test - A job with records", "Timestamp", "Total time in milliseconds", lowContentDataset, PlotOrientation.VERTICAL, true, false, false);
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
