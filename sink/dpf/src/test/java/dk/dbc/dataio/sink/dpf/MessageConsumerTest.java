package dk.dbc.dataio.sink.dpf;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageConsumerTest {
    private final MessageConsumer messageConsumer = newMessageConsumerBean();

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

        final Chunk result = messageConsumer.handleChunk(chunk);

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

    private MessageConsumer newMessageConsumerBean() {
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(Mockito.mock(JobStoreServiceConnector.class)).build();
        return new MessageConsumer(hub, new ServiceBroker());
    }
}
