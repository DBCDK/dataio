package dk.dbc.dataio.sink.marcconv.jms;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the sink's own half of the delivery protocol, which is its watermark opt-out.
 * Header reading, the watermark comparison and result reporting belong to
 * {@code SinkMessageConsumerAdapter} and are covered by its own test, and what the sink
 * does with an item is covered against a database in {@code MessageConsumerIT}.
 */
class MessageConsumerTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final EntityTransaction transaction = mock(EntityTransaction.class);

    /**
     * The opt-out is unconditional, so one item stands for every kind of item the sink
     * receives. The message deliberately carries a record key, to show that a key being
     * present is not what decides it.
     */
    @Test
    void noWatermarkIsRead() throws InvalidMessageException, JobStoreServiceConnectorException {
        newMessageConsumer().handleConsumedMessage(newItemMessage(
                new ChunkItem()
                        .withId(0)
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withType(ChunkItem.Type.STRING)
                        .withData("ignored by processor")));

        verify(jobStoreServiceConnector, never()).getWatermark(anyInt(), anyString());
    }

    private MessageConsumer newMessageConsumer() {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);
        ServiceHub hub = new ServiceHub.Builder()
                .withJobStoreServiceConnector(jobStoreServiceConnector)
                .test();
        return new MessageConsumer(hub, mock(FileStoreServiceConnector.class), entityManagerFactory);
    }

    /**
     * Builds the message job-store dispatches per item, deliberately including the record
     * key this sink must not act on
     */
    private ConsumedMessage newItemMessage(ChunkItem item) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, 42);
        headers.put(JMSHeader.chunkId.name, 3L);
        headers.put(JMSHeader.itemId.name, (short) 0);
        headers.put(JMSHeader.sinkId.name, 1L);
        headers.put(JMSHeader.recordKey.name, "870970:record");
        return new ConsumedMessage("messageId", headers, marshall(item));
    }

    private String marshall(ChunkItem item) {
        try {
            return jsonbContext.marshall(item);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
