package dk.dbc.dataio.jobprocessor;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.ChunkJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JavaScriptJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.NewJobBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.SinkMessageProducerBean;
import dk.dbc.dataio.jobstore.ejb.JobProcessorMessageProducerBean;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.jobprocessor.util.Base64Util.base64decode;
import static dk.dbc.dataio.jobprocessor.util.Base64Util.base64encode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class JobStoreMessageConsumerBeanIT {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;

    private final String javaScriptInvocationMethod = "toUpper";
    private final String modulesInfoModuleResource = "/ModulesInfo.json";
    private final String useModuleResource = "/Use.json";
    private final String sinkResourceName = "sinkResourceName";

    private JMSContext jmsContext;

    @AfterClass
    public static void clearJobStore() {
        ITUtil.clearJobStore();
    }

    @Before
    public void setupMocks() {
        jmsContext = mock(JMSContext.class);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.SINKS_QUEUE_NAME);
    }

    @Test
    public void jobStoreMessageConsumerBean_invalidNewJobOnProcessorQueue_eventuallyRemovedFromProcessorQueue() throws JMSException, JsonException {
        final MockedJmsTextMessage jobStoreMessage = newJobStoreMessageForJobProcessor(new NewJobBuilder().build());
        jobStoreMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
    }

    @Test
    public void jobStoreMessageConsumerBean_validNewJobOnProcessorQueue_eventuallyRemovedForProcessingWithResultsOnProcessorAndSinkQueues() throws Exception {
        final long jobId = 42;

        final String record1 = "one";
        final String record2 = "two";
        final String flow = getFlow();
        final String chunk1 = getChunk(1, flow, base64encode(record1));
        final String chunk2 = getChunk(2, flow, base64encode(record2));
        setupJobStore(jobId, chunk1, chunk2);

        final NewJob newJob = new NewJobBuilder()
                .setJobId(jobId)
                .setChunkCount(2)
                .setSink(getSink())
                .build();

        final MockedJmsTextMessage jobStoreMessage = newJobStoreMessageForJobProcessor(newJob);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        final List<MockedJmsTextMessage> sinksQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.SINKS_QUEUE_NAME, 2, MAX_QUEUE_WAIT_IN_MS);

        ChunkResult processorResult;

        processorResult = assertProcessorMessageForSink(sinksQueue.get(0));
        assertThat(processorResult.getJobId(), is(jobId));
        assertThat(processorResult.getChunkId(), is(1L));
        assertThat(processorResult.getResults().size(), is(1));
        assertThat(base64decode(processorResult.getResults().get(0)), is(record1.toUpperCase()));

        processorResult = assertProcessorMessageForSink(sinksQueue.get(1));
        assertThat(processorResult.getJobId(), is(jobId));
        assertThat(processorResult.getChunkId(), is(2L));
        assertThat(processorResult.getResults().size(), is(1));
        assertThat(base64decode(processorResult.getResults().get(0)), is(record2.toUpperCase()));

        // When the job-store starts reacting to these messages,
        // this assertion won't work any longer.
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 2, MAX_QUEUE_WAIT_IN_MS);
    }

    private ChunkResult assertProcessorMessageForSink(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        assertThat(message.getStringProperty(SinkMessageProducerBean.SOURCE_PROPERTY_NAME), is(SinkMessageProducerBean.SOURCE_PROPERTY_VALUE));
        assertThat(message.getStringProperty(SinkMessageProducerBean.PAYLOAD_PROPERTY_NAME), is(SinkMessageProducerBean.PAYLOAD_PROPERTY_VALUE));
        assertThat(message.getStringProperty(SinkMessageProducerBean.RESOURCE_PROPERTY_NAME), is(sinkResourceName));
        return JsonUtil.fromJson(message.getText(), ChunkResult.class, MixIns.getMixIns());
    }

    private Path setupJobStore(long jobId, String... chunks) throws IOException {
        // This method is tightly bound to job-store internal workings...
        final Path jobPath = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), "dataio-job-store", Long.toString(jobId));
        FileUtils.deleteQuietly(jobPath.toFile());
        Files.createDirectories(jobPath);
        for (int i = 0; i < chunks.length; i++) {
            FileUtils.writeStringToFile(Paths.get(jobPath.toString(), String.format("%d.json", i+1)).toFile(), chunks[i], "UTF-8");
        }
        return jobPath;
    }

    private MockedJmsTextMessage newJobStoreMessageForJobProcessor(NewJob newJob) throws JMSException, JsonException {
        final MockedJmsTextMessage message = (MockedJmsTextMessage) new JobProcessorMessageProducerBean()
                .createMessage(jmsContext, newJob);
        message.setText(JsonUtil.toJson(newJob));
        return message;
    }

    private Sink getSink() throws Exception {
        return new SinkBuilder()
                .setContent(getSinkContent())
                .build();
    }

    private SinkContent getSinkContent() throws Exception {
        return new SinkContentBuilder()
                .setResource(sinkResourceName)
                .build();
    }

    private String getFlow() throws Exception {
        return new FlowJsonBuilder()
                .setContent(getFlowContent())
                .build();
    }

    private String getFlowContent() throws Exception {
        return new FlowContentJsonBuilder()
                .setComponents(Arrays.asList(getFlowComponent()))
                .build();
    }

    private String getFlowComponent() throws Exception {
        return new FlowComponentJsonBuilder()
                .setContent(getFlowComponentContent())
                .build();
    }

    private String getFlowComponentContent() throws Exception {
        return new FlowComponentContentJsonBuilder()
                .setInvocationMethod(javaScriptInvocationMethod)
                .setJavascripts(Arrays.asList(
                        getJavaScript(),
                        resourceToString(modulesInfoModuleResource),
                        resourceToString(useModuleResource)))
                .build();
    }

    private String getJavaScript() {
        return new JavaScriptJsonBuilder()
                .setJavascript(base64encode(getJavaScriptToUpperFunction()))
                .build();
    }

    private String getJavaScriptToUpperFunction() {
        return ""
            + "function " + javaScriptInvocationMethod + "(str) {\n"
            + "    return str.toUpperCase();\n"
            + "}\n";
    }

    private String getChunk(long id, String flow, String... records) {
        return new ChunkJsonBuilder()
                .setId(id)
                .setFlow(flow)
                .setRecords(Arrays.asList(records))
                .build();
    }

    private String resourceToString(String resourceName) throws Exception {
        final java.net.URL url = JobStoreMessageConsumerBeanIT.class.getResource(resourceName);
        final java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
        return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
    }
}
