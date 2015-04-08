package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

public class DummyMessageProcessorBeanTest {
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws ServiceException, InvalidMessageException, JsonException {
        final String messageId = "id";
        final String payloadType = JmsConstants.CHUNK_PAYLOAD_TYPE;
        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(0L).setChunkId(0L).build();
        final String payload = JsonUtil.toJson(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, payloadType, payload);
        getDummyMessageProcessorBean().handleConsumedMessage(consumedMessage);
        
        verify(jobProcessorMessageProducerBean, times(1)).send(any(ExternalChunk.class));
    }

    @Test
    public void processPayload_chunkResultArgIsNonEmpty_returnsNonEmptyDeliveredChunk() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build()
        );
        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();

        final ExternalChunk deliveredChunk = getDummyMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(processedChunkItems.get(1).getStatus()));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(processedChunkItems.get(2).getStatus()));
        assertThat(iterator.hasNext(), is(false));
    }

    private DummyMessageProcessorBean getDummyMessageProcessorBean() {
        final DummyMessageProcessorBean dummyMessageProcessorBean = new DummyMessageProcessorBean();
        dummyMessageProcessorBean.jobProcessorMessageProducer = jobProcessorMessageProducerBean;
        return dummyMessageProcessorBean;
    }
}
