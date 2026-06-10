package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateMessageConsumerTest {
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
    void handleConsumedMessage_jobStoreCommunicationFails_throws()
            throws InvalidMessageException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(any(Chunk.class), anyInt(), anyLong()))
                .thenThrow(new JobStoreServiceConnectorException("job-store down"));

        try {
            consumer.handleConsumedMessage(consumedMessage(ignoredChunk()));
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertThat("exception propagated from job-store call", e, notNullValue());
        }
    }

    @Test
    void handleConsumedMessage_ignoredChunkItem_returnsIgnored()
            throws InvalidMessageException, JobStoreServiceConnectorException {
        consumer.handleConsumedMessage(consumedMessage(ignoredChunk()));

        ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkCaptor.capture(), anyInt(), anyLong());
        ChunkItem delivered = chunkCaptor.getValue().getItems().get(0);
        assertThat(delivered.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(new String(delivered.getData()), containsString("Ignored by processor"));
    }

    @Test
    void handleConsumedMessage_setsConnector() throws InvalidMessageException {
        consumer.handleConsumedMessage(consumedMessage(ignoredChunk()));
        UpdateServiceConnector first = consumer.connector;
        assertThat("first message creates connector", first, notNullValue());

        consumer.handleConsumedMessage(consumedMessage(ignoredChunk()));
        assertThat("second message retains connector", consumer.connector, is(first));
    }

    @Test
    void handleConsumedMessage_configChange_replacesConnector() throws InvalidMessageException {
        consumer.handleConsumedMessage(consumedMessage(ignoredChunk()));
        UpdateServiceConnector first = consumer.connector;

        OpenUpdateSinkConfig updatedConfig = new OpenUpdateSinkConfig()
                .withEndpoint("http://other-service")
                .withUserId("user2")
                .withPassword("pass2");
        when(configRefresher.getConfig(any(ConsumedMessage.class))).thenReturn(updatedConfig);

        consumer.handleConsumedMessage(consumedMessage(ignoredChunk()));
        assertThat("config change replaces connector", consumer.connector, not(first));
    }

    @Test
    void handleConsumedMessage_successChunkItem_callsProcess()
            throws InvalidMessageException, JobStoreServiceConnectorException {
        ChunkItem item = new ChunkItemBuilder()
                .setData("not-json".getBytes())
                .setStatus(ChunkItem.Status.SUCCESS)
                .build();
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(List.of(item)).build();

        consumer.handleConsumedMessage(consumedMessage(chunk));

        ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkCaptor.capture(), anyInt(), anyLong());
        ChunkItem delivered = chunkCaptor.getValue().getItems().get(0);
        assertThat(delivered.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(new String(delivered.getData()), containsString("Failed to parse update record list"));
    }

    @Test
    void handleConsumedMessage_failureChunkItem_returnsIgnored()
            throws InvalidMessageException, JobStoreServiceConnectorException {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.FAILURE).build();
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(List.of(item)).build();

        consumer.handleConsumedMessage(consumedMessage(chunk));

        ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkCaptor.capture(), anyInt(), anyLong());
        ChunkItem delivered = chunkCaptor.getValue().getItems().get(0);
        assertThat(delivered.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(new String(delivered.getData()), containsString("Failed by processor"));
    }

    private ConsumedMessage consumedMessage(Chunk chunk) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, 1L);
            headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, 1L);
            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private Chunk ignoredChunk() {
        ChunkItem item = new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build();
        return new ChunkBuilder(Chunk.Type.PROCESSED).setItems(List.of(item)).build();
    }
}
