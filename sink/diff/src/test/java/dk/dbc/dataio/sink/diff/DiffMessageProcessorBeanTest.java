package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DiffMessageProcessorBeanTest {
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws ServiceException, InvalidMessageException, JsonException, JobStoreServiceConnectorException {
        final String messageId = "id";
        final String payloadType = JmsConstants.CHUNK_PAYLOAD_TYPE;
        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(0L).setChunkId(0L).build();
        final String payload = JsonUtil.toJson(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, payloadType, payload);
        getDiffMessageProcessorBean().handleConsumedMessage(consumedMessage);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong());
    }


    @Test
    public void failOnMissingNextItems() throws Exception {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build()
        );
        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();
        final ExternalChunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(false));

    }

    @Test
    public void processPayload_FailDifferentContent() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData("Item1").setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData("Item2").setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData("Item3").setStatus(ChunkItem.Status.IGNORE).build()
        );
        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData("nextItem1").setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData("nextItem2").setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData("Item3").setStatus(ChunkItem.Status.IGNORE).build()
        );

        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final ExternalChunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void processPayload_FailDifferentStatus() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData("Item1").setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData("Item2").setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData("Item3").setStatus(ChunkItem.Status.IGNORE).build()
        );
        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData("Item1").setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData("Item2").setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData("Item3").setStatus(ChunkItem.Status.SUCCESS).build()
        );

        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final ExternalChunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(false));
    }



    private DiffMessageProcessorBean getDiffMessageProcessorBean() {
        final DiffMessageProcessorBean diffMessageProcessorBean = new DiffMessageProcessorBean();
        diffMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return diffMessageProcessorBean;
    }
}
