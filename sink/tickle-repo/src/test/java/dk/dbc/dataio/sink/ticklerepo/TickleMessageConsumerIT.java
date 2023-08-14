package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.testutil.ObjectFactory;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageConsumerIT extends IntegrationTest {
    Logger LOGGER = LoggerFactory.getLogger(MessageConsumerIT.class);
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
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

    /*  When: handling valid chunk items referencing non-existing dataset
     *  Then: dataset is created as dictated by chunk item tickle attributes
     */
    @Test
    public void datasetCreated() throws InvalidMessageException {
        ConsumedMessage message = ObjectFactory.createConsumedMessage(createChunk());
        MessageConsumer messageConsumer = createMessageConsumerBean();

        messageConsumer.handleConsumedMessage(message);

        assertThat(messageConsumer.tickleRepo.lookupDataSet(new DataSet()
                        .withName(tickleAttributes1.getDatasetName()))
                .isPresent(), is(true));
    }

    /*  When: handling valid chunk items referencing existing dataset
     *  Then: no dataset is created
     */
    @Test
    public void datasetExists() throws InvalidMessageException {
        executeScriptResource("/ticklerepo-existing-dataset.sql");

        ConsumedMessage message = ObjectFactory.createConsumedMessage(createChunk());
        MessageConsumer messageConsumer = createMessageConsumerBean();

        messageConsumer.handleConsumedMessage(message);

        Batch batch = messageConsumer.tickleRepo.lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("existing dataset used", batch.getDataset(), is(1));
    }

    /*  When: handling first chunk from a never before seen job
     *  Then: a new batch of default type INCREMENTAL is created
     *   And: the created batch is cached in the consumer
     *   And: the created batch has the job specification as metadata
     */
    @Test
    public void batchCreated() throws JobStoreServiceConnectorException, JSONBException, InvalidMessageException {
        Chunk chunk = createChunk();
        ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        MessageConsumer messageConsumer = createMessageConsumerBean();

        JobSpecification jobSpecification = new JobSpecification()
                .withDataFile("testFile");
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withSpecification(jobSpecification);
        when(jobStoreServiceConnector.listJobs("job:id = " + chunk.getJobId()))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));

        messageConsumer.handleConsumedMessage(message);

        Batch batch = messageConsumer.tickleRepo.lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch type", batch.getType(), is(Batch.Type.INCREMENTAL));
        assertThat("job ID in cache", messageConsumer.batchCache.asMap().containsKey(chunk.getJobId()), is(true));
        assertThat("cached batch", messageConsumer.batchCache.getIfPresent(chunk.getJobId()).getId(), is(batch.getId()));
        assertThat("batch metadata", jsonbContext.unmarshall(batch.getMetadata(), JobSpecification.class), is(jobSpecification));
    }

    /*  When: handling first chunk from a never before seen job
     *   And: environment specifies tickle TOTAL behaviour
     *  Then: a new batch of type TOTAL is created
     *   And: the created batch is cached in the consumer
     */
    @Test
    public void totalBatchCreated() throws InvalidMessageException {
        environmentVariables.set("TICKLE_BEHAVIOUR", "total");

        Chunk chunk = createChunk();
        ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        MessageConsumer messageConsumer = createMessageConsumerBean();

        messageConsumer.handleConsumedMessage(message);

        Batch batch = messageConsumer.tickleRepo.lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch type", batch.getType(), is(Batch.Type.TOTAL));
        assertThat("job ID in cache", messageConsumer.batchCache.asMap().containsKey(chunk.getJobId()), is(true));
        assertThat("cached batch", messageConsumer.batchCache.getIfPresent(chunk.getJobId()).getId(), is(batch.getId()));
    }

    /*  When: handling subsequent chunks from a known job
     *  Then: the existing batch is used
     *   And: the batch is cached in the consumer
     */
    @Test
    public void batchExists() throws InvalidMessageException {
        Chunk chunk = createChunk();
        ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        MessageConsumer messageConsumer = createMessageConsumerBean();

        messageConsumer.handleConsumedMessage(message);

        messageConsumer.batchCache.invalidateAll();

        messageConsumer.handleConsumedMessage(message);

        assertThat("job ID in cache", messageConsumer.batchCache.asMap().containsKey(chunk.getJobId()), is(true));
        assertThat("cached batch", messageConsumer.batchCache.getIfPresent(chunk.getJobId()).getId(), is(1));
    }

    /*  When: handling chunk containing no successful chunk items
     *  Then: the tickle repo is not updated
     */
    @Test
    public void chunkContainsNoItemsForTickle() throws JobStoreServiceConnectorException, InvalidMessageException {
        Chunk chunk = createIgnoredChunk();
        ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        MessageConsumer messageConsumer = createMessageConsumerBean();

        messageConsumer.handleConsumedMessage(message);

        assertThat("dataset created", messageConsumer.tickleRepo.lookupDataSet(new DataSet().withId(1)).isPresent(), is(false));
        assertThat("batch created", messageConsumer.tickleRepo.lookupBatch(new Batch().withId(1)).isPresent(), is(false));
        assertThat("job ID cached", messageConsumer.batchCache.asMap().containsKey(chunk.getJobId()), is(false));

        ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), eq(chunk.getJobId()), eq(chunk.getChunkId()));

        Chunk result = chunkArgumentCaptor.getValue();
        assertThat("chunk size", result.size(), is(1));
    }

    /*  Given: an empty tickle repository
     *   When: handling chunk containing successful chunk items
     *   Then: the tickle repo is updated with new records
     */
    @Test
    public void recordsCreated() throws InvalidMessageException {
        Chunk chunk = createChunk();
        ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        MessageConsumer messageConsumer = createMessageConsumerBean();

        messageConsumer.handleConsumedMessage(message);

        TickleRepo.ResultSet<Record> rs = persistenceContext.run(() ->
                messageConsumer.tickleRepo.getRecordsInBatch(messageConsumer.batchCache.getIfPresent(chunk.getJobId())));

        Iterator<Record> recordIterator = rs.iterator();

        Record record = recordIterator.next();
        assertThat("1st record local ID", record.getLocalId(), is(tickleAttributes1.getBibliographicRecordId()));
        assertThat("1st record status", record.getStatus(), is(Record.Status.ACTIVE));
        assertThat("1st record checksum", record.getChecksum(), is(tickleAttributes1.getCompareRecord()));
        assertThat("1st record content", StringUtil.asString(record.getContent()),
                is(StringUtil.asString(toAddiRecord(chunk.getItems().get(2).getData()).getContentData())));

        record = recordIterator.next();
        assertThat("2nd record local ID", record.getLocalId(), is(tickleAttributes2.getBibliographicRecordId()));
        assertThat("2nd record status", record.getStatus(), is(Record.Status.DELETED));
        assertThat("2nd record checksum", record.getChecksum(), is(tickleAttributes2.getCompareRecord()));
        assertThat("2nd record content", StringUtil.asString(record.getContent(), StandardCharsets.UTF_8),
                is(StringUtil.asString(toAddiRecord(chunk.getItems().get(4).getData()).getContentData(), StandardCharsets.ISO_8859_1)));

        assertThat("number of records created is 2", recordIterator.hasNext(), is(false));
    }

    /*  Given: a tickle repository containing records
     *   When: handling chunk containing successful chunk items for existing records
     *   Then: the tickle repo is updated only when record checksum indicates a change
     */
    @Test
    public void checksum() throws InvalidMessageException {
        executeScriptResource("/ticklerepo-existing-records.sql");

        Chunk chunk = createChunk();
        ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        MessageConsumer messageConsumer = createMessageConsumerBean();

         messageConsumer.handleConsumedMessage(message);

        Batch batch = messageConsumer.batchCache.getIfPresent(chunk.getJobId());

        Record notUpdated = messageConsumer.tickleRepo.lookupRecord(new Record().withId(1)).orElse(null);
        assertThat("record not updated batch", notUpdated.getBatch(), is(not(batch.getId())));
        assertThat("record not updated status", notUpdated.getStatus(), is(Record.Status.ACTIVE));
        assertThat("record not updated tracking ID", notUpdated.getTrackingId(), is("t1"));
        assertThat("record not updated checksum", notUpdated.getChecksum(), is("chksum1"));
        assertThat("record not updated content", StringUtil.asString(notUpdated.getContent()),
                is(StringUtil.asString(toAddiRecord(chunk.getItems().get(2).getData()).getContentData())));

        Record updated = messageConsumer.tickleRepo.lookupRecord(new Record().withId(2)).orElse(null);
        assertThat("record updated batch", updated.getBatch(), is(batch.getId()));
        assertThat("record updated status", updated.getStatus(), is(Record.Status.DELETED));
        assertThat("record updated tracking ID", updated.getTrackingId(), is(chunk.getItems().get(4).getTrackingId()));
        assertThat("record updated checksum", updated.getChecksum(), is("chksum2"));
        assertThat("record updated content", StringUtil.asString(updated.getContent(), StandardCharsets.UTF_8),
                is(StringUtil.asString(toAddiRecord(chunk.getItems().get(4).getData()).getContentData(), StandardCharsets.ISO_8859_1)));
    }

    /*  When: handling chunk containing successful end-of-job chunk item
     *   Then: the tickle repo batch is closed
     */
    @Test
    public void closeBatch() throws InvalidMessageException {
        MessageConsumer messageConsumer = createMessageConsumerBean();
        Chunk chunk = createChunk();
        messageConsumer.handleConsumedMessage( ObjectFactory.createConsumedMessage(chunk));

        Chunk endJobChunk = createEndJobChunk();
        messageConsumer.handleConsumedMessage(ObjectFactory.createConsumedMessage(endJobChunk));

        Batch batch = messageConsumer.batchCache.getIfPresent(chunk.getJobId());

        assert batch != null;
        assertThat("Batch is closed", batch.getTimeOfCompletion(), is(notNullValue()));

    }

    /*   When: handling chunk containing failed end-of-job chunk item
     *   Then: the tickle repo batch is aborted
     */
    //@Test DISABLED for now and again
    public void abortBatch() throws InvalidMessageException {
        MessageConsumer messageConsumer = createMessageConsumerBean();

        Chunk chunk = createChunk();
        messageConsumer.handleConsumedMessage(ObjectFactory.createConsumedMessage(chunk));

        Chunk endJobChunk = createFailedEndJobChunk();
        messageConsumer.handleConsumedMessage(ObjectFactory.createConsumedMessage(endJobChunk));

        Batch batch = messageConsumer.batchCache.getIfPresent(chunk.getJobId());
        verify(messageConsumer.tickleRepo).abortBatch(batch);
        verify(messageConsumer.tickleRepo).closeBatch(batch);
    }

    @Test
    public void resultingChunk() throws JobStoreServiceConnectorException, InvalidMessageException {
        Chunk chunk = createChunk();
        ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        MessageConsumer messageConsumer = createMessageConsumerBean();

         messageConsumer.handleConsumedMessage(message);

        ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), eq(chunk.getJobId()), eq(chunk.getChunkId()));

        Chunk result = chunkArgumentCaptor.getValue();
        assertThat("chunk size", result.size(), is(5));

        ChunkItem item = result.getItems().get(0);
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("1st item type", item.getType(), is(List.of(ChunkItem.Type.STRING)));
        assertThat("1st item data", StringUtil.asString(item.getData()), is("Ignored by processor"));
        assertThat("1st item tracking ID", item.getTrackingId(), is(chunk.getItems().get(0).getTrackingId()));

        item = result.getItems().get(1);
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd item type", item.getType(), is(List.of(ChunkItem.Type.STRING)));
        assertThat("2nd item data", StringUtil.asString(item.getData()), is("Failed by processor"));
        assertThat("2nd item tracking ID", item.getTrackingId(), is(chunk.getItems().get(1).getTrackingId()));

        item = result.getItems().get(2);
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd item type", item.getType(), is(List.of(ChunkItem.Type.STRING)));
        assertThat("3rd item data contains ERROR", StringUtil.asString(item.getData()).contains("\tERROR\n"), is(true));
        assertThat("3rd item data contains OK", StringUtil.asString(item.getData()).contains("\tOK\n"), is(true));
        assertThat("3rd item tracking ID", item.getTrackingId(), is(chunk.getItems().get(2).getTrackingId()));
        assertThat("3rd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("3rd item 1st diagnostic level", item.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));

        item = result.getItems().get(3);
        assertThat("4th item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("4th item type", item.getType(), is(List.of(ChunkItem.Type.STRING)));
        assertThat("4th item data contains Exception", StringUtil.asString(item.getData()).contains("Exception"), is(true));
        assertThat("4th item tracking ID", item.getTrackingId(), is(chunk.getItems().get(3).getTrackingId()));
        assertThat("4th item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("4th item 1st diagnostic level", item.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));

        item = result.getItems().get(4);
        assertThat("5th item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th item type", item.getType(), is(List.of(ChunkItem.Type.STRING)));
        assertThat("5th item data contains OK", StringUtil.asString(item.getData()).contains("\tOK\n"), is(true));
        assertThat("5th item tracking ID", item.getTrackingId(), is(chunk.getItems().get(4).getTrackingId()));
    }

    private MessageConsumer createMessageConsumerBean() {
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build();
        MessageConsumer messageConsumer = new MessageConsumer(hub, entityManager);
        return messageConsumer;
    }

    private Chunk createChunk() {
        try {
            return new ChunkBuilder(Chunk.Type.PROCESSED)
                    .setItems(new ArrayList<>())
                    .appendItem(ChunkItem.ignoredChunkItem()
                            .withId(0)
                            .withType(ChunkItem.Type.STRING)
                            .withData("IGNORED")
                            .withTrackingId("ignored item"))
                    .appendItem(ChunkItem.failedChunkItem()
                            .withId(1)
                            .withType(ChunkItem.Type.STRING)
                            .withData("FAILED")
                            .withTrackingId("failed item"))
                    .appendItem(ChunkItem.successfulChunkItem()
                            .withId(2)
                            .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                            .withData(toBytes(
                                    new AddiRecord(
                                            jsonbContext.marshall(tickleAttributes1).getBytes(),
                                            "data1a æøå".getBytes()),
                                    new AddiRecord(
                                            jsonbContext.marshall(invalidTickleAttributes).getBytes(),
                                            "data1b".getBytes())))
                            .withTrackingId("multi addi"))
                    .appendItem(ChunkItem.successfulChunkItem()
                            .withId(3)
                            .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                            .withData(toBytes(
                                    new AddiRecord(
                                            "illegal json".getBytes(),
                                            "data2".getBytes())))
                            .withTrackingId("illegal addi metadata"))
                    .appendItem(ChunkItem.successfulChunkItem()
                            .withId(4)
                            .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                            .withEncoding(StandardCharsets.ISO_8859_1)
                            .withData(toBytes(
                                    new AddiRecord(
                                            jsonbContext.marshall(tickleAttributes2).getBytes(),
                                            "data2 æøå".getBytes(StandardCharsets.ISO_8859_1))))
                            .withTrackingId("single addi"))
                    .build();
        } catch (IOException | JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private Chunk createIgnoredChunk() {
        return new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(new ArrayList<>())
                .appendItem(ChunkItem.ignoredChunkItem()
                        .withId(0)
                        .withType(ChunkItem.Type.STRING)
                        .withData("IGNORED")
                        .withTrackingId("ignored item"))
                .build();
    }

    private Chunk createEndJobChunk() {
        return new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(new ArrayList<>())
                .appendItem(ChunkItem.successfulChunkItem()
                        .withId(0)
                        .withType(ChunkItem.Type.JOB_END)
                        .withData("END"))
                .build();
    }

    private Chunk createFailedEndJobChunk() {
        return new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(new ArrayList<>())
                .appendItem(ChunkItem.failedChunkItem()
                        .withId(0)
                        .withType(ChunkItem.Type.JOB_END)
                        .withData("END"))
                .build();
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
}
