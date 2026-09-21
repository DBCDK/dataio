package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.MetaData;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stages one item's records in the batch exchange for a consumer system to collect.
 * <p>
 * Nothing is delivered to a target here, so no verdict is available when {@code deliverItem}
 * returns. The item's records become a batch of pending entries, the consumer system answers
 * for them in its own time, and {@link BatchFinalizer} reports the outcome, which is what
 * {@link #defersDeliveryResult()} declares. An item with nothing to stage is decided on the
 * spot instead.
 */
public class BatchExchangeMessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchExchangeMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    /* Backed by batch_name_pattern_index, so this runs as a range scan rather than a scan of
       the whole table. The ESCAPE clause matters: a record key is opaque and may contain the
       LIKE wildcards itself, see BatchName.likePrefixPattern. */
    private static final String BATCHES_STAGED_FOR_RECORD =
            "SELECT name FROM batch WHERE name LIKE ?1 ESCAPE '\\'";
    private final EntityManagerFactory entityManagerFactory;

    public BatchExchangeMessageConsumer(ServiceHub serviceHub, EntityManagerFactory entityManagerFactory) {
        super(serviceHub);
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Gives the destination this sink's deliveries are counted under, for
     * {@link BatchFinalizer}, which reports the deliveries this class defers and is not a
     * message consumer with a destination of its own
     * <p>
     * Built the way {@code MessageConsumer.getFQN} builds it, so that the deliveries reported
     * there and the ones reported here count towards one series rather than two.
     *
     * @return fully qualified name of the queue this sink consumes
     */
    static String fqn() {
        return ADDRESS + "::" + QUEUE;
    }

    /**
     * Reports the outcome of a staged item from {@link BatchFinalizer} rather than here
     *
     * @return true, this sink's target answers long after an item has been accepted
     */
    @Override
    protected boolean defersDeliveryResult() {
        return true;
    }

    /**
     * Stages a successfully processed item's records for the consumer system, and passes any
     * other item through as ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered,
     * so that it counts towards the job's ignored items and advances no delivery watermark for
     * a record nothing was sent for. It reaches the consumer system no more than it reaches a
     * target, so it is answered here instead of becoming an entry that completes immediately.
     *
     * @return the outcome of an item there was nothing to stage for, or null for an item whose
     * outcome the consumer system still owes
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        return switch (item.getStatus()) {
            case FAILURE -> ignored(item, "Failed by processor");
            case IGNORE -> ignored(item, "Ignored by processor");
            case SUCCESS -> stage(message, item);
        };
    }

    @Override
    public void abortJob(int jobId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            int deleted = deleteBatches(jobId, entityManager);
            transaction.commit();
            LOGGER.warn("Aborted job {}, deleted {} batches", jobId, deleted);
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
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
     * Stages the item's records as a batch of pending entries, unless this sink has already
     * staged the same item or a newer version of the same record
     *
     * @return null once the item is staged and its outcome is the consumer system's to give,
     * or an outcome for an item that was not staged at all
     */
    private ItemDeliveryResult stage(ConsumedMessage message, ChunkItem item) {
        BatchName batchName = BatchName.fromMessage(message);
        List<BatchEntry> entries;
        try {
            entries = createBatchEntries(item);
        } catch (RuntimeException | IOException e) {
            return failed(item, e);
        }
        if (entries.isEmpty()) {
            /* Staging nothing would create a batch that never completes, so the finalizer would
               never report the item and the job would never finish. */
            return ignored(item, "No records to deliver");
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            List<BatchName> staged = batchesStagedFor(batchName, entityManager);
            if (staged.contains(batchName)) {
                LOGGER.info("Item {} is already staged, leaving its result to the finalizer", batchName);
                return null;
            }
            Optional<BatchName> newer = newerThan(batchName, staged);
            if (newer.isPresent()) {
                LOGGER.info("Skipping item {}, newer version {} is already staged", batchName, newer.get());
                return superseded(item, newer.get());
            }

            Batch batch = createBatch(batchName, entityManager);
            String trackingId = trackingId(item, batchName);
            entries.forEach(entry -> entityManager.persist(entry
                    .withBatch(batch.getId())
                    .withTrackingId(trackingId)
                    .withPriority(message.getPriority().getValue())));
            LOGGER.info("Staged item {} as batch {} with {} entries", batchName, batch.getId(), entries.size());
            transaction.commit();
            return null;
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    /**
     * Gives the names of the batches this sink currently holds staged for the given item's
     * record
     * <p>
     * The names are matched on a literal prefix, which can catch a record key that is this one
     * with a hyphen and more appended, so each match is compared on its parsed record key
     * rather than trusted for having matched.
     *
     * @return names of the batches staged for the record, newest last is not implied
     */
    private List<BatchName> batchesStagedFor(BatchName batchName, EntityManager entityManager) {
        Query query = entityManager.createNativeQuery(BATCHES_STAGED_FOR_RECORD);
        query.setParameter(1, batchName.likePrefixPattern());
        @SuppressWarnings("unchecked") List<String> names = query.getResultList();
        List<BatchName> staged = new ArrayList<>(names.size());
        for (String name : names) {
            try {
                BatchName parsed = BatchName.fromString(name);
                if (parsed.hasSameRecordAs(batchName)) {
                    staged.add(parsed);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Ignoring batch with unparseable name {}", name, e);
            }
        }
        return staged;
    }

    /**
     * Finds a staged version of the record newer than the one being delivered
     * <p>
     * An item with no record key has no record identity to compare, so nothing can supersede
     * it and every such item is staged.
     */
    private Optional<BatchName> newerThan(BatchName batchName, List<BatchName> staged) {
        if (batchName.getRecordKey() == null) {
            return Optional.empty();
        }
        return staged.stream()
                .filter(batchName::isOlderThan)
                .findFirst();
    }

    private Batch createBatch(BatchName batchName, EntityManager entityManager) {
        Batch batch = new Batch().withName(batchName.toString());
        entityManager.persist(batch);
        entityManager.flush();
        entityManager.refresh(batch);
        return batch;
    }

    private List<BatchEntry> createBatchEntries(ChunkItem chunkItem) throws IOException {
        List<BatchEntry> entries = new ArrayList<>();
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        while (addiReader.hasNext()) {
            BatchEntry entry = createPendingBatchEntry(addiReader.next());
            if (addiReader.hasNext()) {
                entry.withIsContinued(true);
            }
            entries.add(entry);
        }
        return entries;
    }

    private int deleteBatches(int jobId, EntityManager entityManager) {
        /* The job id is no longer the name's prefix, so the pattern can match a batch whose
           record key happens to contain the same digits between hyphens. Deleting one of those
           would destroy work another job is still waiting on, so every candidate is parsed and
           only a genuine match is deleted. Aborting a job is rare enough to afford the scan. */
        Query query = entityManager.createNativeQuery(
                "SELECT name FROM batch WHERE name LIKE ?1 ESCAPE '\\'");
        query.setParameter(1, "%-" + jobId + "-%-%");
        @SuppressWarnings("unchecked") List<String> candidates = query.getResultList();
        List<String> names = new ArrayList<>();
        for (String name : candidates) {
            try {
                if (BatchName.fromString(name).getJobId() == jobId) {
                    names.add(name);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Ignoring batch with unparseable name {}", name, e);
            }
        }
        if (names.isEmpty()) {
            return 0;
        }
        return entityManager.createQuery("delete from Batch b where b.name in :names")
                .setParameter("names", names)
                .executeUpdate();
    }

    private BatchEntry createPendingBatchEntry(AddiRecord addiRecord) throws IOException {
        return new BatchEntry()
                .withContent(addiRecord.getContentData())
                .withMetadata(MetaData.fromXml(addiRecord.getMetaData()).getInfoJson());
    }

    /**
     * Gives the tracking id to stamp on the item's entries, so that the consumer system's
     * answer can be traced back to the item it belongs to
     */
    private String trackingId(ChunkItem chunkItem, BatchName batchName) {
        String trackingId = chunkItem.getTrackingId();
        if (trackingId == null) {
            return batchName.asTrackingId();
        }
        return trackingId;
    }

    private ItemDeliveryResult ignored(ChunkItem item, String reason) {
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                outcome(item)
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.WARNING, reason))
                        .withData(reason));
    }

    private ItemDeliveryResult failed(ChunkItem item, Exception cause) {
        String message = cause.getMessage();
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                outcome(item)
                        .withStatus(ChunkItem.Status.FAILURE)
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, message, cause))
                        .withData(StringUtil.getStackTraceString(cause)));
    }

    /**
     * Gives the outcome of an item a newer version of the same record has already been staged
     * for
     * <p>
     * Returned by this sink rather than by the sink framework, which can only see versions
     * whose delivery has been reported. For this sink a delivery is reported once the consumer
     * system has answered, so a newer version staged and still pending is invisible there and
     * visible here.
     */
    private ItemDeliveryResult superseded(ChunkItem item, BatchName newer) {
        String message = String.format(
                "Item was skipped, version %d/%d/%d of this record is already staged for delivery",
                newer.getJobId(), newer.getChunkId(), newer.getItemId());
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.SUPERSEDED,
                outcome(item)
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.WARNING, message))
                        .withData(message));
    }

    /**
     * Creates the delivering outcome item of an item decided here, without the status and data
     * naming what became of it
     */
    private ChunkItem outcome(ChunkItem item) {
        return new ChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
    }
}
