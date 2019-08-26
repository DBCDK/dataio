/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.testutil.ObjectFactory;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageConsumerBeanIT extends IntegrationTest {
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
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

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    /*  When: handling valid chunk items referencing non-existing dataset
     *  Then: dataset is created as dictated by chunk item tickle attributes
     */
    @Test
    public void datasetCreated() {
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(createChunk());
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        assertThat(messageConsumerBean.tickleRepo.lookupDataSet(new DataSet()
                .withName(tickleAttributes1.getDatasetName()))
                .isPresent(), is(true));
    }

    /*  When: handling valid chunk items referencing existing dataset
     *  Then: no dataset is created
     */
    @Test
    public void datasetExists() {
        executeScriptResource("/ticklerepo-existing-dataset.sql");

        final ConsumedMessage message = ObjectFactory.createConsumedMessage(createChunk());
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        final Batch batch = messageConsumerBean.tickleRepo.lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("existing dataset used", batch.getDataset(), is(1));
    }

    /*  When: handling first chunk from a never before seen job
     *  Then: a new batch of default type INCREMENTAL is created
     *   And: the created batch is cached in the consumer
     *   And: the created batch has the job specification as metadata
     */
    @Test
    public void batchCreated() throws JobStoreServiceConnectorException, JSONBException {
        final Chunk chunk = createChunk();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        final JobSpecification jobSpecification = new JobSpecification()
                .withDataFile("testFile");
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withSpecification(jobSpecification);
        when(jobStoreServiceConnector.listJobs("job:id = " + chunk.getJobId()))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        final Batch batch = messageConsumerBean.tickleRepo.lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch type", batch.getType(), is(Batch.Type.INCREMENTAL));
        assertThat("job ID in cache", messageConsumerBean.batchCache.containsKey(chunk.getJobId()), is(true));
        assertThat("cached batch", messageConsumerBean.batchCache.get(chunk.getJobId()).getId(), is(batch.getId()));
        assertThat("batch metadata",
                jsonbContext.unmarshall(batch.getMetadata(), JobSpecification.class),
                is(jobSpecification));
    }

    /*  When: handling first chunk from a never before seen job
     *   And: environment specifies tickle TOTAL behaviour
     *  Then: a new batch of type TOTAL is created
     *   And: the created batch is cached in the consumer
     */
    @Test
    public void totalBatchCreated() {
        environmentVariables.set("TICKLE_BEHAVIOUR", "total");

        final Chunk chunk = createChunk();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        final Batch batch = messageConsumerBean.tickleRepo.lookupBatch(new Batch().withId(1)).orElse(null);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch type", batch.getType(), is(Batch.Type.TOTAL));
        assertThat("job ID in cache", messageConsumerBean.batchCache.containsKey(chunk.getJobId()), is(true));
        assertThat("cached batch", messageConsumerBean.batchCache.get(chunk.getJobId()).getId(), is(batch.getId()));
    }

    /*  When: handling subsequent chunks from a known job
     *  Then: the existing batch is used
     *   And: the batch is cached in the consumer
     */
    @Test
    public void batchExists() {
        final Chunk chunk = createChunk();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        messageConsumerBean.batchCache.clear();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        assertThat("job ID in cache", messageConsumerBean.batchCache.containsKey(chunk.getJobId()), is(true));
        assertThat("cached batch", messageConsumerBean.batchCache.get(chunk.getJobId()).getId(), is(1));
    }

    /*  When: handling chunk containing no successful chunk items
     *  Then: the tickle repo is not updated
     */
    @Test
    public void chunkContainsNoItemsForTickle() throws JobStoreServiceConnectorException {
        final Chunk chunk = createIgnoredChunk();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        assertThat("dataset created", messageConsumerBean.tickleRepo.lookupDataSet(new DataSet().withId(1)).isPresent(), is(false));
        assertThat("batch created", messageConsumerBean.tickleRepo.lookupBatch(new Batch().withId(1)).isPresent(), is(false));
        assertThat("job ID cached", messageConsumerBean.batchCache.containsKey(chunk.getJobId()), is(false));

        final ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), eq(chunk.getJobId()), eq(chunk.getChunkId()));

        final Chunk result = chunkArgumentCaptor.getValue();
        assertThat("chunk size", result.size(), is(1));
    }

    /*  Given: an empty tickle repository
     *   When: handling chunk containing successful chunk items
     *   Then: the tickle repo is updated with new records
     */
    @Test
    public void recordsCreated() {
        final Chunk chunk = createChunk();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        final TickleRepo.ResultSet<Record> rs = persistenceContext.run(() ->
                messageConsumerBean.tickleRepo.getRecordsInBatch(
                        messageConsumerBean.batchCache.get(chunk.getJobId())));

        final Iterator<Record> recordIterator = rs.iterator();

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
    public void checksum() {
        executeScriptResource("/ticklerepo-existing-records.sql");

        final Chunk chunk = createChunk();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        final Batch batch = messageConsumerBean.batchCache.get(chunk.getJobId());

        final Record notUpdated = messageConsumerBean.tickleRepo.lookupRecord(new Record().withId(1)).orElse(null);
        assertThat("record not updated batch", notUpdated.getBatch(), is(not(batch.getId())));
        assertThat("record not updated status", notUpdated.getStatus(), is(Record.Status.ACTIVE));
        assertThat("record not updated tracking ID", notUpdated.getTrackingId(), is("t1"));
        assertThat("record not updated checksum", notUpdated.getChecksum(), is("chksum1"));
        assertThat("record not updated content", StringUtil.asString(notUpdated.getContent()),
                is(StringUtil.asString(toAddiRecord(chunk.getItems().get(2).getData()).getContentData())));

        final Record updated = messageConsumerBean.tickleRepo.lookupRecord(new Record().withId(2)).orElse(null);
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
    public void closeBatch() {
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        final Chunk chunk = createChunk();
        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(
                ObjectFactory.createConsumedMessage(chunk)));

        final Chunk endJobChunk = createEndJobChunk();
        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(
                ObjectFactory.createConsumedMessage(endJobChunk)));

        final Batch batch = messageConsumerBean.batchCache.get(chunk.getJobId());
        verify(messageConsumerBean.tickleRepo).closeBatch(batch);
    }

    /*   When: handling chunk containing failed end-of-job chunk item
     *   Then: the tickle repo batch is aborted
     */
    @Test
    public void abortBatch() {
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        final Chunk chunk = createChunk();
        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(
                ObjectFactory.createConsumedMessage(chunk)));

        final Chunk endJobChunk = createFailedEndJobChunk();
        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(
                ObjectFactory.createConsumedMessage(endJobChunk)));

        final Batch batch = messageConsumerBean.batchCache.get(chunk.getJobId());
        verify(messageConsumerBean.tickleRepo).abortBatch(batch);
        verify(messageConsumerBean.tickleRepo).closeBatch(batch);
    }

    @Test
    public void resultingChunk() throws JobStoreServiceConnectorException {
        final Chunk chunk = createChunk();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
        final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();

        persistenceContext.run(() -> messageConsumerBean.handleConsumedMessage(message));

        final ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), eq(chunk.getJobId()), eq(chunk.getChunkId()));

        final Chunk result = chunkArgumentCaptor.getValue();
        assertThat("chunk size", result.size(), is(5));

        ChunkItem item = result.getItems().get(0);
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("1st item type", item.getType(), is(Arrays.asList(ChunkItem.Type.STRING)));
        assertThat("1st item data", StringUtil.asString(item.getData()), is("Ignored by processor"));
        assertThat("1st item tracking ID", item.getTrackingId(), is(chunk.getItems().get(0).getTrackingId()));

        item = result.getItems().get(1);
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd item type", item.getType(), is(Arrays.asList(ChunkItem.Type.STRING)));
        assertThat("2nd item data", StringUtil.asString(item.getData()), is("Failed by processor"));
        assertThat("2nd item tracking ID", item.getTrackingId(), is(chunk.getItems().get(1).getTrackingId()));

        item = result.getItems().get(2);
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd item type", item.getType(), is(Arrays.asList(ChunkItem.Type.STRING)));
        assertThat("3rd item data contains ERROR", StringUtil.asString(item.getData()).contains("\tERROR\n"), is(true));
        assertThat("3rd item data contains OK", StringUtil.asString(item.getData()).contains("\tOK\n"), is(true));
        assertThat("3rd item tracking ID", item.getTrackingId(), is(chunk.getItems().get(2).getTrackingId()));
        assertThat("3rd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("3rd item 1st diagnostic level", item.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));

        item = result.getItems().get(3);
        assertThat("4th item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("4th item type", item.getType(), is(Arrays.asList(ChunkItem.Type.STRING)));
        assertThat("4th item data contains Exception", StringUtil.asString(item.getData()).contains("Exception"), is(true));
        assertThat("4th item tracking ID", item.getTrackingId(), is(chunk.getItems().get(3).getTrackingId()));
        assertThat("4th item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("4th item 1st diagnostic level", item.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));

        item = result.getItems().get(4);
        assertThat("5th item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th item type", item.getType(), is(Arrays.asList(ChunkItem.Type.STRING)));
        assertThat("5th item data contains OK", StringUtil.asString(item.getData()).contains("\tOK\n"), is(true));
        assertThat("5th item tracking ID", item.getTrackingId(), is(chunk.getItems().get(4).getTrackingId()));
    }

    private MessageConsumerBean createMessageConsumerBean() {
        final MessageConsumerBean bean = new MessageConsumerBean();
        bean.tickleRepo = spy(new TickleRepo(entityManager));
        bean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        bean.setTickleBehaviour();
        return bean;
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
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (AddiRecord addiRecord : addiRecords) {
            out.write(addiRecord.getBytes());
        }
        return out.toByteArray();
    }

    private AddiRecord toAddiRecord(byte[] addiRecordBytes) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(addiRecordBytes));
        try {
            return addiReader.next();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}