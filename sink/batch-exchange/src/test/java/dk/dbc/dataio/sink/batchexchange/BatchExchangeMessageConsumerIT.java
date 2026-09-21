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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

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

    @Test
    void olderVersionOfTheRecordAlreadyStaged_itemIsStaged() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message(RECORD_KEY, Priority.NORMAL, JOB_ID - 1, (short) 0),
                successItem(addiRecordX.getBytes()));

        ItemDeliveryResult result = consumer.deliverItem(message(RECORD_KEY), successItem(addiRecordX.getBytes()));

        assertThat("result is deferred", result, is(nullValue()));
        assertThat("both versions staged", batchCount(), is(2L));
    }

    /* The staged names are matched on a literal prefix, and one record key can be another with
       a hyphen and more appended. Only the parsed key decides. */
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

    /* A record key containing the aborted job's id between hyphens matches the same pattern,
       and deleting it would destroy work that job is still waiting on. */
    @Test
    void abortJob_leavesABatchWhoseRecordKeyMerelyLooksLikeTheJobId() {
        BatchExchangeMessageConsumer consumer = consumer();
        consumer.deliverItem(message("870970:x-42-y", Priority.NORMAL, JOB_ID + 1, (short) 1),
                successItem(addiRecordX.getBytes()));

        consumer.abortJob(JOB_ID);

        assertThat("batch kept", batchCount(), is(1L));
    }

    /* The lookup before staging runs once per delivered item, so it has to be a range scan
       over this index rather than a scan of the batch table. */
    @Test
    void theSinkOwnedMigrationCreatesTheIndexTheStagedLookupNeeds() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            @SuppressWarnings("unchecked") List<String> definitions = entityManager
                    .createNativeQuery("SELECT indexdef FROM pg_indexes WHERE indexname = 'batch_name_pattern_index'")
                    .getResultList();
            assertThat("index exists", definitions.size(), is(1));
            assertThat("index serves prefix matching", definitions.get(0).contains("text_pattern_ops"), is(true));
        } finally {
            entityManager.close();
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
                        .withJobStoreServiceConnector(mock(JobStoreServiceConnector.class))
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
