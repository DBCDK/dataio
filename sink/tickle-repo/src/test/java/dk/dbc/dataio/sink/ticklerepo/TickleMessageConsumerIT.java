package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TickleMessageConsumerIT extends IntegrationTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 7;
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "123456:id1";
    // Written out rather than taken from the test's own TickleAttributes, since the sink reports the
    // instance unmarshalled from the addi metadata, where deleted has become false rather than null
    private static final String ORPHANED =
            "TickleAttributes{agencyId=123456, datasetName='null', bibliographicRecordId='orphaned', deleted=false}";
    private static final String ORPHANED_2 =
            "TickleAttributes{agencyId=123456, datasetName='null', bibliographicRecordId='orphaned2', deleted=false}";

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private final TickleAttributes invalidTickleAttributes = new TickleAttributes()
            .withAgencyId(123456)
            .withBibliographicRecordId("orphaned")
            .withCompareRecord("chksum");
    private final TickleAttributes tickleAttributes1 = new TickleAttributes()
            .withAgencyId(123456)
            .withBibliographicRecordId("id1")
            .withCompareRecord("chksum1")
            .withDatasetName("dataset1");
    private final TickleAttributes tickleAttributes2 = new TickleAttributes()
            .withAgencyId(123456)
            .withBibliographicRecordId("id2")
            .withCompareRecord("chksum2")
            .withDatasetName("dataset1")
            .withDeleted(true);

    @BeforeEach
    void clearCache() {
        TickleMessageConsumer.batchCache.invalidateAll();
    }

    /*  When: delivering a valid item referencing a non-existing dataset
     *  Then: the dataset is created as dictated by the item tickle attributes
     */
    @Test
    void datasetCreated() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        deliverAll(messageConsumer, items());

        assertThat(createTickleRepo().lookupDataSet(new DataSet().withName(tickleAttributes1.getDatasetName()))
                .isPresent(), is(true));
    }

    /*  When: delivering a valid item referencing an existing dataset
     *  Then: no dataset is created
     */
    @Test
    void datasetExists() {
        executeScriptResource("/ticklerepo-existing-dataset.sql");
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        deliverAll(messageConsumer, items());

        Batch batch = createTickleRepo().lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("existing dataset used", batch.getDataset(), is(1));
    }

    /*  When: delivering the first item of a never before seen job
     *  Then: a new batch of default type INCREMENTAL is created
     *   And: the created batch is cached in the consumer
     *   And: the created batch has the job specification as metadata
     */
    @Test
    void batchCreated() throws JobStoreServiceConnectorException, JSONBException {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();

        JobSpecification jobSpecification = new JobSpecification()
                .withDataFile("testFile");
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withSpecification(jobSpecification);
        when(jobStoreServiceConnector.listJobs("job:id = " + JOB_ID))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));

        deliverAll(messageConsumer, items());

        Batch batch = createTickleRepo().lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch type", batch.getType(), is(Batch.Type.INCREMENTAL));
        assertThat("job ID in cache", TickleMessageConsumer.batchCache.asMap().containsKey(JOB_ID), is(true));
        assertThat("cached batch", TickleMessageConsumer.batchCache.getIfPresent(JOB_ID).getId(), is(batch.getId()));
        assertThat("batch metadata", jsonbContext.unmarshall(batch.getMetadata(), JobSpecification.class), is(jobSpecification));
    }

    /*  When: delivering the first item of a never before seen job
     *   And: environment specifies tickle TOTAL behaviour
     *  Then: a new batch of type TOTAL is created
     *   And: the created batch is cached in the consumer
     */
    @Test
    void totalBatchCreated() {
        SinkConfig.TICKLE_BEHAVIOUR.setTestOverride("total");
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        deliverAll(messageConsumer, items());

        Batch batch = createTickleRepo().lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch type", batch.getType(), is(Batch.Type.TOTAL));
        assertThat("job ID in cache", TickleMessageConsumer.batchCache.asMap().containsKey(JOB_ID), is(true));
        assertThat("cached batch", TickleMessageConsumer.batchCache.getIfPresent(JOB_ID).getId(), is(batch.getId()));
        SinkConfig.TICKLE_BEHAVIOUR.clearAllTestOverrides();
    }

    /*  When: delivering subsequent items of a known job
     *  Then: the existing batch is used
     *   And: the batch is cached in the consumer
     */
    @Test
    void batchExists() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        deliverAll(messageConsumer, items());

        TickleMessageConsumer.batchCache.invalidateAll();

        deliverAll(messageConsumer, items());

        assertThat("job ID in cache", TickleMessageConsumer.batchCache.asMap().containsKey(JOB_ID), is(true));
        assertThat("cached batch", TickleMessageConsumer.batchCache.getIfPresent(JOB_ID).getId(), is(1));
        assertThat("no second batch created", createTickleRepo().lookupBatch(new Batch().withId(2)).isPresent(), is(false));
    }

    /*  When: delivering an item the processor ignored
     *  Then: the tickle repo is not touched at all
     *   And: the item is reported as ignored rather than delivered
     */
    @Test
    void ignoredItemCreatesNoBatch() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();

        ItemDeliveryResult result = deliver(messageConsumer, items().getFirst());

        TickleRepo tickleRepo = createTickleRepo();
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("dataset created", tickleRepo.lookupDataSet(new DataSet().withId(1)).isPresent(), is(false));
        assertThat("batch created", tickleRepo.lookupBatch(new Batch().withId(1)).isPresent(), is(false));
        assertThat("job ID cached", TickleMessageConsumer.batchCache.asMap().containsKey(JOB_ID), is(false));
    }

    /*  Given: an empty tickle repository
     *   When: delivering successful items
     *   Then: the tickle repo is updated with new records
     */
    @Test
    void recordsCreated() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        List<ChunkItem> items = items();
        deliverAll(messageConsumer, items);

        TickleRepo tickleRepo = createTickleRepo();
        tickleRepo.getEntityManager().getTransaction().begin();
        TickleRepo.ResultSet<Record> rs = tickleRepo.getRecordsInBatch(
                TickleMessageConsumer.batchCache.getIfPresent(JOB_ID));
        tickleRepo.getEntityManager().getTransaction().commit();
        Iterator<Record> recordIterator = rs.iterator();

        Record record = recordIterator.next();
        assertThat("1st record local ID", record.getLocalId(), is(tickleAttributes1.getBibliographicRecordId()));
        assertThat("1st record status", record.getStatus(), is(Record.Status.ACTIVE));
        assertThat("1st record checksum", record.getChecksum(), is(tickleAttributes1.getCompareRecord()));
        assertThat("1st record content", StringUtil.asString(record.getContent()),
                is(StringUtil.asString(toAddiRecord(items.get(2).getData()).getContentData())));

        record = recordIterator.next();
        assertThat("2nd record local ID", record.getLocalId(), is(tickleAttributes2.getBibliographicRecordId()));
        assertThat("2nd record status", record.getStatus(), is(Record.Status.DELETED));
        assertThat("2nd record checksum", record.getChecksum(), is(tickleAttributes2.getCompareRecord()));
        assertThat("2nd record content", StringUtil.asString(record.getContent(), StandardCharsets.UTF_8),
                is(StringUtil.asString(toAddiRecord(items.get(4).getData()).getContentData(), StandardCharsets.ISO_8859_1)));

        assertThat("number of records created is 2", recordIterator.hasNext(), is(false));
    }

    /*  Given: a tickle repository containing records
     *   When: delivering successful items for existing records
     *   Then: the tickle repo is updated only when record checksum indicates a change
     */
    @Test
    void checksum() {
        executeScriptResource("/ticklerepo-existing-records.sql");

        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        List<ChunkItem> items = items();
        deliverAll(messageConsumer, items);

        Batch batch = TickleMessageConsumer.batchCache.getIfPresent(JOB_ID);
        TickleRepo tickleRepo = createTickleRepo();

        Record notUpdated = tickleRepo.lookupRecord(new Record().withId(1)).orElse(null);
        assertThat("record not updated batch", notUpdated.getBatch(), is(not(batch.getId())));
        assertThat("record not updated status", notUpdated.getStatus(), is(Record.Status.ACTIVE));
        assertThat("record not updated tracking ID", notUpdated.getTrackingId(), is("t1"));
        assertThat("record not updated checksum", notUpdated.getChecksum(), is("chksum1"));
        assertThat("record not updated content", StringUtil.asString(notUpdated.getContent()),
                is(StringUtil.asString(toAddiRecord(items.get(2).getData()).getContentData())));

        Record updated = tickleRepo.lookupRecord(new Record().withId(2)).orElse(null);
        assertThat("record updated batch", updated.getBatch(), is(batch.getId()));
        assertThat("record updated status", updated.getStatus(), is(Record.Status.DELETED));
        assertThat("record updated tracking ID", updated.getTrackingId(), is(items.get(4).getTrackingId()));
        assertThat("record updated checksum", updated.getChecksum(), is("chksum2"));
        assertThat("record updated content", StringUtil.asString(updated.getContent(), StandardCharsets.UTF_8),
                is(StringUtil.asString(toAddiRecord(items.get(4).getData()).getContentData(), StandardCharsets.ISO_8859_1)));
    }

    /*  When: delivering a successful item
     *  Then: its records are committed by the time the delivery returns, without waiting for
     *        any further item of the job
     */
    @Test
    void recordsOfOneItemAreCommittedIndependently() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();

        ItemDeliveryResult result = deliver(messageConsumer, items().get(4));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        List<Record> committed = createTickleRepo().lookupRecords(1,
                List.of(tickleAttributes2.getBibliographicRecordId()));
        assertThat("record visible outside the delivering entity manager", committed.size(), is(1));
        assertThat("record tracking ID", committed.getFirst().getTrackingId(), is("single addi"));
    }

    /*  When: an item holds both a valid and an invalid tickle record
     *  Then: the valid record is committed and the item is reported as failed
     */
    @Test
    void itemWithOneInvalidRecordCommitsTheValidOneAndReportsFailed() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();

        ItemDeliveryResult result = deliver(messageConsumer, items().get(2));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome diagnostics", result.chunkItem().getDiagnostics().size(), is(1));
        assertThat("outcome diagnostic level", result.chunkItem().getDiagnostics().getFirst().getLevel(),
                is(Diagnostic.Level.FATAL));
        List<Record> committed = createTickleRepo().lookupRecords(1,
                List.of(tickleAttributes1.getBibliographicRecordId()));
        assertThat("valid record committed", committed.size(), is(1));
    }

    /*  When: an item holds two valid addi records for records not seen before
     *  Then: both are created, and the item report names each one in the order they were read
     */
    @Test
    void reportNamesEachRecordCreated() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        ChunkItem item = addiItem(0, "two new records",
                addiRecord(tickleAttributes1, "data1"),
                addiRecord(tickleAttributes2, "data2"));

        ItemDeliveryResult result = deliver(messageConsumer, item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("report", StringUtil.asString(result.chunkItem().getData()), is(
                """
                        Record 1: created tickle repo record with ID id1 in dataset 1
                        \tOK
                        Record 2: created tickle repo record with ID id2 in dataset 1
                        \tOK
                        """));
    }

    /*  Given: a tickle repository containing records
     *   When: an item holds one record whose checksum is unchanged and one whose checksum changed
     *   Then: the item report tells the two apart
     */
    @Test
    void reportNamesRecordUpdatedAndRecordUnchanged() {
        executeScriptResource("/ticklerepo-existing-records.sql");
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        ChunkItem item = addiItem(0, "one unchanged one updated",
                addiRecord(tickleAttributes1, "data1"),
                addiRecord(tickleAttributes2, "data2"));

        ItemDeliveryResult result = deliver(messageConsumer, item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("report", StringUtil.asString(result.chunkItem().getData()), is(
                """
                        Record 1: tickle repo record with ID id1 not updated in dataset 1 \
                        since checksum indicates no change
                        \tOK
                        Record 2: updated tickle repo record with ID id2 in dataset 1
                        \tOK
                        """));
    }

    /*  When: an item holds a record whose tickle attributes are incomplete
     *  Then: the report line for it is the diagnostic's own message, marked ERROR
     */
    @Test
    void reportNamesTheInvalidAttributes() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        ChunkItem item = addiItem(0, "invalid attributes", addiRecord(invalidTickleAttributes, "data"));

        ItemDeliveryResult result = deliver(messageConsumer, item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("report", StringUtil.asString(result.chunkItem().getData()), is(
                "Record 1: Invalid tickle attributes extracted from record " + ORPHANED + "\n\tERROR\n"));
        assertThat("diagnostic message", result.chunkItem().getDiagnostics().getFirst().getMessage(),
                is("Invalid tickle attributes extracted from record " + ORPHANED));
    }

    /*  When: an item holds two records whose tickle attributes are incomplete
     *  Then: the outcome carries one diagnostic per rejected record rather than only the last
     */
    @Test
    void itemWithTwoInvalidRecordsCarriesTwoDiagnostics() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        TickleAttributes secondInvalid = new TickleAttributes()
                .withAgencyId(123456)
                .withBibliographicRecordId("orphaned2")
                .withCompareRecord("chksum");
        ChunkItem item = addiItem(0, "two invalid records",
                addiRecord(invalidTickleAttributes, "data1"),
                addiRecord(secondInvalid, "data2"));

        ItemDeliveryResult result = deliver(messageConsumer, item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("diagnostics", result.chunkItem().getDiagnostics().size(), is(2));
        assertThat("1st diagnostic level", result.chunkItem().getDiagnostics().get(0).getLevel(),
                is(Diagnostic.Level.FATAL));
        assertThat("2nd diagnostic level", result.chunkItem().getDiagnostics().get(1).getLevel(),
                is(Diagnostic.Level.FATAL));
        assertThat("report", StringUtil.asString(result.chunkItem().getData()), is(
                "Record 1: Invalid tickle attributes extracted from record " + ORPHANED + "\n\tERROR\n" +
                "Record 2: Invalid tickle attributes extracted from record " + ORPHANED_2 + "\n\tERROR\n"));
        assertThat("no batch created", createTickleRepo().lookupBatch(new Batch().withId(1)).isPresent(), is(false));
    }

    /*  When: an item holds the same local ID twice
     *  Then: the record is created once and the second occurrence updates it, which is what the
     *        lookup map is mutated mid-item for
     */
    @Test
    void sameRecordIdTwiceInOneItemIsWrittenOnce() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        ChunkItem item = addiItem(0, "same id twice",
                addiRecord(tickleAttributes1, "first"),
                addiRecord(tickleAttributes1, "second"));

        ItemDeliveryResult result = deliver(messageConsumer, item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("report", StringUtil.asString(result.chunkItem().getData()), is(
                """
                        Record 1: created tickle repo record with ID id1 in dataset 1
                        \tOK
                        Record 2: updated tickle repo record with ID id1 in dataset 1
                        \tOK
                        """));
        List<Record> written = createTickleRepo().lookupRecords(1,
                List.of(tickleAttributes1.getBibliographicRecordId()));
        assertThat("one record written", written.size(), is(1));
        assertThat("content of the last occurrence", StringUtil.asString(written.getFirst().getContent()), is("second"));
    }

    /*  When: a successfully processed item holds nothing that can be read as an addi record
     *  Then: it is reported as ignored rather than delivered, so that no watermark is advanced
     *        for a record the tickle repo never saw
     *   And: no batch is created for it
     */
    @Test
    void itemWithNoReadableAddiRecordIsIgnored() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        ChunkItem item = ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                .withData("not an addi record")
                .withTrackingId("unreadable addi");

        ItemDeliveryResult result = deliver(messageConsumer, item);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("No addi records could be read from the item"));
        assertThat("batch created", createTickleRepo().lookupBatch(new Batch().withId(1)).isPresent(), is(false));
    }

    /*  When: writing the records of an item fails
     *  Then: the item is reported as failed and nothing it wrote is left behind
     */
    @Test
    void writeFailureIsReportedAsFailedAndLeavesNoRecords() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        TickleRepo failingRepo = mock(TickleRepo.class);
        when(failingRepo.getEntityManager()).thenReturn(entityManagerFactory.createEntityManager());
        when(failingRepo.lookupRecords(anyInt(), anyList())).thenThrow(new IllegalStateException("tickle repo is down"));

        ChunkItem item = items().get(4);
        ItemDeliveryResult result = messageConsumer.deliverItem(itemMessage(item), item, failingRepo);

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("no records written", createTickleRepo().lookupRecords(1,
                List.of(tickleAttributes2.getBibliographicRecordId())).isEmpty(), is(true));
    }

    /*  When: delivering a successful termination item
     *  Then: the tickle repo batch is closed
     */
    @Test
    void terminationItemClosesTheBatch() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        deliverAll(messageConsumer, items());

        ItemDeliveryResult result = deliver(messageConsumer, jobEndItem(ChunkItem.Status.SUCCESS));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Batch 1 closed"));
        Batch batch = createTickleRepo().lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch is closed", batch.getTimeOfCompletion(), is(notNullValue()));
    }

    /*  When: delivering a failed termination item
     *  Then: the tickle repo batch is aborted
     */
    @Test
    void failedTerminationItemAbortsTheBatch() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        deliverAll(messageConsumer, items());

        ItemDeliveryResult result = deliver(messageConsumer, jobEndItem(ChunkItem.Status.FAILURE));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("Batch 1 aborted"));
        Batch batch = createTickleRepo().lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch is completed", batch.getTimeOfCompletion(), is(notNullValue()));
    }

    /*  When: delivering a termination item for a job that never created a batch
     *  Then: nothing is finalized and the item is still reported as delivered
     */
    @Test
    void terminationItemForJobWithNoRecordsIsDelivered() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();

        ItemDeliveryResult result = deliver(messageConsumer, jobEndItem(ChunkItem.Status.SUCCESS));

        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()), is("OK"));
        assertThat("batch created", createTickleRepo().lookupBatch(new Batch().withId(1)).isPresent(), is(false));
    }

    /*  When: delivering the items of a chunk one at a time
     *  Then: each item is reported with the verdict its processing outcome earns, so that the
     *        delivering counters split exactly as they did when whole chunks were delivered
     */
    @Test
    void verdictPerItem() {
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();
        List<ChunkItem> items = items();

        ItemDeliveryResult result = deliver(messageConsumer, items.getFirst());
        assertThat("1st item verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("1st item outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("1st item outcome type", result.chunkItem().getType(), is(List.of(ChunkItem.Type.STRING)));
        assertThat("1st item outcome data", StringUtil.asString(result.chunkItem().getData()), is("Ignored by processor"));
        assertThat("1st item tracking ID", result.chunkItem().getTrackingId(), is(items.get(0).getTrackingId()));

        result = deliver(messageConsumer, items.get(1));
        assertThat("2nd item verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("2nd item outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd item outcome data", StringUtil.asString(result.chunkItem().getData()), is("Failed by processor"));
        assertThat("2nd item tracking ID", result.chunkItem().getTrackingId(), is(items.get(1).getTrackingId()));

        result = deliver(messageConsumer, items.get(2));
        assertThat("3rd item verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("3rd item outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd item outcome data contains ERROR", StringUtil.asString(result.chunkItem().getData()).contains("\tERROR\n"), is(true));
        assertThat("3rd item outcome data contains OK", StringUtil.asString(result.chunkItem().getData()).contains("\tOK\n"), is(true));
        assertThat("3rd item tracking ID", result.chunkItem().getTrackingId(), is(items.get(2).getTrackingId()));

        result = deliver(messageConsumer, items.get(3));
        assertThat("4th item verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("4th item outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("4th item outcome data contains ERROR", StringUtil.asString(result.chunkItem().getData()).contains("ERROR"), is(true));
        assertThat("4th item tracking ID", result.chunkItem().getTrackingId(), is(items.get(3).getTrackingId()));

        result = deliver(messageConsumer, items.get(4));
        assertThat("5th item verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("5th item outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th item outcome type", result.chunkItem().getType(), is(List.of(ChunkItem.Type.STRING)));
        assertThat("5th item outcome data contains OK", StringUtil.asString(result.chunkItem().getData()).contains("\tOK\n"), is(true));
        assertThat("5th item tracking ID", result.chunkItem().getTrackingId(), is(items.get(4).getTrackingId()));
    }

    /*  This sink keeps the delivery watermark rather than opting out of it, which is observable
     *  only as the lookup being made.
     */
    @Test
    void watermarkIsConsulted() throws InvalidMessageException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getWatermark(anyInt(), anyString())).thenReturn(Optional.<Watermark>empty());
        TickleMessageConsumer messageConsumer = createMessageConsumerBean();

        messageConsumer.handleConsumedMessage(itemMessage(items().get(4)));

        verify(jobStoreServiceConnector).getWatermark((int) SINK_ID, RECORD_KEY);
    }

    private TickleMessageConsumer createMessageConsumerBean() {
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build();
        return new TickleMessageConsumer(hub, entityManagerFactory);
    }

    private void deliverAll(TickleMessageConsumer messageConsumer, List<ChunkItem> items) {
        items.forEach(item -> deliver(messageConsumer, item));
    }

    private ItemDeliveryResult deliver(TickleMessageConsumer messageConsumer, ChunkItem item) {
        return messageConsumer.deliverItem(itemMessage(item), item);
    }

    private ConsumedMessage itemMessage(ChunkItem item) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.payload.name, JMSHeader.ITEM_PAYLOAD_TYPE);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, (long) CHUNK_ID);
        headers.put(JMSHeader.itemId.name, (short) item.getId());
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        try {
            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(item));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<ChunkItem> items() {
        try {
            return List.of(
                    ChunkItem.ignoredChunkItem()
                            .withId(0)
                            .withType(ChunkItem.Type.STRING)
                            .withData("IGNORED")
                            .withTrackingId("ignored item"),
                    ChunkItem.failedChunkItem()
                            .withId(1)
                            .withType(ChunkItem.Type.STRING)
                            .withData("FAILED")
                            .withTrackingId("failed item"),
                    ChunkItem.successfulChunkItem()
                            .withId(2)
                            .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                            .withData(toBytes(
                                    new AddiRecord(
                                            jsonbContext.marshall(tickleAttributes1).getBytes(),
                                            "data1a æøå".getBytes()),
                                    new AddiRecord(
                                            jsonbContext.marshall(invalidTickleAttributes).getBytes(),
                                            "data1b".getBytes())))
                            .withTrackingId("multi addi"),
                    ChunkItem.successfulChunkItem()
                            .withId(3)
                            .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                            .withData(toBytes(
                                    new AddiRecord(
                                            "illegal json".getBytes(),
                                            "data2".getBytes())))
                            .withTrackingId("illegal addi metadata"),
                    ChunkItem.successfulChunkItem()
                            .withId(4)
                            .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                            .withEncoding(StandardCharsets.ISO_8859_1)
                            .withData(toBytes(
                                    new AddiRecord(
                                            jsonbContext.marshall(tickleAttributes2).getBytes(),
                                            "data2 æøå".getBytes(StandardCharsets.ISO_8859_1))))
                            .withTrackingId("single addi"));
        } catch (IOException | JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private ChunkItem jobEndItem(ChunkItem.Status status) {
        return new ChunkItem()
                .withId(0)
                .withStatus(status)
                .withType(ChunkItem.Type.JOB_END)
                .withData("END")
                .withTrackingId("job end item");
    }

    private ChunkItem addiItem(long id, String trackingId, AddiRecord... addiRecords) {
        try {
            return ChunkItem.successfulChunkItem()
                    .withId(id)
                    .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                    .withData(toBytes(addiRecords))
                    .withTrackingId(trackingId);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private AddiRecord addiRecord(TickleAttributes tickleAttributes, String content) {
        try {
            return new AddiRecord(jsonbContext.marshall(tickleAttributes).getBytes(), content.getBytes());
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] toBytes(AddiRecord... addiRecords) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (AddiRecord addiRecord : addiRecords) {
            out.write(addiRecord.getBytes());
        }
        return out.toByteArray();
    }

    private AddiRecord toAddiRecord(byte[] addiRecordBytes) {
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(addiRecordBytes));
        try {
            return addiReader.next();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private TickleRepo createTickleRepo() {
        return new TickleRepo(entityManagerFactory.createEntityManager());
    }
}
