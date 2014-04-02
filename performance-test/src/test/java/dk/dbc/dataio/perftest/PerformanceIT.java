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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PerformanceIT {
    private static final String DATASET_FILE = "dataset.json";

    @Test
    public void performanceTest() throws JsonException, IOException, JobStoreServiceConnectorException {
        String flowStorebaseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        String jobStorebaseUrl = String.format("http://localhost:%s/job-store", System.getProperty("glassfish.port"));
        Client restClient = HttpClient.newClient();

        // insert submitter:
        SubmitterContent submitterContent = new SubmitterContent(424242L, "perftestbib", "Library for performancetest");
        long submitterId = insertObjectInFlowStore(restClient, flowStorebaseUrl, submitterContent, FlowStoreServiceConstants.SUBMITTERS);

        // insert flowcomponent with javascript with no functionality:
        JavaScript js = new JavaScript("function invocationFunction(record, supplementaryData) { return \"Hello from javascript!\" }", "NoModule");
        FlowComponentContent flowComponentContent = new FlowComponentContent("perftest-flow-component", "No_SVN_project", 1L, "invocationJavaScript", Arrays.asList(js), "invocationFunction");
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
        FlowBinderContent flowBinderContent = new FlowBinderContent("perftest-flowbinder", "flowbinder for perftest", "xml", "testdata", "utf8", "dummysink", "Default Record Splitter", flowId, Arrays.asList(new Long(submitterId)), sinkId);
        insertObjectInFlowStore(restClient, flowStorebaseUrl, flowBinderContent, FlowStoreServiceConstants.FLOW_BINDERS);

        // Create Job
        JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(restClient, jobStorebaseUrl);
        JobSpecification jobSpec = new JobSpecification("xml", "testdata", "utf8", "dummysink", 424242L, "jda@dbc.dk", "jda@dbc.dk", "jda", "/home/damkjaer/svn.dbc.dk/repos/dataio/trunk/performance-test/data.xml");
        //JobSpecification jobSpec = new JobSpecification("xml", "testdata", "utf8", "dummysink", 424242L, "jda@dbc.dk", "jda@dbc.dk", "jda", "/home/jbn/dev/repos/dataio/performance-test/data.xml");

        // Start timer
        long timer = System.currentTimeMillis();
        // Insert Job
        JobInfo jobInfo = jobStoreServiceConnector.createJob(jobSpec);

        boolean done = false;
        // Wait for Job-completion
        while(!done) {
            JobState jobState = jobStoreServiceConnector.getState(jobInfo.getJobId());
            if(jobState.getLifeCycleStateFor(JobState.OperationalState.DELIVERING) == JobState.LifeCycleState.DONE) {
                done = true;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PerformanceIT.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        timer = System.currentTimeMillis() - timer;
        // End Timer

        updateDataset(timer);

        // read json with previous data-points
        // Add new data point to json
        // Render graph
        // Write graph to main.png file
        createChart();
    }

    private void updateDataset(long measurement) throws IOException {
        final Dataset dataset = Dataset.fromJsonFile(DATASET_FILE);
        Dataset.DatasetValue datasetValue = new Dataset.DatasetValue();
        datasetValue.value = measurement;
        datasetValue.rowKey = "keyX";
        datasetValue.columnKey = "keyY";
        dataset.addValue(datasetValue);
        Dataset.toJsonFile(DATASET_FILE, dataset);
    }

    private <T> long insertObjectInFlowStore(Client restClient, String baseUrl, T type, String restEndPoint) throws JsonException {
        final String json = JsonUtil.toJson(type);
        final Response response = HttpClient.doPostWithJson(restClient, json, baseUrl, restEndPoint);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));
        long id = ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue(response);
        return id;
    }


    private void createChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(5.0, "alfa", "beta");
        JFreeChart lineChart = ChartFactory.createLineChart("This is a title", "SomeAxis", "SomeOtherAxis", dataset);
        try {
            ChartUtilities.saveChartAsPNG(new File("main.png"), lineChart, 320, 200);
        } catch (IOException ex) {
            Logger.getLogger(PerformanceIT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static class DatasetValue {
        public Number value;
        public String rowKey;
        public String columnKey;
    }
}
