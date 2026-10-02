package dk.dbc.dataio.sink.dummy;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DummyMessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "870970:12345678";
    private static final String TRACKING_ID = "rr:1223io:12534";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final DummyMessageConsumer consumer = new DummyMessageConsumer(
            new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test());

    @Test
    void successItem_isDelivered() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.SUCCESS));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    /**
     * IGNORED rather than DELIVERED is what keeps an item this sink did not send counted
     * as ignored in the delivering phase, as it was when whole chunks were delivered.
     */
    @Test
    void failureItem_isIgnored() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.FAILURE));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
    }

    @Test
    void ignoreItem_isIgnored() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.IGNORE));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
    }

    @Test
    void outcomeCarriesTrackingIdAndDummyData() {
        ChunkItem outcome = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.SUCCESS)).chunkItem();

        assertThat("id", outcome.getId(), is((long) ITEM_ID));
        assertThat("tracking id", outcome.getTrackingId(), is(TRACKING_ID));
        assertThat("data", StringUtil.asString(outcome.getData()), is("Set by DummySink"));
        assertThat("type", outcome.getType(), is(List.of(ChunkItem.Type.STRING)));
    }

    /**
     * This sink keeps the delivery watermark rather than opting out of it, which is
     * observable only as the lookup being made.
     */
    @Test
    void watermarkIsConsulted() throws InvalidMessageException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.<Watermark>empty());

        consumer.handleConsumedMessage(itemMessage());

        verify(jobStoreServiceConnector).getWatermark((int) SINK_ID, RECORD_KEY);
    }

    private ChunkItem item(ChunkItem.Status status) {
        return new ChunkItem()
                .withId(ITEM_ID)
                .withStatus(status)
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(TRACKING_ID)
                .withData("processing outcome");
    }

    private ConsumedMessage itemMessage() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        try {
            return new ConsumedMessage("id", headers, jsonbContext.marshall(item(ChunkItem.Status.SUCCESS)));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
