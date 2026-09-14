package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateMessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "870970:12345678";

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final ConfigRefresher configRefresher = mock(ConfigRefresher.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private final OpenUpdateSinkConfig config = new OpenUpdateSinkConfig()
            .withEndpoint("http://update-service")
            .withUserId("user")
            .withPassword("secret");
    private final ServiceHub serviceHub = new ServiceHub.Builder()
            .withJobStoreServiceConnector(jobStoreServiceConnector).test();
    private final UpdateMessageConsumer consumer = new UpdateMessageConsumer(serviceHub, configRefresher);

    @BeforeEach
    void setupMocks() {
        when(configRefresher.getConfig(any(ConsumedMessage.class))).thenReturn(config);
    }

    @Test
    void failureItem_isIgnored() {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.FAILURE).build();

        ItemDeliveryResult result = consumer.deliverItem(message(), item);

        assertThat(result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat(result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(new String(result.chunkItem().getData(), StandardCharsets.UTF_8),
                containsString("Failed by processor"));
    }

    @Test
    void ignoreItem_isIgnored() {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build();

        ItemDeliveryResult result = consumer.deliverItem(message(), item);

        assertThat(result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat(result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(new String(result.chunkItem().getData(), StandardCharsets.UTF_8),
                containsString("Ignored by processor"));
    }

    @Test
    void unparseableSuccessItem_isFailed() {
        ChunkItem item = new ChunkItemBuilder()
                .setData("not-json".getBytes(StandardCharsets.UTF_8))
                .setStatus(ChunkItem.Status.SUCCESS)
                .build();

        ItemDeliveryResult result = consumer.deliverItem(message(), item);

        assertThat(result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat(result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(new String(result.chunkItem().getData(), StandardCharsets.UTF_8),
                containsString("Failed to parse update record list"));
    }

    @Test
    void firstItemCreatesConnector() {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build();

        consumer.deliverItem(message(), item);
        UpdateServiceConnector first = consumer.connector;
        assertThat("first item creates connector", first, notNullValue());

        consumer.deliverItem(message(), item);
        assertThat("second item retains connector", consumer.connector, is(first));
    }

    @Test
    void configChangeReplacesConnector() {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build();

        consumer.deliverItem(message(), item);
        UpdateServiceConnector first = consumer.connector;

        OpenUpdateSinkConfig updatedConfig = new OpenUpdateSinkConfig()
                .withEndpoint("http://other-service")
                .withUserId("user2")
                .withPassword("pass2");
        when(configRefresher.getConfig(any(ConsumedMessage.class))).thenReturn(updatedConfig);

        consumer.deliverItem(message(), item);
        assertThat("config change replaces connector", consumer.connector, not(first));
    }

    /**
     * Pins the sink's choice to stay subject to the delivery watermark, which is otherwise
     * unobservable: {@code usesDeliveryWatermark} is protected in the framework and this test class
     * is not a subclass of it.
     */
    @Test
    void watermarkIsConsulted() throws InvalidMessageException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.<Watermark>empty());

        consumer.handleConsumedMessage(message());

        verify(jobStoreServiceConnector).getWatermark((int) SINK_ID, RECORD_KEY);
    }

    private ConsumedMessage message() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.sinkVersion.name, 1L);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        try {
            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(new ChunkItemBuilder()
                    .setId(ITEM_ID)
                    .setStatus(ChunkItem.Status.IGNORE)
                    .build()));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
