package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateMessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "870970:12345678";

    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final OpenUpdateConfig openUpdateConfig = mock(OpenUpdateConfig.class);
    private final AddiRecordPreprocessor addiRecordPreprocessor = Mockito.spy(new AddiRecordPreprocessor());
    private final OpenUpdateSinkConfig config = new OpenUpdateSinkConfig()
            .withEndpoint("testEndpoint").withUserId("testUser").withPassword("testPass");
    private final JSONBContext jsonbContext = new JSONBContext();
    private final String queueProvider = "queue";
    private final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().setQueueProvider(queueProvider).build();
    private final FlowBinder flowBinder = new FlowBinderBuilder().setId(42).setVersion(10).setContent(flowBinderContent).build();
    private final ServiceHub serviceHub = new ServiceHub.Builder()
            .withJobStoreServiceConnector(jobStoreServiceConnector).test();
    private final UpdateMessageConsumer consumer = new UpdateMessageConsumer(
            serviceHub, flowStoreServiceConnector, openUpdateConfig, addiRecordPreprocessor);

    static byte[] getValidAddi(String... content) {
        StringBuilder addi = new StringBuilder();
        for (String s : content) {
            addi.append(String.format("19\n<es:referencedata/>\n%d\n%s\n",
                    s.getBytes(StandardCharsets.UTF_8).length, s));
        }
        return addi.toString().getBytes(StandardCharsets.UTF_8);
    }

    @BeforeEach
    void setupMocks() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getFlowBinder(flowBinder.getId())).thenReturn(flowBinder);
        when(openUpdateConfig.getConfig(any(ConsumedMessage.class))).thenReturn(config);
        UpdateMessageConsumer.cachedFlowBinders.invalidateAll();
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
    void successItem_callsAddiRecordPreprocessorWithQueueProvider() {
        ChunkItem item = new ChunkItemBuilder().setData(getValidAddi("addi content")).build();

        consumer.deliverItem(message(), item);

        verify(addiRecordPreprocessor).preprocess(any(AddiRecord.class), eq(queueProvider));
    }

    @Test
    void flowBinderIsCached() throws FlowStoreServiceConnectorException {
        ChunkItem item = new ChunkItemBuilder().setData(getValidAddi("addi content")).build();

        consumer.deliverItem(message(), item);

        verify(flowStoreServiceConnector, times(1)).getFlowBinder(flowBinder.getId());
        assertThat(UpdateMessageConsumer.cachedFlowBinders.getIfPresent(flowBinder.getId()), is(notNullValue()));

        consumer.deliverItem(message(), item);

        verify(flowStoreServiceConnector, times(1)).getFlowBinder(flowBinder.getId());
    }

    @Test
    void flowStoreCommunicationFails_throws() throws FlowStoreServiceConnectorException {
        FlowStoreServiceConnectorException flowStoreServiceConnectorException =
                new FlowStoreServiceConnectorException("Exception from flow-store");
        when(flowStoreServiceConnector.getFlowBinder(flowBinder.getId())).thenThrow(flowStoreServiceConnectorException);
        ChunkItem item = new ChunkItemBuilder().setData(getValidAddi("addi content")).build();

        try {
            consumer.deliverItem(message(), item);
            fail("No RuntimeException thrown");
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(flowStoreServiceConnectorException));
        }
    }

    @Test
    void firstItemCreatesConnector() {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build();

        consumer.deliverItem(message(), item);
        OpenUpdateServiceConnector first = consumer.connector;
        assertThat("first item creates connector", first, is(notNullValue()));
        assertThat("first item sets config", consumer.config, is(config));

        consumer.deliverItem(message(), item);
        assertThat("second item retains connector", consumer.connector, is(first));
    }

    @Test
    void configChangeReplacesConnector() {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build();

        consumer.deliverItem(message(), item);
        OpenUpdateServiceConnector first = consumer.connector;

        OpenUpdateSinkConfig updatedConfig = new OpenUpdateSinkConfig()
                .withEndpoint("updatedEndpoint")
                .withUserId("updatedUser")
                .withPassword("updatedPass");
        when(openUpdateConfig.getConfig(any(ConsumedMessage.class))).thenReturn(updatedConfig);

        consumer.deliverItem(message(), item);
        assertThat("config change replaces connector", consumer.connector, is(not(first)));
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
        headers.put(JMSHeader.flowBinderId.name, flowBinder.getId());
        headers.put(JMSHeader.flowBinderVersion.name, flowBinder.getVersion());
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
