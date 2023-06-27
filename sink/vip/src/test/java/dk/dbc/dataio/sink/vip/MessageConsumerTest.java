package dk.dbc.dataio.sink.vip;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

// TODO: 24-04-19 Add wiremock request/response tests when VIP-CORE service is available

public class MessageConsumerTest {
    private final ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(mock(JobStoreServiceConnector.class)).build();
    private final MessageConsumer messageConsumer = new MessageConsumer(hub, new ConfigBean(mock(FlowStoreServiceConnector.class)));

    private static AddiRecord createAddiRecord(String metadata, String content) {
        return new AddiRecord(StringUtil.asBytes(metadata), StringUtil.asBytes(content));
    }

    @Test
    public void handleChunk() {
        List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(), // failed by job processor
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.IGNORE).build(),  // ignored by job processor
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.SUCCESS)   // invalid addi metadata
                        .setData(createAddiRecord("not JSON", "{}").getBytes())
                        .build()
        );
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        Chunk result = messageConsumer.handleChunk(chunk);

        assertThat("number of chunk items", result.size(), is(3));
        assertThat("1st chunk item", result.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item", result.getItems().get(1).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("3rd chunk item", result.getItems().get(2).getStatus(), is(ChunkItem.Status.FAILURE));
    }
}
