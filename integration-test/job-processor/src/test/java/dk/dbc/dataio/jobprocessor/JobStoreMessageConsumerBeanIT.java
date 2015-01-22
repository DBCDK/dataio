package dk.dbc.dataio.jobprocessor;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobstore.ejb.JobProcessorMessageProducerBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class JobStoreMessageConsumerBeanIT {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;

    private final String sinkResourceName = new SinkBuilder().build().getContent().getResource();

    private JMSContext jmsContext;

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
    public void jobStoreMessageConsumerBean_invalidChunkOnProcessorQueue_eventuallyRemovedFromProcessorQueue() throws JMSException, JsonException {
        final ChunkItem item = new ChunkItemBuilder().setId(0L).build();
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).setItems(Arrays.asList(item)).build();
        final MockedJmsTextMessage jobStoreMessage = newJobStoreMessageForJobProcessor(chunk);
        
        jobStoreMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
    }

    @Test
    public void jobStoreMessageConsumerBean_validChunkOnProcessorQueue_eventuallyRemovedForProcessingWithResultsOnProcessorAndSinkQueues() throws Exception {
        final long jobId = 42;
        final String itemData = "data";

        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setId(0L)
                .setData(Base64Util.base64encode(itemData))
                .build();

        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(Arrays.asList(chunkItem))
                .build();

        final MockedJmsTextMessage jobStoreMessage = newJobStoreMessageForJobProcessor(chunk);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        final List<MockedJmsTextMessage> sinksQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.SINKS_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        ExternalChunk processedChunk = assertProcessorMessageForSink(sinksQueue.get(0));
        assertThat(processedChunk.getType(), is(ExternalChunk.Type.PROCESSED));
        assertThat(processedChunk.getJobId(), is(jobId));
        assertThat(processedChunk.getChunkId(), is(chunk.getChunkId()));
        assertThat(processedChunk.size(), is(1));
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(Base64Util.base64decode(iterator.next().getData()), is(itemData.toUpperCase()));

        final List<MockedJmsTextMessage> processorQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        ChunkResult processorResult = assertProcessorMessageForJobStore(processorQueue.get(0));
        assertThat(processorResult.getJobId(), is(jobId));
        assertThat(processorResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(processorResult.getItems().size(), is(1));
        assertThat(Base64Util.base64decode(processorResult.getItems().get(0).getData()), is(itemData.toUpperCase()));
    }

    private ChunkResult assertProcessorMessageForJobStore(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        assertThat(message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE));
        return JsonUtil.fromJson(message.getText(), ChunkResult.class, MixIns.getMixIns());
    }

    private ExternalChunk assertProcessorMessageForSink(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        assertThat(message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE));
        assertThat(message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(sinkResourceName));
        return JsonUtil.fromJson(message.getText(), ExternalChunk.class, MixIns.getMixIns());
    }

    private MockedJmsTextMessage newJobStoreMessageForJobProcessor(ExternalChunk chunk) throws JMSException, JsonException {
        final MockedJmsTextMessage message = (MockedJmsTextMessage) new JobProcessorMessageProducerBean()
                .createMessage(jmsContext, chunk);
        message.setText(JsonUtil.toJson(chunk));
        return message;
    }

    private Flow buildToUpperFlow() throws Exception {
        return new FlowBuilder()
                .setContent(buildToUpperFlowContent())
                .build();
    }

    private FlowContent buildToUpperFlowContent() throws Exception {
        return new FlowContentBuilder()
                .setComponents(Arrays.asList(buildToUpperFlowComponent()))
                .build();
    }

    private FlowComponent buildToUpperFlowComponent() throws Exception {
        return new FlowComponentBuilder()
                .setContent(buildToUpperFlowComponentContent())
                .build();
    }

    private FlowComponentContent buildToUpperFlowComponentContent() throws Exception {
        return new FlowComponentContentBuilder()
                .setInvocationMethod("toUpper")
                .setJavascripts(Arrays.asList(
                        buildJavaScript(buildJavaScriptToUpperFunction(), ""),
                        buildJavaScript(resourceToString("/ModulesInfo.js"), "ModulesInfo"),
                        buildJavaScript(resourceToString("/Use.js"), "Use")))
                .build();
    }

    private JavaScript buildJavaScript(String javaScript, String moduleName) {
        return new JavaScriptBuilder()
                .setModuleName(moduleName)
                .setJavascript(Base64Util.base64encode(javaScript))
                .build();
    }

    private String buildJavaScriptToUpperFunction() {
        return ""
            + "function toUpper(str) {\n"
            + "    return str.toUpperCase();\n"
            + "}\n";
    }

    private String resourceToString(String resourceName) throws Exception {
        final java.net.URL url = this.getClass().getResource(resourceName);
        final java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
        return new String(java.nio.file.Files.readAllBytes(resPath), StandardCharsets.UTF_8);
    }
}
