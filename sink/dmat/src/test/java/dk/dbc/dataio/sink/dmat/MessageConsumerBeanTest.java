package dk.dbc.dataio.sink.dmat;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

public class MessageConsumerBeanTest {
    private final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();

    @Test
    public void testHandleChunk() {
        final List<ChunkItem> chunkItems = Arrays.asList(
                ChunkItem.failedChunkItem().withId(0L),     // failed by job processor
                ChunkItem.ignoredChunkItem().withId(1L),    // ignored by job processor
                ChunkItem.successfulChunkItem().withId(2L)  // invalid processing instructions
                        .withData("invalid-chunk"), // Todo: We may have to be more specifically invalid
                ChunkItem.successfulChunkItem().withId(3L)  // successfully delivered
                        .withData("{}") // Todo: Add minimalistic dmat record data json
        );

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = messageConsumerBean.handleChunk(chunk);

        MatcherAssert.assertThat("number of chunk items", result.size(), is(4));
        MatcherAssert.assertThat("1st chunk item", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.IGNORE));
        MatcherAssert.assertThat("2nd chunk item", result.getItems().get(1).getStatus(),
                is(ChunkItem.Status.IGNORE));
        MatcherAssert.assertThat("3rd chunk item", result.getItems().get(2).getStatus(),
                is(ChunkItem.Status.SUCCESS));  // Todo: Should be FAILURE when handling has been implemented
        MatcherAssert.assertThat("4th chunk item", result.getItems().get(3).getStatus(),
                is(ChunkItem.Status.SUCCESS));
    }

    private MessageConsumerBean newMessageConsumerBean() {
        final MessageConsumerBean messageConsumerBean = new MessageConsumerBean();
        return messageConsumerBean;
    }
}
