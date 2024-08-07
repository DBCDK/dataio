package dk.dbc.dataio.jobprocessor2;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobprocessor2.jms.JobStoreMessageConsumer;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jse.artemis.common.JobProcessorException;
import dk.dbc.dataio.jse.artemis.common.Metric;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static dk.dbc.dataio.jobprocessor2.Metric.ATag.rollback;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class JobStoreMessageConsumerBeanTest {
    private static final ServiceHub SERVICE_HUB = new ServiceHub.Builder().withJobStoreServiceConnector(mock(JobStoreServiceConnector.class)).build();
    private final Map<String, Object> headers = Map.of(
            JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE,
            JmsConstants.FLOW_ID_PROPERTY_NAME, 42L,
            JmsConstants.FLOW_VERSION_PROPERTY_NAME, 1L,
            JmsConstants.ADDITIONAL_ARGS, "{}"
    );

    @BeforeEach
    public void init() {
        ((PrometheusMetricRegistry)SERVICE_HUB.metricRegistry).resetAll();
        ChunkProcessor.clearFlowCache();
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidNewJob_noTransactionRollback() throws JMSException {
        JobStoreMessageConsumer jobStoreMessageConsumer = new JobStoreMessageConsumer(SERVICE_HUB);
        MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        JMSHeader.payload.addHeader(textMessage, JMSHeader.CHUNK_PAYLOAD_TYPE);
        JMSHeader.jobId.addHeader(textMessage, 0);
        textMessage.setText("{'invalid': 'instance'}");
        jobStoreMessageConsumer.onMessage(textMessage);
        Assertions.assertEquals(0, Metric.dataio_message_count.counter(rollback.is("true")).getCount());
    }

    @Test
    public void handleConsumedMessage_messageArgPayloadIsInvalidNewJob_throws() throws JobProcessorException {
        ConsumedMessage consumedMessage = new ConsumedMessage("id", headers, "{'invalid': 'instance'}");
        JobStoreMessageConsumer jobStoreMessageConsumer = new JobStoreMessageConsumer(SERVICE_HUB);
        assertThrows(InvalidMessageException.class, () -> jobStoreMessageConsumer.handleConsumedMessage(consumedMessage));
    }

    @Test
    public void handleConsumedMessage_messagePayloadCanNotBeUnmarshalledToJson_throws() throws JobProcessorException {
        ConsumedMessage message = new ConsumedMessage("id", headers, "invalid");
        JobStoreMessageConsumer jobStoreMessageConsumer = new JobStoreMessageConsumer(SERVICE_HUB);
        assertThrows(InvalidMessageException.class, () -> jobStoreMessageConsumer.handleConsumedMessage(message));
    }

    @Test
    public void handleConsumedMessage_messageChunkIsOfIncorrectType_throws() throws JobProcessorException, JSONBException {
        ChunkItem item = new ChunkItemBuilder().setData(StringUtil.asBytes("This is some data")).setStatus(ChunkItem.Status.SUCCESS).build();
        // The Chunk-type 'processed' is not allowed in the JobProcessor, only 'partitioned' is allowed.
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(Collections.singletonList(item)).build();
        String jsonChunk = new JSONBContext().marshall(chunk);

        JobStoreMessageConsumer jobStoreMessageConsumer = new JobStoreMessageConsumer(SERVICE_HUB);
        ConsumedMessage message = new ConsumedMessage("id", headers, jsonChunk);
        assertThrows(InvalidMessageException.class, () -> jobStoreMessageConsumer.handleConsumedMessage(message));
    }

//    @Test
//    public void handleConsumedMessage() throws Exception {
//        DeprecatedChunkProcessorTest jsFactory = new DeprecatedChunkProcessorTest();
//        Flow flow = jsFactory.getFlow(new DeprecatedChunkProcessorTest.ScriptWrapper(DeprecatedChunkProcessorTest.javaScriptReturnUpperCase, DeprecatedChunkProcessorTest.getJavaScript(DeprecatedChunkProcessorTest.getJavaScriptReturnUpperCaseFunction())));
//
//        when(SERVICE_HUB.jobStoreServiceConnector.getCachedFlow(((Long)headers.get(JmsConstants.FLOW_ID_PROPERTY_NAME)).intValue())).thenReturn(flow);
//
//        ChunkItem item = new ChunkItemBuilder().setData(StringUtil.asBytes("data")).setStatus(ChunkItem.Status.SUCCESS).build();
//        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(((Long)headers.get(JmsConstants.FLOW_ID_PROPERTY_NAME)).intValue()).setItems(Collections.singletonList(item)).build();
//        String jsonChunk = new JSONBContext().marshall(chunk);
//
//        JobStoreMessageConsumer jobStoreMessageConsumer = new JobStoreMessageConsumer(SERVICE_HUB);
//        ConsumedMessage message = new ConsumedMessage("id", headers, jsonChunk);
//        jobStoreMessageConsumer.handleConsumedMessage(message);  // Flow is fetched from job-store
//        jobStoreMessageConsumer.handleConsumedMessage(message);  // cached flow is used
//        verify(SERVICE_HUB.jobStoreServiceConnector, times(1)).getCachedFlow((int) chunk.getJobId());
//        verify(SERVICE_HUB.jobStoreServiceConnector, times(2)).addChunkIgnoreDuplicates(any(Chunk.class), anyInt(), anyLong());
//    }
}


