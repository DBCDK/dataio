package dk.dbc.dataio.dlq.errorhandler;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jse.artemis.common.Metric;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.destination;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.redelivery;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.rejected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class DmqMessageConsumerBeanTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String TRACKING_ID = "42/7";
    private static final String RECORD_KEY = "870970:12345678";

    private Map<String, Object> headers;
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final DLQMessageConsumer dlqMessageConsumer = new DLQMessageConsumer(new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build());

    public DmqMessageConsumerBeanTest() {

    }

    @BeforeEach
    public void setup() {
        headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        PrometheusMetricRegistry.create().resetAll();
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalid_noTransactionRollback() throws JMSException {
        MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        JMSHeader.payload.addHeader(textMessage, JMSHeader.CHUNK_PAYLOAD_TYPE);
        JMSHeader.jobId.addHeader(textMessage, 0);
        textMessage.setText("{'invalid': 'instance'}");
        dlqMessageConsumer.onMessage(textMessage);
        long rej = Metric.dataio_message_count.counter(destination.is(dlqMessageConsumer.getFQN()), redelivery.is("false"), rejected.is("true")).getCount();
        assertEquals(1, rej, "Message should be rejected");
    }

    @Test
    public void handleConsumedMessage_messageArgPayloadIsInvalid_throws() throws JobStoreException, InvalidMessageException {
        ConsumedMessage consumedMessage = new ConsumedMessage("id", headers, "{'invalid': 'instance'}");
        assertThrows(InvalidMessageException.class, () -> dlqMessageConsumer.handleConsumedMessage(consumedMessage));
    }

    @Test
    public void handleConsumedMessage_messageArgPayloadIsUnknown_throws() throws JobStoreException, InvalidMessageException {
        ConsumedMessage consumedMessage = new ConsumedMessage("id", Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, "Unknown"), "{'unknown': 'instance'}");
        assertThrows(InvalidMessageException.class, () -> dlqMessageConsumer.handleConsumedMessage(consumedMessage));
    }

    @Test
    public void onMessage_deadPartitionedChunk_singleChunkAdded() throws JMSException, JSONBException, JobStoreServiceConnectorException {
        Chunk originalChunk = new ChunkBuilder(Chunk.Type.PARTITIONED).build();
        MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        JMSHeader.payload.addHeader(textMessage, JMSHeader.CHUNK_PAYLOAD_TYPE);
        JMSHeader.jobId.addHeader(textMessage, 0);
        textMessage.setText(dlqMessageConsumer.jsonbContext.marshall(originalChunk));
        dlqMessageConsumer.onMessage(textMessage);
        long rec = Metric.dataio_message_count.counter(destination.is(dlqMessageConsumer.getFQN()), redelivery.is("false")).getCount();
        long rej = Metric.dataio_message_count.counter(destination.is(dlqMessageConsumer.getFQN()), redelivery.is("false"), rejected.is("true")).getCount();
        Mockito.verify(jobStoreServiceConnector, times(1)).addChunk(any(Chunk.class), eq(originalChunk.getJobId()), eq(originalChunk.getChunkId()));
        Mockito.verify(jobStoreServiceConnector, never()).addItemDelivered(any(), anyInt(), anyInt(), anyShort());
        assertEquals(1, rec, "Message should be successfully consumed");
        assertEquals(0, rej, "Message should be accepted");
    }

    /**
     * Every queue dead letters here, so after per item dispatch the sink queues send
     * single items rather than whole chunks. Failing them individually is what lets their
     * job complete, see docs/chunk-scheduling-redesign.md.
     */
    @Test
    public void handleConsumedMessage_deadItem_itemIsFailed() throws Exception {
        ChunkItem item = new ChunkItem().withId(ITEM_ID).withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.STRING).withTrackingId(TRACKING_ID).withData("processing outcome");

        dlqMessageConsumer.handleConsumedMessage(itemMessage(item));

        ArgumentCaptor<ItemDeliveryResult> captor = ArgumentCaptor.forClass(ItemDeliveryResult.class);
        Mockito.verify(jobStoreServiceConnector).addItemDelivered(captor.capture(), eq(JOB_ID), eq(CHUNK_ID), eq(ITEM_ID));
        Mockito.verify(jobStoreServiceConnector, never()).addChunk(any(Chunk.class), anyInt(), anyInt());

        ItemDeliveryResult result = captor.getValue();
        assertEquals(SINK_ID, result.sinkId(), "sink id is carried from the message");
        assertEquals(RECORD_KEY, result.recordKey(), "the dead item names its watermark row");
        assertEquals(ItemDeliveryResult.Status.FAILED, result.status(), "item is failed");
        assertEquals(ITEM_ID, result.chunkItem().getId(), "outcome item id");
        assertEquals(ChunkItem.Status.FAILURE, result.chunkItem().getStatus(), "outcome item status");
        assertEquals(TRACKING_ID, result.chunkItem().getTrackingId(), "tracking id is preserved");
        assertEquals(ChunkItem.Type.STRING, result.chunkItem().getType().getFirst(), "outcome item type");
    }

    @Test
    public void handleConsumedMessage_deadTerminationItem_outcomeKeepsJobEndType() throws Exception {
        ChunkItem item = new ChunkItem().withId(ITEM_ID).withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.JOB_END).withTrackingId(TRACKING_ID).withData("end");

        dlqMessageConsumer.handleConsumedMessage(itemMessage(item));

        ArgumentCaptor<ItemDeliveryResult> captor = ArgumentCaptor.forClass(ItemDeliveryResult.class);
        Mockito.verify(jobStoreServiceConnector).addItemDelivered(captor.capture(), eq(JOB_ID), eq(CHUNK_ID), eq(ITEM_ID));
        assertEquals(ChunkItem.Type.JOB_END, captor.getValue().chunkItem().getType().getFirst(),
                "termination item stays recognizable to job-store");
    }

    @Test
    public void handleConsumedMessage_deadItemPayloadIsInvalid_throws() {
        Map<String, Object> itemHeaders = itemHeaders();
        ConsumedMessage consumedMessage = new ConsumedMessage("id", itemHeaders, "{'invalid': 'instance'}");
        assertThrows(InvalidMessageException.class, () -> dlqMessageConsumer.handleConsumedMessage(consumedMessage));
    }

    @Test
    public void handleConsumedMessage_deadItemCanNotBeReported_throwsNamingTheItem() throws Exception {
        doThrow(new JobStoreServiceConnectorException("down"))
                .when(jobStoreServiceConnector).addItemDelivered(any(), anyInt(), anyInt(), anyShort());
        ChunkItem item = new ChunkItem().withId(ITEM_ID).withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.STRING).withData("processing outcome");

        InvalidMessageException e = assertThrows(InvalidMessageException.class,
                () -> dlqMessageConsumer.handleConsumedMessage(itemMessage(item)));

        assertTrue(e.getMessage().contains(JOB_ID + "/" + CHUNK_ID + "/" + ITEM_ID),
                "message names the item that could not be reported: " + e.getMessage());
    }

    /**
     * An item with no record identity, the job termination item being the one that occurs,
     * carries no recordKey header and so names no watermark row
     */
    @Test
    public void handleConsumedMessage_deadItemWithoutRecordKey_reportsNullWatermarkKey() throws Exception {
        Map<String, Object> itemHeaders = itemHeaders();
        itemHeaders.remove(JMSHeader.recordKey.name);
        ChunkItem item = new ChunkItem().withId(ITEM_ID).withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.JOB_END).withData("end");

        dlqMessageConsumer.handleConsumedMessage(
                new ConsumedMessage("id", itemHeaders, dlqMessageConsumer.jsonbContext.marshall(item)));

        ArgumentCaptor<ItemDeliveryResult> captor = ArgumentCaptor.forClass(ItemDeliveryResult.class);
        Mockito.verify(jobStoreServiceConnector).addItemDelivered(captor.capture(), eq(JOB_ID), eq(CHUNK_ID), eq(ITEM_ID));
        assertNull(captor.getValue().recordKey(), "no record identity, no watermark row");
    }

    private ConsumedMessage itemMessage(ChunkItem item) throws JSONBException {
        return new ConsumedMessage("id", itemHeaders(), dlqMessageConsumer.jsonbContext.marshall(item));
    }

    private Map<String, Object> itemHeaders() {
        Map<String, Object> itemHeaders = new HashMap<>();
        itemHeaders.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        itemHeaders.put(JMSHeader.jobId.name, JOB_ID);
        itemHeaders.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        itemHeaders.put(JMSHeader.itemId.name, ITEM_ID);
        itemHeaders.put(JMSHeader.sinkId.name, SINK_ID);
        itemHeaders.put(JMSHeader.recordKey.name, RECORD_KEY);
        return itemHeaders;
    }
}
