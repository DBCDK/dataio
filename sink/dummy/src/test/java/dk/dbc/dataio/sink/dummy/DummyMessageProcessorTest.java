package dk.dbc.dataio.sink.dummy;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DummyMessageProcessorTest {
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final Map<String, Object> headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
    private final String trackingId = "rr:1223io:12534";

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws InvalidMessageException, JSONBException, JobStoreServiceConnectorException {
        final String messageId = "id";
        Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(0).setChunkId(0L).build();
        String payload = new JSONBContext().marshall(processedChunk);
        ConsumedMessage consumedMessage = new ConsumedMessage(messageId, headers, payload);
        getDummyMessageProcessorBean().handleConsumedMessage(consumedMessage);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(Chunk.class), anyInt(), anyLong());
    }

    @Test
    public void processPayload_chunkResultArgIsNonEmpty_returnsNonEmptyDeliveredChunk() {
        List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).setTrackingId(trackingId).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).setTrackingId(trackingId).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).setTrackingId(trackingId).build()
        );
        Chunk chunkResult = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();

        Chunk deliveredChunk = getDummyMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(item0.getTrackingId(), is(trackingId));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(processedChunkItems.get(1).getStatus()));
        assertThat(item1.getTrackingId(), is(trackingId));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(processedChunkItems.get(2).getStatus()));
        assertThat(item2.getTrackingId(), is(trackingId));
        assertThat(iterator.hasNext(), is(false));
    }

    private DummyMessageConsumer getDummyMessageProcessorBean() {
        return new DummyMessageConsumer(new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).withHealthService(null).build());
    }
}
