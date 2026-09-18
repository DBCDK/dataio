package dk.dbc.dataio.sink.ticklerepo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TickleMessageConsumer extends SinkMessageConsumerAdapter {
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
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        return deliverItem(message, item, new TickleRepo(entityManagerFactory.createEntityManager()));
    }

    ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item, TickleRepo tickleRepo) {
        int jobId = JMSHeader.jobId.getHeader(message, Integer.class);
        if (isTerminationItem(item)) {
            return finalizeBatch(jobId, item, tickleRepo);
        }
        return switch (item.getStatus()) {
            case SUCCESS -> putInTickleBatch(jobId, item, tickleRepo);
            case FAILURE -> ignored(item, "Failed by processor");
            case IGNORE -> ignored(item, "Ignored by processor");
        };
    }

    /**
     * Recognizes the job termination item the same way the chunk carrying it used to be
     * recognized, by the type job-store gives that item alone.
     */
    private boolean isTerminationItem(ChunkItem item) {
        return item.isTyped() && item.getType().getFirst() == ChunkItem.Type.JOB_END;
    }

    /**
     * Writes the records of one item to the tickle repo in a transaction of its own
     * <p>
     * The batch is resolved before that transaction opens and in one of its own, since every
     * thread and pod delivering an item of this job has to see the same batch. A failure
     * resolving it therefore propagates and has the item redelivered, where a failure writing
     * the records is reported as a failed item: a tickle repo that cannot create the batch will
     * not take the records either, whereas a single record that cannot be written is what the
     * delivering counters exist to express.
     */
    private ItemDeliveryResult putInTickleBatch(int jobId, ChunkItem item, TickleRepo tickleRepo) {
        List<ExpandedChunkItem> expandedItems = ExpandedChunkItem.safeFrom(item);
        if (expandedItems.isEmpty()) {
            // An item that holds no readable addi record leaves nothing to write, so it is
            // reported as ignored rather than delivered. Delivered is the one verdict that
            // advances the record's watermark, and advancing it here would claim this version
            // of the record as delivered when nothing of it reached the tickle repo.
            return ignored(item, "No addi records could be read from the item");
        }
        long startTime = System.currentTimeMillis();
        EntityManager entityManager = tickleRepo.getEntityManager();
        Batch batch = getOrCreateBatch(jobId, expandedItems, entityManager);
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Map<String, Record> records = lookupRecords(expandedItems, batch, tickleRepo);
            ItemDeliveryResult result = writeRecords(batch, item, expandedItems, records, tickleRepo);
            transaction.commit();
            Metric.HANDLE_CHUNK_ITEM.timer().update(Duration.ofMillis(System.currentTimeMillis() - startTime));
            return result;
        } catch (Exception e) {
            Metric.CHUNK_ITEM_FAILURES.counter().inc();
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED, failedItem(item, e));
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    /**
     * Closes the batch of the job the termination item belongs to, or aborts it when the item
     * says the job failed
     * <p>
     * A null batch means the job delivered no record carrying valid tickle attributes, so there
     * is nothing to close.
     */
    private ItemDeliveryResult finalizeBatch(int jobId, ChunkItem item, TickleRepo tickleRepo) {
        EntityManager entityManager = tickleRepo.getEntityManager();
        Batch batch = getBatch(jobId, entityManager);
        ChunkItem outcome = jobEndItem(item);
        if (batch == null) {
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED, outcome);
        }
        LOGGER.info("Got the termination item {} for batch {}", item.getTrackingId(), batch.getId());
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            // Nothing waits here for the records of the job to arrive. Each item commits its
            // own transaction before it is reported, and the job's termination item is not
            // dispatched until every one of them has been reported, so the batch is complete
            // by the time this item reaches any pod.
            if (item.getStatus() == ChunkItem.Status.SUCCESS) {
                batch.withTimeOfCompletion(tickleRepo.closeBatch(batch).getTimeOfCompletion());
                outcome.withData(String.format("Batch %d closed", batch.getId()));
            } else {
                batch.withTimeOfCompletion(tickleRepo.abortBatch(batch).getTimeOfCompletion());
                outcome.withData(String.format("Batch %d aborted", batch.getId()));
            }
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
        logCompletion(batch, entityManager);
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED, outcome);
    }

    private void logCompletion(Batch batch, EntityManager entityManager) {
        Batch batchCheck = entityManager.find(Batch.class, batch.getId(), Map.of(QueryHints.READ_ONLY, true));
        if (batchCheck.getTimeOfCompletion() == null) {
            LOGGER.error("Completed batch {} for job {} has no completion timestamp", batchCheck.getId(), batchCheck.getBatchKey());
        } else {
            LOGGER.info("Batch {} for job {} was closed with completion time: {}", batchCheck.getId(), batchCheck.getBatchKey(), batchCheck.getTimeOfCompletion());
        }
    }

    private Map<String, Record> lookupRecords(List<ExpandedChunkItem> expandedItems, Batch batch, TickleRepo tickleRepo) {
        if (batch == null) {
            return Map.of();
        }
        List<String> recordIds = expandedItems.stream()
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

    /**
     * Looks the job's batch up without creating one, for the termination item and for any other
     * caller that must not bring a batch into existence.
     *
     * @return the job's batch, or null when it has none
     */
    private Batch getBatch(int jobId, EntityManager entityManager) {
        Batch batch = batchCache.getIfPresent(jobId);
        if (batch != null) {
            return batch;
        }
        synchronized (TickleMessageConsumer.class) {
            batch = batchCache.getIfPresent(jobId);
            if (batch != null) {
                return batch;
            }
            batch = new TickleRepo(entityManager).lookupBatch(new Batch().withBatchKey(jobId), true).orElse(null);
            if (batch != null) {
                batchCache.put(jobId, batch);
            }
            return batch;
        }
    }

    /**
     * Resolves the job's batch, creating it from the first valid tickle attributes of the item
     * that got here first
     *
     * @return the job's batch, or null when this item carries no valid tickle attributes to
     * create one from
     */
    private Batch getOrCreateBatch(int jobId, List<ExpandedChunkItem> expandedItems, EntityManager entityManager) {
        Batch batch = getBatch(jobId, entityManager);
        if (batch != null) {
            return batch;
        }
        synchronized (TickleMessageConsumer.class) {
            batch = getBatch(jobId, entityManager);
            if (batch != null) {
                return batch;
            }
            batch = createBatch(jobId, expandedItems, entityManager);
            if (batch != null) {
                batchCache.put(jobId, batch);
            }
            return batch;
        }
    }

    protected Batch getBatch(int jobId, TickleRepo tickleRepo) {
        return tickleRepo.lookupBatch(new Batch().withBatchKey(jobId)).orElse(null);
    }

    private Batch createBatch(int jobId, List<ExpandedChunkItem> expandedItems, EntityManager entityManager) {
        TickleRepo tickleRepo = new TickleRepo(entityManager);
        TickleAttributes tickleAttributes = findFirstTickleAttributes(expandedItems).orElse(null);
        if (tickleAttributes == null) {
            return null;
        }
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
                    .withBatchKey(jobId)
                    .withDataset(dataset.getId())
                    .withType(tickleBehaviour)
                    .withMetadata(getBatchMetadata(jobId)));
            transaction.commit();
            return batch;
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
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

    private Optional<TickleAttributes> findFirstTickleAttributes(List<ExpandedChunkItem> expandedItems) {
        return expandedItems.stream()
                .map(ExpandedChunkItem::getTickleAttributes)
                .filter(TickleAttributes::isValid)
                .findFirst();
    }

    private ChunkItem jobEndItem(ChunkItem item) {
        return ChunkItem.successfulChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8)
                .withData("OK");
    }

    /**
     * The delivering outcome for an item this sink had nothing to send for, reported as
     * {@link ItemDeliveryResult.Status#IGNORED} so that it counts as ignored rather than
     * succeeded and advances no watermark, which is what the chunk path did with it.
     */
    private ItemDeliveryResult ignored(ChunkItem item, String reason) {
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED, ChunkItem.ignoredChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withStatus(ChunkItem.Status.IGNORE)
                .withType(ChunkItem.Type.STRING)
                .withData(reason)
                .withEncoding(StandardCharsets.UTF_8));
    }

    private ChunkItem failedItem(ChunkItem item, Exception e) {
        return ChunkItem.failedChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withStatus(ChunkItem.Status.FAILURE)
                .withType(ChunkItem.Type.STRING)
                .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                .withData(e.getMessage())
                .withEncoding(StandardCharsets.UTF_8);
    }

    /**
     * Writes the addi records of one item to the batch, collecting what each of them contributed
     * to the item's report
     * <p>
     * The verdict is decided from whether any record was rejected rather than read off the
     * returned item's status, which {@link ChunkItem#withDiagnostics} sets as a side effect of
     * recording a rejection.
     *
     * @return the outcome to report, delivered when every record of the item was written and
     * failed when at least one was rejected
     */
    private ItemDeliveryResult writeRecords(Batch batch, ChunkItem item, List<ExpandedChunkItem> expandedItems,
                                            Map<String, Record> records, TickleRepo tickleRepo) {
        ChunkItem outcome = new ChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);

        StringBuilder report = new StringBuilder();
        boolean rejected = false;
        int recordNo = 1;
        for (ExpandedChunkItem expandedItem : expandedItems) {
            RecordReport recordReport = writeRecord(batch, expandedItem, records, tickleRepo);
            report.append(String.format(DATA_PRINTF, recordNo++, recordReport.message(),
                    recordReport.isRejected() ? "ERROR" : "OK"));
            if (recordReport.isRejected()) {
                rejected = true;
                outcome.withDiagnostics(recordReport.diagnostic());
            }
        }

        outcome.withStatus(rejected ? ChunkItem.Status.FAILURE : ChunkItem.Status.SUCCESS)
                .withData(report.toString().getBytes(StandardCharsets.UTF_8));
        return ItemDeliveryResult.of(
                rejected ? ItemDeliveryResult.Status.FAILED : ItemDeliveryResult.Status.DELIVERED,
                outcome);
    }

    /**
     * What became of one addi record of an item: the line it contributes to the item's report, and
     * the diagnostic when the record was rejected rather than written.
     */
    private record RecordReport(String message, Diagnostic diagnostic) {
        static RecordReport written(String message) {
            return new RecordReport(message, null);
        }

        static RecordReport rejected(Diagnostic diagnostic) {
            return new RecordReport(diagnostic.getMessage(), diagnostic);
        }

        boolean isRejected() {
            return diagnostic != null;
        }
    }

    private RecordReport writeRecord(Batch batch, ExpandedChunkItem expandedItem,
                                     Map<String, Record> records, TickleRepo tickleRepo) {
        TickleAttributes tickleAttributes = expandedItem.getTickleAttributes();
        if (!tickleAttributes.isValid()) {
            return RecordReport.rejected(new Diagnostic(Diagnostic.Level.FATAL,
                    "Invalid tickle attributes extracted from record " + tickleAttributes));
        }
        Record existing = records.get(tickleAttributes.getBibliographicRecordId());
        if (existing == null) {
            return createRecord(batch, tickleAttributes, expandedItem, records, tickleRepo);
        }
        return updateRecord(batch, tickleAttributes, expandedItem, existing);
    }

    /**
     * Moves an existing record into this batch, unless its checksum says the content did not
     * change, in which case it is left in the batch that last modified it.
     */
    private RecordReport updateRecord(Batch batch, TickleAttributes tickleAttributes,
                                      ExpandedChunkItem expandedItem, Record record) {
        record.withContent(getContent(expandedItem)).withStatus(toStatus(tickleAttributes));
        record.updateBatchIfModified(batch, tickleAttributes.getCompareRecord());
        LOGGER.debug("Handled record {} in dataset {}", record.getLocalId(), record.getDataset());
        if (record.getBatch() != batch.getId()) {
            return RecordReport.written("tickle repo record with ID " + record.getLocalId() +
                    " not updated in dataset " + record.getDataset() +
                    " since checksum indicates no change");
        }
        record.withTrackingId(expandedItem.getTrackingId());
        return RecordReport.written("updated tickle repo record with ID " + record.getLocalId() +
                " in dataset " + record.getDataset());
    }

    private RecordReport createRecord(Batch batch, TickleAttributes tickleAttributes,
                                      ExpandedChunkItem expandedItem, Map<String, Record> records,
                                      TickleRepo tickleRepo) {
        Record record = new Record()
                .withBatch(batch.getId())
                .withDataset(batch.getDataset())
                .withStatus(toStatus(tickleAttributes))
                .withTrackingId(expandedItem.getTrackingId())
                .withLocalId(tickleAttributes.getBibliographicRecordId())
                .withContent(getContent(expandedItem))
                .withChecksum(tickleAttributes.getCompareRecord());
        EntityManager entityManager = tickleRepo.getEntityManager();
        entityManager.persist(record);
        entityManager.flush();
        entityManager.refresh(record);
        // Visible to the rest of this item, so an item carrying the same local ID twice updates the
        // record it just created rather than trying to create it a second time.
        records.put(tickleAttributes.getBibliographicRecordId(), record);
        LOGGER.debug("Handled record {} in dataset {}", record.getLocalId(), record.getDataset());
        return RecordReport.written("created tickle repo record with ID " + record.getLocalId() +
                " in dataset " + record.getDataset());
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
