package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@MessageDriven(name = "tickleRepoListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "resource = '${ENV=MESSAGE_NAME_FILTER}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryBackOffMultiplier", propertyValue = "4"),
        @ActivationConfigProperty(propertyName = "maximumRedeliveries", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "MaxSession", propertyValue = "4")
})
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    Batch.Type tickleBehaviour = Batch.Type.INCREMENTAL;

    @PostConstruct
    public void setTickleBehaviour() {
        final String behaviour = System.getenv("TICKLE_BEHAVIOUR");
        if (behaviour != null && "TOTAL".equals(behaviour.toUpperCase())) {
            tickleBehaviour = Batch.Type.TOTAL;
        }
        LOGGER.info("Sink running with {} tickle behaviour", tickleBehaviour);
    }

    @EJB
    TickleRepo tickleRepo;

    @Inject
    MetricsHandlerBean metricsHandler;

    // cached mappings of job-ID to Batch
    Cache<Integer, Batch> batchCache = CacheManager.createLRUCache(50);

    @Override
    @Stopwatch
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, ServiceException {
        try {
            final Chunk chunk = unmarshallPayload(consumedMessage);
            LOGGER.info("Handling chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

            final Batch batch = getBatch(chunk);

            final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
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
                    throw new SinkException(e);
                }
                result.insertItem(handleJobEnd(chunk.getItems().get(0), batch));
            } else {
                chunk.getItems().forEach(
                        chunkItem -> result.insertItem(handleChunkItem(chunkItem, batch)));
            }

            uploadChunk(result);
        } catch (Exception any) {
            LOGGER.error("Caught unhandled exception: " + any.getMessage());
            metricsHandler.increment(TickleCounterMetrics.UNHANDLED_EXCEPTIONS);
            throw any;
        }
    }

    private Batch getBatch(Chunk chunk) {
        if (batchCache.containsKey(chunk.getJobId())) {
            // we already have the batch cached
            return batchCache.get(chunk.getJobId());
        }

        final Optional<Batch> batch = tickleRepo.lookupBatch(new Batch().withBatchKey(chunk.getJobId()));
        if (batch.isPresent()) {
            // batch is not in cache but already exists in tickle repo
            batchCache.put(chunk.getJobId(), batch.get());
            return batch.get();
        }

        final TickleAttributes tickleAttributes = findFirstTickleAttributes(chunk).orElse(null);
        if (tickleAttributes != null) {
            // find dataset or else create it
            final DataSet searchValue = new DataSet()
                    .withName(tickleAttributes.getDatasetName())
                    .withAgencyId(tickleAttributes.getAgencyId());
            final DataSet dataset = tickleRepo.lookupDataSet(searchValue)
                    .orElseGet(() -> tickleRepo.createDataSet(searchValue));
            // create new batch and cache it
            final Batch createdBatch = tickleRepo.createBatch(new Batch()
                    .withBatchKey(chunk.getJobId())
                    .withDataset(dataset.getId())
                    .withType(tickleBehaviour)
                    .withMetadata(getBatchMetadata(chunk.getJobId())));
            batchCache.put(chunk.getJobId(), createdBatch);
            return createdBatch;
        }

        // no chunk item exists with valid tickle attributes
        return null;
    }

    /* Use job specification as batch metadata */
    private String getBatchMetadata(long jobId) {
        try {
            final List<JobInfoSnapshot> jobInfoSnapshots = jobStoreServiceConnectorBean.getConnector()
                    .listJobs("job:id = " + jobId);
            if (!jobInfoSnapshots.isEmpty()) {
                final JSONBContext jsonbContext = new JSONBContext();
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
        final ChunkItem result = ChunkItem.successfulChunkItem()
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
                    metricsHandler.update(TickleTimerMetrics.HANDLE_CHUNK_ITEM, Duration.ofMillis(System.currentTimeMillis() - handleChunkItemStartTime),
                            new Tag("dataset", Integer.toString(batch.getDataset())));
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
            metricsHandler.increment(TickleCounterMetrics.CHUNK_ITEM_FAILURES);
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
        final ChunkItem result = new ChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);

        final StringBuilder dataBuffer = new StringBuilder();
        final String dataPrintf = "Record %d: %s\n\t%s\n";
        int recordNo = 1;
        for (ExpandedChunkItem item : ExpandedChunkItem.from(chunkItem)) {
            final TickleAttributes tickleAttributes = item.getTickleAttributes();
            if (!tickleAttributes.isValid()) {
                final Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL,
                        "Invalid tickle attributes extracted from record " + tickleAttributes);
                result.withDiagnostics(diagnostic);
                dataBuffer.append(String.format(dataPrintf, recordNo, diagnostic.getMessage(), "ERROR"));
            } else {
                final byte[] content = getContent(item);
                Record tickleRecord = new Record()
                        .withBatch(batch.getId())
                        .withDataset(batch.getDataset())
                        .withStatus(toStatus(tickleAttributes))
                        .withTrackingId(item.getTrackingId())
                        .withLocalId(tickleAttributes.getBibliographicRecordId())
                        .withContent(content)
                        .withChecksum(tickleAttributes.getCompareRecord());

                final Optional<Record> lookupRecord = tickleRepo.lookupRecord(tickleRecord);
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
