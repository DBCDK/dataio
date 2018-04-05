/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.perftest;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
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
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PerformanceIT {
    private static Logger LOGGER = LoggerFactory.getLogger(PerformanceIT.class);

    private static final long SLEEP_INTERVAL_IN_MS = 1000;
    private static final long MAX_WAIT_IN_MS = 3600000;
    private static final String DATASET_FILE = "dataset.json";
    private static final String PACKAGING = "xml";
    private static final String FORMAT = "testdata";
    private static final String ENCODING = "utf8";
    private static final String DESTINATION = "dummysink";
    private static final String SINK_RESOURCE = "jdbc/dataio/dummy";
    private static final String FILE_STORE_BASE_URL = String.format("http://%s:%s%s",
            System.getProperty("container.hostname"), System.getProperty("container.http.port"), System.getProperty("file-store-service.context"));
    public static final String FLOW_STORE_BASE_URL = String.format("http://%s:%s%s",
            System.getProperty("container.hostname"), System.getProperty("container.http.port"), System.getProperty("flow-store-service.context"));
    public static final String JOB_STORE_BASE_URL = String.format("http://%s:%s%s",
            System.getProperty("container.hostname"), System.getProperty("container.http.port"), System.getProperty("job-store-service.context"));

    private static final long RECORDS_PER_TEST = 10000;
    private static long lowContentTimingResult = 0; // to be set from low-content test
    private static long highContentTimingResult = 0; // to be set from high-content test
    private static long timestampForTestStart = 0;
    private static FileStoreServiceConnector fileStoreServiceConnector;
    private static FlowStoreServiceConnector flowStoreServiceConnector;
    private static JobStoreServiceConnector jobStoreServiceConnector;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupClass() throws ClassNotFoundException {
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(100);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(100);

        final ClientConfig config = new ClientConfig();
        config.property(ApacheClientProperties.CONNECTION_MANAGER, poolingHttpClientConnectionManager);
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        config.register(new JacksonFeature());

        final Client httpClient = HttpClient.newClient(config);

        fileStoreServiceConnector = new FileStoreServiceConnector(httpClient, FILE_STORE_BASE_URL);
        flowStoreServiceConnector = new FlowStoreServiceConnector(httpClient, FLOW_STORE_BASE_URL);
        jobStoreServiceConnector = new JobStoreServiceConnector(httpClient, JOB_STORE_BASE_URL);
    }

    @BeforeClass
    public static void setStartTime() {
        timestampForTestStart = System.currentTimeMillis();
    }

    @AfterClass
    public static void updateDataPointsAndCreateGraphImage() throws IOException {
        Dataset dataset = updateDataset(timestampForTestStart, lowContentTimingResult, highContentTimingResult);
        createChart(dataset);
    }

    @Test
    public void lowContentPerformanceTest() throws IOException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final long submitterNumber = 424242;

        // Create test data
        final File testdata = tmpFolder.newFile();
        createTemporaryFile(testdata, RECORDS_PER_TEST, "low");

        final String fileStoreId;
        try (final FileInputStream fileInputStream = new FileInputStream(testdata)) {
            fileStoreId = fileStoreServiceConnector.addFile(fileInputStream);
        }

        // Initialize flow-store
        initializeFlowStore("lowContent", submitterNumber, getJavaScriptsForSmallPerformanceTest());

        // Create JobSpec
        final JobSpecification jobSpec = new JobSpecification()
                .withPackaging(PACKAGING)
                .withFormat(FORMAT)
                .withCharset(ENCODING)
                .withDestination(DESTINATION)
                .withSubmitterId(submitterNumber)
                .withDataFile(FileStoreUrn.create(fileStoreId).toString());

        // Insert Job
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(new JobInputStream(jobSpec, true, 0));

        // Wait for job-completion
        jobInfoSnapshot = waitForJobCompletion(jobInfoSnapshot.getJobId());

        lowContentTimingResult = jobInfoSnapshot.getTimeOfCompletion().getTime() - jobInfoSnapshot.getTimeOfCreation().getTime();
    }

    @Test
    public void highContentPerformanceTest() throws IOException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final long submitterNumber = 434343;

        // Create test data
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\n");
        }
        final File testdata = tmpFolder.newFile();
        createTemporaryFile(testdata, RECORDS_PER_TEST, sb.toString());

        final String fileStoreId;
        try (final FileInputStream fileInputStream = new FileInputStream(testdata)) {
            fileStoreId = fileStoreServiceConnector.addFile(fileInputStream);
        }

        // Initialize flow-store
        initializeFlowStore("highContent", submitterNumber, getJavaScriptsForLargePerformanceTest());

        // Create JobSpec
        final JobSpecification jobSpec = new JobSpecification()
                .withPackaging(PACKAGING)
                .withFormat(FORMAT)
                .withCharset(ENCODING)
                .withDestination(DESTINATION)
                .withSubmitterId(submitterNumber)
                .withDataFile(FileStoreUrn.create(fileStoreId).toString());

        // Insert Job
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(new JobInputStream(jobSpec, true, 0));

        // Wait for job-completion
        jobInfoSnapshot = waitForJobCompletion(jobInfoSnapshot.getJobId());

        highContentTimingResult = jobInfoSnapshot.getTimeOfCompletion().getTime() - jobInfoSnapshot.getTimeOfCreation().getTime();
    }

    private JobInfoSnapshot waitForJobCompletion(long jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        // Wait for Job-completion
        long remainingWaitInMs = MAX_WAIT_IN_MS;

        while ( remainingWaitInMs > 0 ) {
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
        return state.allPhasesAreDone() && state.getPhase(State.Phase.DELIVERING).getSucceeded() == RECORDS_PER_TEST;
    }

    private void createTemporaryFile(File f, long numberOfElements, String data) throws IOException {
        final String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<container>\n";
        final String tail = "</container>\n";
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath(), Charset.forName("utf-8"))) {
            bw.write(head);
            for (long i = 0; i < numberOfElements; i++) {
                bw.write("  <record>" + data + i + "</record>\n");
            }
            bw.write(tail);
        }
    }

    private void initializeFlowStore(String id, long submitterNumber, List<JavaScript> javaScripts) throws FlowStoreServiceConnectorException {
        // insert submitter:
        final SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setNumber(submitterNumber)
                .setName(id)
                .build();
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);

        // insert flowcomponent with javascripts:
        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(id)
                .setInvocationJavascriptName("invocationJavaScript")
                .setJavascripts(javaScripts)
                .setInvocationMethod("invocationFunction")
                .build();
        final FlowComponent flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);

        // insert flow:
        final FlowContent flowContent = new FlowContentBuilder()
                .setName(id)
                .setComponents(Collections.singletonList(flowComponent))
                .build();
        final Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // insert sink:
        final SinkContent sinkContent = new SinkContentBuilder()
                .setName(id)
                .setResource(SINK_RESOURCE)
                .build();
        final Sink sink = flowStoreServiceConnector.createSink(sinkContent);

        // insert flowbinder
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(id)
                .setPackaging(PACKAGING)
                .setFormat(FORMAT)
                .setCharset(ENCODING)
                .setDestination(DESTINATION)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
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
                + "return record;"
                + "}").getBytes("UTF-8")), "NoModule");
        JavaScript jsUse = new JavaScript(Base64.encodeBase64String("function use(module) {};".getBytes("UTF-8")), "Use");
        JavaScript jsModulesInfo = new JavaScript(Base64.encodeBase64String("var __ModulesInfo = function() { var that = {}; that.checkDepAlreadyLoaded = function( moduleName ) { return true; }; return that;}();".getBytes("UTF-8")), "ModulesInfo");
        return Arrays.asList(js, jsUse, jsModulesInfo);
    }

    /*
     * Creates realistic sized javascripts for use in the performance-test.
     */
    private List<JavaScript> getJavaScriptsForLargePerformanceTest() throws IOException {
        final List<JavaScript> javascripts = new ArrayList<>(Arrays.asList(
                new JavaScript(Base64.encodeBase64String((
                          "use(\"MarcClasses\")\n\n"
                        + "function invocationFunction(record, supplementaryData) {\n"
                        + "  var instance = Packages.java.security.MessageDigest.getInstance(\"md5\");\n"
                        + "  instance.update((new Packages.java.lang.String(record)).getBytes(\"UTF-8\"));\n"
                        + "  var md5 = instance.digest();\n"
                        + "  var res = String();\n"
                        + "  for(var i=0; i<md5.length; i++) {\n"
                        + "    res += (md5[i] & 0xFF).toString(16);\n"
                        + "  }\n"
                        + "  return res;\n"
                        + "}").getBytes("UTF-8")), ""),
                new JavaScript(ResourceReader.getResourceAsBase64(PerformanceIT.class, "javascript/jscommon/system/Use.use.js"), "Use"),
                new JavaScript(ResourceReader.getResourceAsBase64(PerformanceIT.class, "javascript/jscommon/system/ModulesInfo.use.js"), "ModulesInfo"),
                new JavaScript(ResourceReader.getResourceAsBase64(PerformanceIT.class, "javascript/jscommon/system/Use.RequiredModules.use.js"), "Use.RequiredModules"),
                new JavaScript(ResourceReader.getResourceAsBase64(PerformanceIT.class, "javascript/jscommon/external/ES5.use.js"), "ES5"),
                new JavaScript(ResourceReader.getResourceAsBase64(PerformanceIT.class, "javascript/jscommon/system/Engine.use.js"), "Engine")));

        // The following resources are located in the test/resources folder in the project.
        // All files are copied from jscommon and the filename is prefixed with "TestResource_".
        // This is in order to ensure that they are not confused with the actual javascripts.
        // The purpose of these scripts are just to add bulk/volume to the chunk, and to ensure that a believable
        // amount of javascript is read into the javascripts-environment during the test.
        for (String jsDependency : Arrays.asList(
                "Log",
                "LogCore",
                "Global",
                "MarcClasses",
                "MarcClassesCore",
                "System",
                "Underscore",
                "UnitTest",
                "Util")) {
            javascripts.add(new JavaScript(ResourceReader.getResourceAsBase64(PerformanceIT.class, "TestResource_" + jsDependency + ".use.js"), jsDependency));
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
}
