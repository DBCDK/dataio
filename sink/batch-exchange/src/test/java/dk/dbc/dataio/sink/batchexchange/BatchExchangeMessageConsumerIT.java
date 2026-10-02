package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Covers what this sink itself decides, which is what {@code deliverItem} returns and what it
 * stages. The header reading, the delivery watermark and the result reporting around it belong
 * to {@code SinkMessageConsumerAdapter} and are covered by its own test.
 */
class BatchExchangeMessageConsumerIT extends IntegrationTest {
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "870970:12345678";
    private static final int JOB_ID = 42;
    private static final long CHUNK_ID = 0;
    private static final short ITEM_ID = 7;

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    private final String addiMetadata = "<referenceData><info submitter=\"424242\"/></referenceData>";
    private final AddiRecord addiRecordX = new AddiRecord(addiMetadata.getBytes(), "contentX".getBytes());
    private final AddiRecord addiRecordY = new AddiRecord(addiMetadata.getBytes(), "contentY".getBytes());
    private final AddiRecord addiRecordWithInvalidMetadata =
            new AddiRecord("Invalid XML metadata".getBytes(), "content".getBytes());

    @Test
    void successfulItem_isStagedAndItsResultDeferred() {
        ChunkItem item = successItem(addiRecordX.getBytes());

        ItemDeliveryResult result = consumer().deliverItem(message(RECORD_KEY, Priority.HIGH), item);

        assertThat("no result, the consumer system still owes one", result, is(nullValue()));

        Batch batch = onlyBatch();
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch status", batch.getStatus(), is(Batch.Status.PENDING));
        assertThat("batch name names the item and its watermark row", batch.getName(),
                is("15-870970:12345678-42-0-7"));

        List<BatchEntry> entries = entriesOf(batch);
        assertThat("number of entries", entries.size(), is(1));
        assertThat("entry status", entries.get(0).getStatus(), is(BatchEntry.Status.PENDING));
        assertThat("entry content", StringUtil.asString(entries.get(0).getContent()), is("contentX"));
        assertThat("entry tracking id", entries.get(0).getTrackingId(), is("tracking"));
        assertThat("entry priority", entries.get(0).getPriority(), is(Priority.HIGH.getValue()));
    }

    @Test
    void itemHoldingSeveralRecords_becomesOneBatchOfChainedEntries() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(addiRecordX.getBytes());
        data.write(addiRecordY.getBytes());
        data.write(addiRecordX.getBytes());

        ItemDeliveryResult result = consumer().deliverItem(message(RECORD_KEY), successItem(data.toByteArray()));

        assertThat("result is deferred", result, is(nullValue()));

