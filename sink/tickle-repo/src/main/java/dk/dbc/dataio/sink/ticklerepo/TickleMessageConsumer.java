package dk.dbc.dataio.sink.ticklerepo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Tools;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.persistence.config.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TickleMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickleMessageConsumer.class);
    private final Batch.Type tickleBehaviour = Batch.Type.valueOf(SinkConfig.TICKLE_BEHAVIOUR.asString().toUpperCase());
    static final Cache<Integer, Batch> batchCache = CacheBuilder.newBuilder().maximumSize(50).expireAfterAccess(Duration.ofHours(1)).build();
    private final EntityManagerFactory entityManagerFactory;
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private static final String DATA_PRINTF = "Record %d: %s\n\t%s\n";


    public TickleMessageConsumer(ServiceHub serviceHub, EntityManagerFactory entityManagerFactory) {
        super(serviceHub);
        this.entityManagerFactory = entityManagerFactory;
        registerMetrics(PrometheusMetricRegistry.create());
    }

    @SuppressWarnings({"java:S2095", "unchecked"})
    public void registerMetrics(MetricRegistry metricRegistry) {
        Query query = entityManagerFactory.createEntityManager().createNativeQuery("SELECT * FROM dataset", DataSet.class);
        List<DataSet> dataSets = query.getResultList();
        for (DataSet dataSet : dataSets) {
            Tag dataSetTag = new Tag("dataset_name", dataSet.getName());
            MetricID metricID = new MetricID("dataio_tickle_repo_oldest_batch_in_hours", dataSetTag);
            Gauge<?> gauge = metricRegistry.getGauge(metricID);
            if (gauge == null) metricRegistry.gauge(metricID, () -> getOldestOpenBatch(dataSet.getId()));
            LOGGER.info("Registered age gauge for dataSet -> {}", dataSet.getId());
        }
    }

    @SuppressWarnings("java:S2095")
    private long getOldestOpenBatch(int dataSetId) {
        String timeZone = SinkConfig.TIMEZONE.asString();
        Query query = entityManagerFactory.createEntityManager().createNativeQuery("select * from batch where dataset = ? and timeofcompletion is null order by timeofcreation", Batch.class);
        query.setHint(QueryHints.READ_ONLY, true);
        query.setParameter(1, dataSetId);
        @SuppressWarnings("unchecked")
        List<Batch> batches = query.getResultList();
        if (batches.isEmpty()) return 0;
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of(timeZone));
        ZonedDateTime then = batches.get(0).getTimeOfCreation().toLocalDateTime().atZone(ZoneId.of(timeZone));
        return ChronoUnit.HOURS.between(then, now);
    }



    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        handleConsumedMessage(consumedMessage, new TickleRepo(entityManagerFactory.createEntityManager()));
    }

    public void handleConsumedMessage(ConsumedMessage consumedMessage, TickleRepo tickleRepo) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        EntityManager entityManager = tickleRepo.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        Batch batch = getBatch(chunk, entityManager);
        try {
            transaction.begin();
            Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
            if (chunk.isTerminationChunk()) {
                LOGGER.info("Got the termination chunk {} for batch {}", chunk.getTrackingId(), batch.getId());
                // Give the before-last message enough time to commit
                // its records to the tickle-repo before initiating
                // the finalization process.
                // (The result is uploaded to the job-store before the
                // implicit commit, so without the sleep pause, there was a
                // small risk that the end-chunk would reach this bean
                // before all data was available.)
                Tools.sleep(5000);
                result.insertItem(handleJobEnd(chunk.getItems().get(0), batch, tickleRepo));
            } else {
                IdentityHashMap<ChunkItem, List<ExpandedChunkItem>> expandChunkItems = expandChunkItems(chunk);
                Map<String, Record> records = extractRecordsFrom(expandChunkItems.values().stream().flatMap(Collection::stream), batch, tickleRepo);
                chunk.getItems().forEach(chunkItem -> result.insertItem(handleChunkItem(chunkItem, batch, records, expandChunkItems, tickleRepo)));
            }

            transaction.commit();
            if(chunk.isTerminationChunk()) {
                Batch batchCheck = entityManager.find(Batch.class, batch.getId(), Map.of(QueryHints.READ_ONLY, true));
                if(batchCheck.getTimeOfCompletion() == null) LOGGER.error("Completed batch {} for job {} has no completion timestamp", batchCheck.getId(), batchCheck.getBatchKey());
                else LOGGER.info("Batch {} for job {} was closed with completion time: {}", batchCheck.getId(), batchCheck.getBatchKey(), batchCheck.getTimeOfCompletion());
            }
            sendResultToJobStore(result);
        } finally {
            if(transaction.isActive()) transaction.rollback();
        }
    }

    public IdentityHashMap<ChunkItem, List<ExpandedChunkItem>> expandChunkItems(Chunk chunk) {
        return chunk.getItems().stream().collect(Collectors.toMap(ci -> ci, ExpandedChunkItem::safeFrom, (l1, l2) -> l1, IdentityHashMap::new));
    }

    public Map<String, Record> extractRecordsFrom(Stream<ExpandedChunkItem> expandChunkItems, Batch batch, TickleRepo tickleRepo) {
        if(batch == null) return Map.of();
        List<String> recordIds = expandChunkItems
                .map(ExpandedChunkItem::getTickleAttributes)
                .filter(TickleAttributes::isValid)
                .map(TickleAttributes::getBibliographicRecordId)
                .collect(Collectors.toList());
        List<Record> records = tickleRepo.lookupRecords(batch.getDataset(), recordIds);
        return records.stream().collect(Collectors.toMap(Record::getLocalId, r -> r, (k1, k2) -> k1, HashMap::new));
    }

    @Override
    public void abortJob(int jobId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            Batch batch = getBatch(jobId, new TickleRepo(entityManager));
            entityManager.remove(batch);
            batchCache.invalidate(jobId);
        } finally {
            if(transaction.isActive()) transaction.commit();
        }
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    public Batch getBatch(Chunk chunk, EntityManager entityManager) {
        int jobId = chunk.getJobId();
        Batch batch = batchCache.getIfPresent(jobId);
        if(batch != null) return batch;
        synchronized (TickleMessageConsumer.class) {
            batch = batchCache.getIfPresent(jobId);
            if(batch != null) return batch;
            batch = new TickleRepo(entityManager).lookupBatch(new Batch().withBatchKey(jobId), true).orElse(null);
            if(batch == null) batch = createBatch(chunk, entityManager);
            if(batch != null) batchCache.put(jobId, batch);
            return batch;
        }
    }

    protected Batch getBatch(int jobId, TickleRepo tickleRepo) {
        return tickleRepo.lookupBatch(new Batch().withBatchKey(jobId)).orElse(null);
    }

    private Batch createBatch(Chunk chunk, EntityManager entityManager) {
        TickleRepo tickleRepo = new TickleRepo(entityManager);
        TickleAttributes tickleAttributes = findFirstTickleAttributes(chunk).orElse(null);
        if(tickleAttributes == null) return null;
        // find dataset or else create it
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            DataSet searchValue = new DataSet()
                    .withName(tickleAttributes.getDatasetName())
                    .withAgencyId(tickleAttributes.getAgencyId());
            DataSet dataset = tickleRepo.lookupDataSet(searchValue)
                    .orElseGet(() -> tickleRepo.createDataSet(searchValue));
            // create new batch and cache it
            Batch batch = tickleRepo.createBatch(new Batch()
                    .withBatchKey(chunk.getJobId())
                    .withDataset(dataset.getId())
                    .withType(tickleBehaviour)
                    .withMetadata(getBatchMetadata(chunk.getJobId())));
            transaction.commit();
            return batch;
        } finally {
            if(transaction.isActive()) transaction.rollback();
        }
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

    private ChunkItem handleJobEnd(ChunkItem chunkItem, Batch batch, TickleRepo tickleRepo) {
        ChunkItem result = ChunkItem.successfulChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8)
                .withData("OK");

        if (batch != null) {
            if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                batch.withTimeOfCompletion(tickleRepo.closeBatch(batch).getTimeOfCompletion());
                result.withData(String.format("Batch %d closed", batch.getId()));
            } else {
                batch.withTimeOfCompletion(tickleRepo.abortBatch(batch).getTimeOfCompletion());
                result.withData(String.format("Batch %d aborted", batch.getId()));
            }
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem, Batch batch, Map<String, Record> records, IdentityHashMap<ChunkItem, List<ExpandedChunkItem>> expandChunkItems, TickleRepo tickleRepo) {
        try {
            DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());

            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    long handleChunkItemStartTime = System.currentTimeMillis();
                    ChunkItem item = putInTickleBatch(batch, chunkItem, records, expandChunkItems, tickleRepo);
                    Metric.HANDLE_CHUNK_ITEM.timer().update(Duration.ofMillis(System.currentTimeMillis() - handleChunkItemStartTime));
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

    private ChunkItem putInTickleBatch(Batch batch, ChunkItem chunkItem, Map<String, Record> records, IdentityHashMap<ChunkItem, List<ExpandedChunkItem>> expandChunkItems, TickleRepo tickleRepo) {
        ChunkItem result = new ChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);

        StringBuilder dataBuffer = new StringBuilder();
        int recordNo = 1;
        for (ExpandedChunkItem item : expandChunkItems.get(chunkItem)) {
            TickleAttributes tickleAttributes = item.getTickleAttributes();
            if (!tickleAttributes.isValid()) {
                Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL,
                        "Invalid tickle attributes extracted from record " + tickleAttributes);
                result.withDiagnostics(diagnostic);
                dataBuffer.append(String.format(DATA_PRINTF, recordNo, diagnostic.getMessage(), "ERROR"));
            } else {
                byte[] content = getContent(item);
                Record tickleRecord;
                Record lookupRecord = records.get(tickleAttributes.getBibliographicRecordId());
                if (lookupRecord != null) {
                    tickleRecord = lookupRecord.withContent(content).withStatus(toStatus(tickleAttributes));
                    tickleRecord.updateBatchIfModified(batch, tickleAttributes.getCompareRecord());
                    if (tickleRecord.getBatch() == batch.getId()) {
                        tickleRecord.withTrackingId(item.getTrackingId());
                        dataBuffer.append(String.format(DATA_PRINTF, recordNo,
                                "updated tickle repo record with ID " + tickleRecord.getLocalId() +
                                        " in dataset " + tickleRecord.getDataset(), "OK"));
                    } else {
                        dataBuffer.append(String.format(DATA_PRINTF, recordNo,
                                "tickle repo record with ID " + tickleRecord.getLocalId() +
                                        " not updated in dataset " + tickleRecord.getDataset() +
                                        " since checksum indicates no change", "OK"));
                    }
                } else {
                    tickleRecord = createTickleRecord(batch, tickleAttributes, item, content, recordNo, dataBuffer, tickleRepo);
                    records.put(tickleAttributes.getBibliographicRecordId(), tickleRecord);
                }

                LOGGER.debug("Handled record {} in dataset {}", tickleRecord.getLocalId(), tickleRecord.getDataset());
            }
            recordNo++;
        }

        if (result.getStatus() == null) {
            result.withStatus(ChunkItem.Status.SUCCESS);
        }
        return result.withData(dataBuffer.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Record createTickleRecord(Batch batch, TickleAttributes tickleAttributes, ChunkItem item, byte[] content, int recordNo, StringBuilder dataBuffer, TickleRepo tickleRepo) {
        Record tickleRecord = new Record()
                .withBatch(batch.getId())
                .withDataset(batch.getDataset())
                .withStatus(toStatus(tickleAttributes))
                .withTrackingId(item.getTrackingId())
                .withLocalId(tickleAttributes.getBibliographicRecordId())
                .withContent(content)
                .withChecksum(tickleAttributes.getCompareRecord());
        tickleRepo.getEntityManager().persist(tickleRecord);
        tickleRepo.getEntityManager().flush();
        tickleRepo.getEntityManager().refresh(tickleRecord);
        dataBuffer.append(String.format(DATA_PRINTF, recordNo,
                "created tickle repo record with ID " + tickleRecord.getLocalId() +
                        " in dataset " + tickleRecord.getDataset(), "OK"));
        return tickleRecord;
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
