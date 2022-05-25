package dk.dbc.dataio.sink.dummy;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DummyMessageProcessorBeanTest {
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private Map<String, Object> headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
    private final String trackingId = "rr:1223io:12534";

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws ServiceException, InvalidMessageException, JSONBException, JobStoreServiceConnectorException {
        final String messageId = "id";
        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(0L).setChunkId(0L).build();
        final String payload = new JSONBContext().marshall(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, headers, payload);
        getDummyMessageProcessorBean().handleConsumedMessage(consumedMessage);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void processPayload_chunkResultArgIsNonEmpty_returnsNonEmptyDeliveredChunk() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).setTrackingId(trackingId).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).setTrackingId(trackingId).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).setTrackingId(trackingId).build()
        );
        final Chunk chunkResult = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();

        final Chunk deliveredChunk = getDummyMessageProcessorBean().processPayload(chunkResult);
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

    private DummyMessageProcessorBean getDummyMessageProcessorBean() {
        final DummyMessageProcessorBean dummyMessageProcessorBean = new DummyMessageProcessorBean();
        dummyMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return dummyMessageProcessorBean;
    }
}