        List<BatchEntry> entries = entriesOf(onlyBatch());
        assertThat("number of entries", entries.size(), is(3));
        assertThat("1st entry is continued", entries.get(0).getContinued(), is(true));
        assertThat("2nd entry is continued", entries.get(1).getContinued(), is(true));
        assertThat("last entry ends the item", entries.get(2).getContinued(), is(false));
    }

    @Test
    void itemWithoutATrackingId_entriesAreStampedWithTheItemsIdentity() {
        ChunkItem item = new ChunkItemBuilder()
                .setId(ITEM_ID)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(addiRecordX.getBytes())
                .setTrackingId(null)
                .build();

        consumer().deliverItem(message(RECORD_KEY), item);

        assertThat(entriesOf(onlyBatch()).get(0).getTrackingId(), is("io:42-0-7"));
    }

    @Test
    void itemFailedByProcessor_isIgnoredAndNothingIsStaged() {
        ChunkItem item = new ChunkItemBuilder()
                .setId(ITEM_ID)
                .setStatus(ChunkItem.Status.FAILURE)
                .setTrackingId("tracking")
                .build();

        ItemDeliveryResult result = consumer().deliverItem(message(RECORD_KEY), item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Failed by processor"));
        assertThat("nothing staged", batchCount(), is(0L));
    }

    @Test
    void itemIgnoredByProcessor_isIgnoredAndNothingIsStaged() {
        ChunkItem item = new ChunkItemBuilder()
                .setId(ITEM_ID)
                .setStatus(ChunkItem.Status.IGNORE)
                .setTrackingId("tracking")
                .build();

        ItemDeliveryResult result = consumer().deliverItem(message(RECORD_KEY), item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Ignored by processor"));
        assertThat("nothing staged", batchCount(), is(0L));
    }

    @Test
    void itemWithUnreadableAddi_isFailedAndNothingIsStaged() {
        ItemDeliveryResult result = consumer().deliverItem(message(RECORD_KEY),
                successItem(addiRecordWithInvalidMetadata.getBytes()));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome carries a fatal diagnostic", result.chunkItem().getDiagnostics().size(), is(1));
        assertThat("nothing staged", batchCount(), is(0L));
    }

    /* An empty batch would never complete, so the finalizer would never report the item and
       the job would never finish. */
    @Test
    void itemWithoutRecords_isIgnoredRatherThanStagedAsAnEmptyBatch() {
        ItemDeliveryResult result = consumer().deliverItem(message(RECORD_KEY), successItem(new byte[0]));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("No records to deliver"));
        assertThat("nothing staged", batchCount(), is(0L));
    }

    @Test
    void itemAlreadyStaged_isNotStagedTwiceAndItsResultStaysDeferred() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        assertThat("still deferred to the finalizer", result, is(nullValue()));
        assertThat("only one batch", batchCount(), is(1L));
    }

    @Test
    void newerVersionOfTheRecordAlreadyStaged_itemIsSuperseded() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID + 1, (short) 0),
                successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.SUPERSEDED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome names the version that won", StringUtil.asString(result.chunkItem().getData()),
                is("Item was skipped, version 43/0/0 of this record is already staged for delivery"));
        assertThat("the newer version alone is staged", batchCount(), is(1L));
    }

    /* Staging both would leave the consumer system to apply them in whatever order its thread
       pool arrives at, and the target could end up holding the older one. */
    @Test
    void olderVersionOfTheRecordInFlight_itemIsHeldRatherThanStagedAlongsideIt() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID - 1, (short) 0),
                successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        assertThat("result is deferred", result, is(nullValue()));
        assertThat("the older version alone is staged", batchCount(), is(1L));
        assertThat("the newer version is held", heldItems().size(), is(1));
        assertThat("and it is the newer one", heldItems().get(0).getJobId(), is(JOB_ID));
    }

    @Test
    void itemAlreadyHeld_isNotHeldTwiceAndNothingIsReportedForIt() throws Exception {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID - 1, (short) 0),
                successItem(addiRecordX.getBytes()));
        consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        assertThat("still deferred", result, is(nullValue()));
        assertThat("one held item", heldItems().size(), is(1));
        assertThat("nothing staged for it", batchCount(), is(1L));
        verify(jobStoreServiceConnector, never()).addItemDelivered(any(), anyInt(), anyInt(), anyShort());
    }

    @Test
    void newerVersionOfTheRecordAlreadyHeld_itemIsSuperseded() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID - 1, (short) 0),
                successItem(addiRecordX.getBytes()));
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID + 1, ITEM_ID),
                successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.SUPERSEDED));
        assertThat("outcome names the version that won", StringUtil.asString(result.chunkItem().getData()),
                is("Item was skipped, version 43/0/7 of this record is already held for delivery"));
        assertThat("the held version is untouched", heldItems().get(0).getJobId(), is(JOB_ID + 1));
    }

    /* Only the newest version of a record ever waits. Applying the ones it overtook would
       reach a state immediately overwritten, which is the rule the delivery watermark already
       applies to versions that reached the target. */
    @Test
    void severalVersionsArrivingWhileOneIsInFlight_collapseToTheNewest() throws Exception {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID + 1, ITEM_ID),
                successItem(addiRecordX.getBytes()));
        assertThat("one held after the 2nd version", heldItems().size(), is(1));

        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID + 2, ITEM_ID),
                successItem(addiRecordX.getBytes()));
        assertThat("one held after the 3rd version", heldItems().size(), is(1));

        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID + 3, ITEM_ID),
                successItem(addiRecordX.getBytes()));

        assertThat("one held after the 4th version", heldItems().size(), is(1));
        assertThat("the newest version is the one held", heldItems().get(0).getJobId(), is(JOB_ID + 3));
        assertThat("only the in-flight version is staged", batchCount(), is(1L));

        HeldItem held = heldItems().get(0);
        assertThat("versions skipped to arrive at it", held.getCollapsedCount(), is(2));
        assertThat("the oldest of them", held.getCollapsedFrom(), is("43/0/7"));

        ArgumentCaptor<ItemDeliveryResult> reported = ArgumentCaptor.forClass(ItemDeliveryResult.class);
        verify(jobStoreServiceConnector, times(2))
                .addItemDelivered(reported.capture(), anyInt(), anyInt(), anyShort());
        assertThat("both displaced versions reported as superseded",
                reported.getAllValues().stream().map(ItemDeliveryResult::status).toList(),
                is(List.of(ItemDeliveryResult.Status.SUPERSEDED, ItemDeliveryResult.Status.SUPERSEDED)));
    }

    /* Record keys are compared for equality, so one key being another with more appended is
       simply a different record. */
    @Test
    void recordKeyThatIsAnotherWithMoreAppended_doesNotSupersedeIt() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message("870970:123-456", Priority.NORMAL, JOB_ID + 1, (short) 0),
                successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(
                message("870970:123"), successItem(addiRecordX.getBytes()));

        assertThat("result is deferred", result, is(nullValue()));
        assertThat("both staged", batchCount(), is(2L));
    }

    @Test
    void itemWithoutARecordKey_isStagedUnconditionally() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(null, Priority.NORMAL, JOB_ID + 1, (short) 0),
                successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(message(null), successItem(addiRecordX.getBytes()));

        assertThat("nothing supersedes an item with no record identity", result, is(nullValue()));
        assertThat("both staged", batchCount(), is(2L));
    }

    @Test
    void abortJob_deletesOnlyTheBatchesOfThatJob() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));
        consumer.deliverItem(message("870970:other", Priority.NORMAL, JOB_ID + 1, (short) 1),
                successItem(addiRecordY.getBytes()));

        consumer.abortJob(JOB_ID);

        assertThat("the other job's batch remains", batchCount(), is(1L));
        assertThat("and it is the one that was kept", onlyBatch().getName(), is("15-870970:other-43-0-1"));
    }

    @Test
    void abortJob_deletesTheItemsThatJobWasHolding() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID - 1, (short) 0),
                successItem(addiRecordX.getBytes()));
        consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        consumer.abortJob(JOB_ID);

        assertThat("nothing held for the aborted job", heldItems().size(), is(0));
        assertThat("the other job's batch remains", batchCount(), is(1L));
    }

    /* Discarding it with the batch it waited behind would leave its own job unable to finish,
       since nothing else would ever release it. */
    @Test
    void abortJob_stagesAnotherJobsItemItWasHoldingBack() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID + 1, ITEM_ID),
                successItem(addiRecordX.getBytes()));

        consumer.abortJob(JOB_ID);

        assertThat("nothing left held", heldItems().size(), is(0));
        assertThat("the released version is staged", batchCount(), is(1L));
        assertThat("and it is the one that was held", onlyBatch().getName(), is("15-870970:12345678-43-0-7"));
    }

    /* A record key containing the aborted job's id between hyphens is still a different job's
       work, and deleting it would destroy work that job is waiting on. */
    @Test
    void abortJob_leavesABatchWhoseRecordKeyMerelyLooksLikeTheJobId() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message("870970:x-42-y", Priority.NORMAL, JOB_ID + 1, (short) 1),
                successItem(addiRecordX.getBytes()));

        consumer.abortJob(JOB_ID);

        assertThat("batch kept", batchCount(), is(1L));
    }

    /* Discarding a batch without the record lets a consumer read that batch on its way out
       and hold its item behind it, and the item then waits for a batch that is gone. */
    @Test
    void abortJob_waitsForWhoeverElseHoldsTheRecord() throws Exception {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));
        EntityManager holder = entityManagerFactory.createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            holder.getTransaction().begin();
            RecordLock.acquire(holder, SINK_ID, RECORD_KEY);

            Future<?> aborting = executor.submit(() -> consumer.abortJob(JOB_ID));

            assertThrows(TimeoutException.class, () -> aborting.get(2, TimeUnit.SECONDS));
            assertThat("nothing discarded while the record is held", batchCount(), is(1L));

            holder.getTransaction().commit();

            aborting.get(20, TimeUnit.SECONDS);
            assertThat("discarded once the record is released", batchCount(), is(0L));
        } finally {
            executor.shutdownNow();
            if (holder.getTransaction().isActive()) {
                holder.getTransaction().rollback();
            }
            holder.close();
        }
    }

    /* The check before staging is what keeps a record to one version in flight, and it is a
       check rather than an invariant unless the database refuses the second row. */
    @Test
    void twoVersionsOfOneRecordCannotBeStagedAtOnce() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery(
                            "INSERT INTO batch (name) VALUES ('a'), ('b')").executeUpdate();
            entityManager.createNativeQuery("INSERT INTO staged_item"
                            + " (batch, sink_id, record_key, job_id, chunk_id, item_id)"
                            + " VALUES (1, 15, '870970:1', 1, 0, 0)").executeUpdate();
            assertThrows(RuntimeException.class, () -> entityManager.createNativeQuery("INSERT INTO staged_item"
                    + " (batch, sink_id, record_key, job_id, chunk_id, item_id)"
                    + " VALUES (2, 15, '870970:1', 2, 0, 0)").executeUpdate());
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    /* Two items without a record key have no record identity to share, so neither can block
       the other. Null record keys compare as distinct, which is what allows that. */
    @Test
    void itemsWithoutARecordKey_doNotBlockEachOther() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(null, Priority.NORMAL, JOB_ID, (short) 0),
                successItem(addiRecordX.getBytes()));
        consumer.deliverItem(message(null, Priority.NORMAL, JOB_ID, (short) 1),
                successItem(addiRecordX.getBytes()));
        consumer.deliverItem(message(null, Priority.NORMAL, JOB_ID, (short) 2),
                successItem(addiRecordX.getBytes()));

        assertThat("all staged", batchCount(), is(3L));
        assertThat("none held", heldItems().size(), is(0));
    }

    /* Grouping orders the consumers of one record against each other and says nothing about
       the finalizer, which is a timer. Both decide on what they read and then act, so both have
       to take the record first. */
    @Test
    void stagingWaitsForWhoeverElseHoldsTheRecord() throws Exception {
        EntityManager holder = entityManagerFactory.createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            holder.getTransaction().begin();
            RecordLock.acquire(holder, SINK_ID, RECORD_KEY);

            Future<ItemDeliveryResult> staging = executor.submit(() ->
                    consumer().deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes())));

            assertThrows(TimeoutException.class, () -> staging.get(2, TimeUnit.SECONDS));
            assertThat("nothing staged while the record is held", batchCount(), is(0L));

            holder.getTransaction().commit();

            assertThat("result once the record is released", staging.get(20, TimeUnit.SECONDS), is(nullValue()));
            assertThat("staged once the record is released", batchCount(), is(1L));
        } finally {
            executor.shutdownNow();
            if (holder.getTransaction().isActive()) {
                holder.getTransaction().rollback();
            }
            holder.close();
        }
    }

    /* The framework counts the deliveries this sink decides itself, BatchFinalizer counts the
       ones it defers. Both must name the same destination or one sink's deliveries split
       across two series, neither matching what every other sink reports. */
    @Test
    void theFinalizersMetricDestinationMatchesTheFrameworks() {
        assertThat(BatchExchangeMessageConsumer.fqn(), is(consumer().getFQN()));
    }

    private BatchExchangeMessageConsumer consumer() {
        return new BatchExchangeMessageConsumer(
                new ServiceHub.Builder()
                        .withJobStoreServiceConnector(jobStoreServiceConnector)
                        .test(),
                entityManagerFactory);
    }

    private ChunkItem successItem(byte[] data) {
        return new ChunkItemBuilder()
                .setId(ITEM_ID)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(data)
                .setTrackingId("tracking")
                .build();
    }

    private ConsumedMessage message(String recordKey) {
        return message(recordKey, Priority.NORMAL);
    }

    private ConsumedMessage message(String recordKey, Priority priority) {
        return message(recordKey, priority, JOB_ID, ITEM_ID);
    }

    private ConsumedMessage message(String recordKey, Priority priority, int jobId, short itemId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.jobId.name, jobId);
        headers.put(JMSHeader.chunkId.name, CHUNK_ID);
        headers.put(JMSHeader.itemId.name, itemId);
        if (recordKey != null) {
            headers.put(JMSHeader.recordKey.name, recordKey);
        }
        return new ConsumedMessage("id", headers, "", priority);
    }

    private Batch onlyBatch() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return (Batch) entityManager
                    .createNativeQuery("SELECT * FROM batch ORDER BY id", Batch.class)
                    .getResultList()
                    .get(0);
        } finally {
            entityManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    private List<BatchEntry> entriesOf(Batch batch) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return (List<BatchEntry>) entityManager
                    .createNamedQuery(BatchEntry.GET_BATCH_ENTRIES_QUERY_NAME)
                    .setParameter(1, batch.getId())
                    .setHint("eclipselink.refresh", true)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }

    private List<HeldItem> heldItems() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return entityManager
                    .createQuery("select d from HeldItem d order by d.id", HeldItem.class)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }

    private long batchCount() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return ((Number) entityManager
                    .createNativeQuery("SELECT count(*) FROM batch")
                    .getSingleResult()).longValue();
        } finally {
            entityManager.close();
        }
    }
}
