package dk.dbc.dataio.jobprocessor;

import dk.dbc.dataio.commons.types.ChunkResult;
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
import dk.dbc.dataio.commons.utils.test.json.NewJobJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkJsonBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.SinkMessageProducerBean;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

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

public class JobStoreMessageConsumerBeanIT {
    private final String javaScriptInvocationMethod = "toUpper";
    private final String modulesInfoModuleResource = "/ModulesInfo.json";
    private final String useModuleResource = "/Use.json";
    private final String sinkResourceName = "sinkResourceName";

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.SINKS_QUEUE_NAME);
    }

    @Test
    public void jobStoreMessageConsumerBean_invalidNewJobOnProcessorQueue_eventuallyRemovedFromProcessorQueue() throws JMSException, InterruptedException {
        final MockedJmsTextMessage jobStoreMessage = JmsQueueConnector.newJobStoreMessageForJobProcessor();
        jobStoreMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        Thread.sleep(500);
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME), is(0));
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME), is(0));
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

        final String newJob = new NewJobJsonBuilder()
                .setJobId(jobId)
                .setChunkCount(2)
                .setSink(getSink())
                .build();
        final MockedJmsTextMessage jobStoreMessage = JmsQueueConnector.newJobStoreMessageForJobProcessor();
        jobStoreMessage.setText(newJob);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        Thread.sleep(2000);

        final List<MockedJmsTextMessage> sinksQueue = JmsQueueConnector.listQueue(JmsQueueConnector.SINKS_QUEUE_NAME);
        assertThat(sinksQueue.size(), is(2));

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
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME), is(2));
    }

    public ChunkResult assertProcessorMessageForSink(MockedJmsTextMessage message) throws JMSException, JsonException {
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

    private String getSink() throws Exception {
        return new SinkJsonBuilder()
                .setContent(getSinkContent())
                .build();
    }

    private String getSinkContent() throws Exception {
        return new SinkContentJsonBuilder()
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
