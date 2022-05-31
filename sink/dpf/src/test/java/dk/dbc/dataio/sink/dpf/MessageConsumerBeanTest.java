package dk.dbc.dataio.sink.dpf;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageConsumerBeanTest {
    private final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();

    @Test
    public void handleChunk() {
        final List<ChunkItem> chunkItems = Arrays.asList(
                ChunkItem.failedChunkItem().withId(0L),     // failed by job processor
                ChunkItem.ignoredChunkItem().withId(1L),    // ignored by job processor
                ChunkItem.successfulChunkItem().withId(2L)  // invalid processing instructions
                        .withData(createAddiRecord("not JSON", "{}").getBytes())
        );

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = messageConsumerBean.handleChunk(chunk);

        assertThat("number of chunk items", result.size(), is(3));
        assertThat("1st chunk item", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item", result.getItems().get(1).getStatus(),
                is(ChunkItem.Status.IGNORE));
        assertThat("3rd chunk item", result.getItems().get(2).getStatus(),
                is(ChunkItem.Status.FAILURE));
    }

    private static AddiRecord createAddiRecord(String metadata, String content) {
        return new AddiRecord(StringUtil.asBytes(metadata), StringUtil.asBytes(content));
    }

    private MessageConsumerBean newMessageConsumerBean() {
        final MessageConsumerBean messageConsumerBean = new MessageConsumerBean();
        messageConsumerBean.configBean = new ConfigBean();
        return messageConsumerBean;
    }
}
