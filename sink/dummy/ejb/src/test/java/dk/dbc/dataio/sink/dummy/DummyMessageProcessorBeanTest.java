package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DummyMessageProcessorBeanTest {
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws ServiceException, InvalidMessageException {
        final String messageId = "id";
        final String payloadType = "ChunkResult";
        final String payload = new ChunkResultJsonBuilder().build();
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, payloadType, payload);
        getDummyMessageProcessorBean().handleConsumedMessage(consumedMessage);

        verify(jobProcessorMessageProducerBean, times(1)).send(any(SinkChunkResult.class));
    }

    @Test
    public void processPayload_chunkResultArgIsNonEmpty_returnsNonEmptySinkChunkResult() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build()
        );
        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();

        final SinkChunkResult sinkChunkResult = getDummyMessageProcessorBean().processPayload(chunkResult);
        assertThat(sinkChunkResult.getItems().size(), is(processedChunkItems.size()));
        assertThat(sinkChunkResult.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(sinkChunkResult.getItems().get(1).getStatus(), is(processedChunkItems.get(1).getStatus()));
        assertThat(sinkChunkResult.getItems().get(2).getStatus(), is(processedChunkItems.get(2).getStatus()));
    }

    private DummyMessageProcessorBean getDummyMessageProcessorBean() {
        final DummyMessageProcessorBean dummyMessageProcessorBean = new DummyMessageProcessorBean();
        dummyMessageProcessorBean.jobProcessorMessageProducer = jobProcessorMessageProducerBean;
        return dummyMessageProcessorBean;
    }
}
