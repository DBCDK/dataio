package dk.dbc.dataio.jse.artemis.common.jms;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult.Status;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.ItemDeliveryException;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SinkMessageConsumerAdapterTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "870970:12345678";
    private static final String TRACKING_ID = "42/7";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final TestSink sink = new TestSink(jobStoreServiceConnector);

    @Test
    void noWatermark_itemIsDelivered() throws Exception {
        givenWatermark(null);

        sink.handleConsumedMessage(itemMessage());

        assertThat("item delivered", sink.delivered.getId(), is((long) ITEM_ID));
        assertThat("reported result", reportedResult(),
                is(new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.DELIVERED, sink.outcome)));
    }

    @Test
    void incomingIsNewerThanWatermark_itemIsDelivered() throws Exception {
        givenWatermark(new Watermark(JOB_ID, CHUNK_ID, (short) (ITEM_ID - 1)));

        sink.handleConsumedMessage(itemMessage());

        assertThat("item delivered", sink.delivered, is(notNullValue()));
        assertThat("reported status", reportedResult().status(), is(Status.DELIVERED));
    }

    @Test
    void incomingEqualsWatermark_itemIsDeliveredAsExactRetransmit() throws Exception {
        givenWatermark(new Watermark(JOB_ID, CHUNK_ID, ITEM_ID));

        sink.handleConsumedMessage(itemMessage());

        assertThat("item delivered", sink.delivered, is(notNullValue()));
        assertThat("reported status", reportedResult().status(), is(Status.DELIVERED));
    }

    @Test
    void incomingIsOlderThanWatermark_itemIsSkipped() throws Exception {
        givenWatermark(new Watermark(JOB_ID + 1, 0, (short) 0));

        sink.handleConsumedMessage(itemMessage());

        assertThat("item not delivered", sink.delivered, is(nullValue()));

        ItemDeliveryResult result = reportedResult();
        assertThat("reported status", result.status(), is(Status.SKIPPED));
        assertThat("the skipped item still names its watermark row", result.recordKey(), is(RECORD_KEY));
        assertThat("outcome item id", result.chunkItem().getId(), is((long) ITEM_ID));
        assertThat("outcome item status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome item tracking id", result.chunkItem().getTrackingId(), is(TRACKING_ID));
    }

    @Test
    void noRecordKeyHeader_watermarkIsNotConsulted() throws Exception {
        Map<String, Object> headers = itemHeaders();
        headers.remove(JMSHeader.recordKey.name);

        sink.handleConsumedMessage(message(headers));

        verify(jobStoreServiceConnector, never()).getWatermark(anyInt(), anyString());
        assertThat("item delivered", sink.delivered, is(notNullValue()));
        assertThat("reported result", reportedResult(),
                is(new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED, sink.outcome)));
    }

    @Test
    void watermarkOptOut_everyItemIsDeliveredAndReportedWithoutWatermark() throws Exception {
        TestSink optedOut = new TestSink(jobStoreServiceConnector) {
            @Override
            protected boolean usesDeliveryWatermark() {
                return false;
            }
        };

        optedOut.handleConsumedMessage(itemMessage());

        verify(jobStoreServiceConnector, never()).getWatermark(anyInt(), anyString());
        assertThat("item delivered", optedOut.delivered, is(notNullValue()));
        assertThat("reported result", reportedResult(),
                is(new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED, optedOut.outcome)));
    }

    @Test
    void watermarkLookupFails_messageIsRolledBackAndNothingIsDelivered() throws Exception {
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString()))
                .thenThrow(new JobStoreServiceConnectorException("down"));

        assertThrows(ItemDeliveryException.class, () -> sink.handleConsumedMessage(itemMessage()));

        assertThat("item not delivered", sink.delivered, is(nullValue()));
        verify(jobStoreServiceConnector, never()).addItemDelivered(any(), anyInt(), anyInt(), anyShort());
    }

    @Test
    void deliveryFails_messageIsRolledBackAndNoResultIsReported() throws Exception {
        givenWatermark(null);
        sink.failure = new IllegalStateException("target is down");

        assertThrows(IllegalStateException.class, () -> sink.handleConsumedMessage(itemMessage()));

        verify(jobStoreServiceConnector, never()).addItemDelivered(any(), anyInt(), anyInt(), anyShort());
    }

    @Test
    void resultReportingFails_messageIsRolledBack() throws Exception {
        givenWatermark(null);
        doThrow(new JobStoreServiceConnectorException("down"))
                .when(jobStoreServiceConnector).addItemDelivered(any(), anyInt(), anyInt(), anyShort());

        assertThrows(ItemDeliveryException.class, () -> sink.handleConsumedMessage(itemMessage()));
    }

    @Test
    void sinkReportsFailure_resultIsReportedWithRecordKey() throws Exception {
        givenWatermark(null);
        sink.status = Status.FAILED;

        sink.handleConsumedMessage(itemMessage());

        assertThat("reported result", reportedResult(),
                is(new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.FAILED, sink.outcome)));
    }

    @Test
    void chunkPayloadType_messageIsInvalid() {
        Map<String, Object> headers = itemHeaders();
        headers.put(JMSHeader.payload.name, JMSHeader.CHUNK_PAYLOAD_TYPE);

        InvalidMessageException e = assertThrows(InvalidMessageException.class,
                () -> sink.handleConsumedMessage(message(headers)));

        assertThat("names both payload types", e.getMessage(), is("Message<id> payload type Chunk != Item"));
        assertThat("item not delivered", sink.delivered, is(nullValue()));
    }

    @Test
    void missingItemIdHeader_messageIsInvalid() {
        Map<String, Object> headers = itemHeaders();
        headers.remove(JMSHeader.itemId.name);

        assertThrows(InvalidMessageException.class, () -> sink.handleConsumedMessage(message(headers)));
    }

    @Test
    void missingSinkIdHeader_messageIsInvalid() {
        Map<String, Object> headers = itemHeaders();
        headers.remove(JMSHeader.sinkId.name);

        assertThrows(InvalidMessageException.class, () -> sink.handleConsumedMessage(message(headers)));
    }

    @Test
    void unparseableBody_messageIsInvalid() {
        assertThrows(InvalidMessageException.class,
                () -> sink.handleConsumedMessage(new ConsumedMessage("id", itemHeaders(), "not an item")));
    }

    @Test
    void resultIsReportedAfterDelivery() throws Exception {
        givenWatermark(null);

        sink.handleConsumedMessage(itemMessage());

        InOrder inOrder = inOrder(jobStoreServiceConnector);
        inOrder.verify(jobStoreServiceConnector).getWatermark(anyInt(), anyString());
        inOrder.verify(jobStoreServiceConnector).addItemDelivered(any(), anyInt(), anyInt(), anyShort());
        assertThat("delivery happened before the report", sink.deliveredBeforeReport, is(true));
    }

    private void givenWatermark(Watermark watermark) throws JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.ofNullable(watermark));
    }

    private ItemDeliveryResult reportedResult() throws JobStoreServiceConnectorException {
        ArgumentCaptor<ItemDeliveryResult> captor = ArgumentCaptor.forClass(ItemDeliveryResult.class);
        verify(jobStoreServiceConnector).addItemDelivered(captor.capture(), eq(JOB_ID), eq(CHUNK_ID), eq(ITEM_ID));
        return captor.getValue();
    }

    private ConsumedMessage itemMessage() {
        return message(itemHeaders());
    }

    private ConsumedMessage message(Map<String, Object> headers) {
        try {
            return new ConsumedMessage("id", headers, jsonbContext.marshall(new ChunkItem()
                    .withId(ITEM_ID)
                    .withStatus(ChunkItem.Status.SUCCESS)
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(TRACKING_ID)
                    .withData("processing outcome")));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> itemHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        return headers;
    }

    private static class TestSink extends SinkMessageConsumerAdapter {
        private final ChunkItem outcome = new ChunkItem()
                .withId(ITEM_ID)
                .withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(TRACKING_ID)
                .withData("delivered");
        private ChunkItem delivered;
        private Status status = Status.DELIVERED;
        private RuntimeException failure;
        private boolean deliveredBeforeReport;

        TestSink(JobStoreServiceConnector jobStoreServiceConnector) {
            super(new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test());
        }

        @Override
        protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
            if (failure != null) {
                throw failure;
            }
            delivered = item;
            deliveredBeforeReport = true;
            return ItemDeliveryResult.of(status, outcome);
        }

        @Override
        public String getQueue() {
            return "queue";
        }

        @Override
        public String getAddress() {
            return "address";
        }
    }
}
