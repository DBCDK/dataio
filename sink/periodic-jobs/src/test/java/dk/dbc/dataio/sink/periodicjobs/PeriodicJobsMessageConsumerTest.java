package dk.dbc.dataio.sink.periodicjobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers this sink's own delivery surface: the verdict each processing outcome maps to,
 * the job-end branch, and the watermark opt-out. Header reading, the watermark decision
 * itself and result reporting belong to {@code SinkMessageConsumerAdapter} and are covered
 * by its own test.
 */
class PeriodicJobsMessageConsumerTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final short ITEM_ID = 3;
    private static final long SINK_ID = 15;
    private static final String TRACKING_ID = "rr:1223io:12534";

    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final EntityTransaction transaction = mock(EntityTransaction.class);
    private final PeriodicJobsFinalizerBean periodicJobsFinalizerBean = mock(PeriodicJobsFinalizerBean.class);

    private PeriodicJobsMessageConsumer consumer;

    @BeforeEach
    void setupConsumer() {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);
        consumer = new PeriodicJobsMessageConsumer(
                new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test(),
                entityManagerFactory);
        consumer.periodicJobsFinalizerBean = periodicJobsFinalizerBean;
    }

    @Test
    void successfulConversion_isDelivered() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), addiItem("record-1"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Converted"));
        verify(entityManager).persist(any(PeriodicJobsDataBlock.class));
    }

    /**
     * IGNORED rather than DELIVERED is what keeps an item this sink converted nothing for
     * counted as ignored in the delivering phase, as it was when whole chunks were
     * delivered.
     */
    @Test
    void processorFailure_isIgnored() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.FAILURE, "irrelevant"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Failed by processor"));
        verify(entityManager, never()).persist(any());
    }

    @Test
    void processorIgnore_isIgnored() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), item(ChunkItem.Status.IGNORE, "irrelevant"));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Ignored by processor"));
        verify(entityManager, never()).persist(any());
    }

    @Test
    void conversionFailure_isFailed() {
        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), addiItem(""));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(notNullValue()));
        verify(entityManager, never()).persist(any());
    }

    @Test
    void outcomeCarriesItemIdAndTrackingId() {
        ChunkItem outcome = consumer.deliverItem(itemMessage(), addiItem("record-1")).chunkItem();

        assertThat("id", outcome.getId(), is((long) ITEM_ID));
        assertThat("tracking id", outcome.getTrackingId(), is(TRACKING_ID));
    }

    /**
     * The job and chunk ids the finalizer chain is handed must be the ones the item
     * message carries, since the chunk id is what tells an empty job from one with data.
     */
    @Test
    void terminationItem_isFinalized() throws InvalidMessageException {
        when(periodicJobsFinalizerBean.finalizeJob(anyInt(), anyInt(), any(EntityManager.class)))
                .thenReturn(deliveredJobEndItem());

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), terminationItem(ChunkItem.Status.SUCCESS));

        verify(periodicJobsFinalizerBean).finalizeJob(eq(JOB_ID), eq(CHUNK_ID), any(EntityManager.class));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome type", result.chunkItem().getType(), is(List.of(ChunkItem.Type.JOB_END)));
    }

    /**
     * On the chunk path this exception had the message discarded without a result, leaving
     * the job forever incomplete. A FAILED verdict completes it and sets its fatal error
     * flag instead.
     */
    @Test
    void terminationItem_finalizationRejected_isFailed() throws InvalidMessageException {
        when(periodicJobsFinalizerBean.finalizeJob(anyInt(), anyInt(), any(EntityManager.class)))
                .thenThrow(new InvalidMessageException("no pickup for you"));

        ItemDeliveryResult result = consumer.deliverItem(itemMessage(), terminationItem(ChunkItem.Status.SUCCESS));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics(), is(notNullValue()));
    }

    /**
     * The datablock write must be committed before the framework reports the item, since
     * that report is what releases the job's termination chunk, and the finalization it
     * triggers reads the datablocks with a different EntityManager in a different
     * transaction. Reporting first is the ordering the chunk protocol had, and the reason
     * it needed a sleep before finalizing.
     */
    @Test
    void commitPrecedesResultReport() throws InvalidMessageException, JobStoreServiceConnectorException {
        consumer.handleConsumedMessage(itemMessage());

        InOrder ordered = inOrder(transaction, jobStoreServiceConnector);
        ordered.verify(transaction).commit();
        ordered.verify(jobStoreServiceConnector).addItemDelivered(any(ItemDeliveryResult.class), anyInt(),
                anyInt(), anyShort());
    }

    /**
     * This sink opts out of the delivery watermark, which is observable only as the lookup
     * not being made.
     */
    @Test
    void watermarkIsNotConsulted() throws InvalidMessageException, JobStoreServiceConnectorException {
        consumer.handleConsumedMessage(itemMessage());

        verify(jobStoreServiceConnector, never()).getWatermark(anyInt(), anyString());
        verify(jobStoreServiceConnector).addItemDelivered(any(ItemDeliveryResult.class), anyInt(), anyInt(),
                anyShort());
    }

    private ChunkItem item(ChunkItem.Status status, String data) {
        return new ChunkItem()
                .withId(ITEM_ID)
                .withStatus(status)
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(TRACKING_ID)
                .withData(data);
    }

    private ChunkItem addiItem(String record) {
        return new ChunkItem()
                .withId(ITEM_ID)
                .withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.ADDI)
                .withTrackingId(TRACKING_ID)
                .withData(newAddiRecord(record).getBytes());
    }

    private ChunkItem terminationItem(ChunkItem.Status status) {
        return new ChunkItem()
                .withId(0)
                .withStatus(status)
                .withType(ChunkItem.Type.JOB_END)
                .withTrackingId(JOB_ID + ".JOB_END")
                .withData("Job termination item");
    }

    private ChunkItem deliveredJobEndItem() {
        return ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.JOB_END)
                .withData("delivered to pickup");
    }

    private AddiRecord newAddiRecord(String record) {
        try {
            byte[] metadata = new ObjectMapper().writeValueAsBytes(new ConversionParam());
            return new AddiRecord(metadata, record.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConsumedMessage itemMessage() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        try {
            return new ConsumedMessage("id", headers, jsonbContext.marshall(addiItem("record-1")));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
