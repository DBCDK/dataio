package dk.dbc.dataio.sink.ticklerepo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);
    private final Batch.Type tickleBehaviour = Batch.Type.valueOf(SinkConfig.TICKLE_BEHAVIOUR.asString());
    final Cache<Integer, Batch> batchCache = CacheBuilder.newBuilder().maximumSize(50).expireAfterAccess(Duration.ofHours(1)).build();
    final TickleRepo tickleRepo;
    private final EntityManager entityManager;


    public MessageConsumer(ServiceHub serviceHub, EntityManager entityManager) {
        super(serviceHub);
        this.entityManager = entityManager;
        this.tickleRepo = new TickleRepo(entityManager);
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Batch batch = getBatch(chunk);
            Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
            if (chunk.isTerminationChunk()) {
                try {
                    // Give the before-last message enough time to commit
                    // its records to the tickle-repo before initiating
                    // the finalization process.
                    // (The result is uploaded to the job-store before the
                    // implicit commit, so without the sleep pause, there was a
                    // small risk that the end-chunk would reach this bean
                    // before all data was available.)
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                result.insertItem(handleJobEnd(chunk.getItems().get(0), batch));
            } else {
                chunk.getItems().forEach(chunkItem -> result.insertItem(handleChunkItem(chunkItem, batch)));
            }

            sendResultToJobStore(result);
            transaction.commit();
        } finally {
            if(transaction.isActive()) transaction.rollback();
        }
    }

    @Override
    public String getQueue() {
        return null;
    }

    @Override
    public String getAddress() {
        return null;
    }

    private Batch getBatch(Chunk chunk) {
        Batch batch = batchCache.getIfPresent(chunk.getJobId());
        if(batch != null) return batch;
        batch = tickleRepo.lookupBatch(new Batch().withBatchKey(chunk.getJobId())).orElse(createBatch(chunk));
        if(batch != null) batchCache.put(chunk.getJobId(), batch);
        return batch;
    }

    private Batch createBatch(Chunk chunk) {
        TickleAttributes tickleAttributes = findFirstTickleAttributes(chunk).orElse(null);
        if(tickleAttributes == null) return null;
        // find dataset or else create it
        DataSet searchValue = new DataSet()
                .withName(tickleAttributes.getDatasetName())
                .withAgencyId(tickleAttributes.getAgencyId());
        DataSet dataset = tickleRepo.lookupDataSet(searchValue)
                .orElseGet(() -> tickleRepo.createDataSet(searchValue));
        // create new batch and cache it
        return tickleRepo.createBatch(new Batch()
                .withBatchKey(chunk.getJobId())
                .withDataset(dataset.getId())
                .withType(tickleBehaviour)
                .withMetadata(getBatchMetadata(chunk.getJobId())));
    }

    /* Use job specification as batch metadata */
    private String getBatchMetadata(long jobId) {
        try {
            List<JobInfoSnapshot> jobInfoSnapshots = jobStoreServiceConnector.listJobs("job:id = " + jobId);
            if (!jobInfoSnapshots.isEmpty()) {
                JSONBContext jsonbContext = new JSONBContext();
                return jsonbContext.marshall(jobInfoSnapshots.get(0).getSpecification());
            }
        } catch (JobStoreServiceConnectorException | JSONBException e) {
            LOGGER.error("Unable to retrieve metadata for batch", e);
        }
        return null;
    }

    private Optional<TickleAttributes> findFirstTickleAttributes(Chunk chunk) {
        return chunk.getItems().stream()
                .filter(chunkItem -> chunkItem.getStatus() == ChunkItem.Status.SUCCESS)
                .map(ExpandedChunkItem::safeFrom)
                .flatMap(Collection::stream)
                .map(ExpandedChunkItem::getTickleAttributes)
                .filter(TickleAttributes::isValid)
                .findFirst();
    }

    private ChunkItem handleJobEnd(ChunkItem chunkItem, Batch batch) {
        ChunkItem result = ChunkItem.successfulChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8)
                .withData("OK");

        if (batch != null) {
            if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                tickleRepo.closeBatch(batch);
                result.withData(String.format("Batch %d closed", batch.getId()));
            } else {
                tickleRepo.abortBatch(batch);
                result.withData(String.format("Batch %d aborted", batch.getId()));
            }
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem, Batch batch) {
        try {
            DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());

            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    long handleChunkItemStartTime = System.currentTimeMillis();
                    ChunkItem item = putInTickleBatch(batch, chunkItem);
                    Metric.HANDLE_CHUNK_ITEM.simpleTimer().update(Duration.ofMillis(System.currentTimeMillis() - handleChunkItemStartTime));
                    return item;
                case FAILURE:
                    return ChunkItem.ignoredChunkItem()
                            .withId(chunkItem.getId())
                            .withTrackingId(chunkItem.getTrackingId())
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withType(ChunkItem.Type.STRING)
                            .withData("Failed by processor")
                            .withEncoding(StandardCharsets.UTF_8);
                case IGNORE:
                    return ChunkItem.ignoredChunkItem()
                            .withId(chunkItem.getId())
                            .withTrackingId(chunkItem.getTrackingId())
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withType(ChunkItem.Type.STRING)
                            .withData("Ignored by processor")
                            .withEncoding(StandardCharsets.UTF_8);
                default:
                    throw new IllegalStateException("Unhandled chunk item status " + chunkItem.getStatus());
            }
        } catch (Exception e) {
            Metric.CHUNK_ITEM_FAILURES.counter().inc();
            return ChunkItem.failedChunkItem()
                    .withId(chunkItem.getId())
                    .withTrackingId(chunkItem.getTrackingId())
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withType(ChunkItem.Type.STRING)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage())
                    .withEncoding(StandardCharsets.UTF_8);
        } finally {
            DBCTrackedLogContext.remove();
        }
    }

    private ChunkItem putInTickleBatch(Batch batch, ChunkItem chunkItem) throws IOException, JSONBException {
        ChunkItem result = new ChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);

        StringBuilder dataBuffer = new StringBuilder();
        final String dataPrintf = "Record %d: %s\n\t%s\n";
        int recordNo = 1;
        for (ExpandedChunkItem item : ExpandedChunkItem.from(chunkItem)) {
            TickleAttributes tickleAttributes = item.getTickleAttributes();
            if (!tickleAttributes.isValid()) {
                Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL,
                        "Invalid tickle attributes extracted from record " + tickleAttributes);
                result.withDiagnostics(diagnostic);
                dataBuffer.append(String.format(dataPrintf, recordNo, diagnostic.getMessage(), "ERROR"));
            } else {
                byte[] content = getContent(item);
                Record tickleRecord = new Record()
                        .withBatch(batch.getId())
                        .withDataset(batch.getDataset())
                        .withStatus(toStatus(tickleAttributes))
                        .withTrackingId(item.getTrackingId())
                        .withLocalId(tickleAttributes.getBibliographicRecordId())
                        .withContent(content)
                        .withChecksum(tickleAttributes.getCompareRecord());

                Optional<Record> lookupRecord = tickleRepo.lookupRecord(tickleRecord);
                if (lookupRecord.isPresent()) {
                    tickleRecord = lookupRecord.get()
                            .withContent(content)
                            .withStatus(toStatus(tickleAttributes));
                    tickleRecord.updateBatchIfModified(batch, tickleAttributes.getCompareRecord());
                    if (tickleRecord.getBatch() == batch.getId()) {
                        tickleRecord.withTrackingId(item.getTrackingId());
                        dataBuffer.append(String.format(dataPrintf, recordNo,
                                "updated tickle repo record with ID " + tickleRecord.getLocalId() +
                                        " in dataset " + tickleRecord.getDataset(), "OK"));
                    } else {
                        dataBuffer.append(String.format(dataPrintf, recordNo,
                                "tickle repo record with ID " + tickleRecord.getLocalId() +
                                        " not updated in dataset " + tickleRecord.getDataset() +
                                        " since checksum indicates no change", "OK"));
                    }
                } else {
                    tickleRepo.getEntityManager().persist(tickleRecord);
                    tickleRepo.getEntityManager().flush();
                    tickleRepo.getEntityManager().refresh(tickleRecord);
                    dataBuffer.append(String.format(dataPrintf, recordNo,
                            "created tickle repo record with ID " + tickleRecord.getLocalId() +
                                    " in dataset " + tickleRecord.getDataset(), "OK"));
                }

                LOGGER.info("Handled record {} in dataset {}", tickleRecord.getLocalId(), tickleRecord.getDataset());
            }
            recordNo++;
        }

        if (result.getStatus() == null) {
            result.withStatus(ChunkItem.Status.SUCCESS);
        }
        return result.withData(dataBuffer.toString().getBytes(StandardCharsets.UTF_8));
    }

    private byte[] getContent(ExpandedChunkItem item) {
        if (!StandardCharsets.UTF_8.equals(item.getEncoding())) {
            // Force UTF-8 encoding for tickle record content
            return StringUtil.asBytes(StringUtil.asString(item.getData(), item.getEncoding()), StandardCharsets.UTF_8);
        }
        return item.getData();
    }

    private Record.Status toStatus(TickleAttributes tickleAttributes) {
        if (tickleAttributes.isDeleted()) {
            return Record.Status.DELETED;
        }
        return Record.Status.ACTIVE;
    }
}
