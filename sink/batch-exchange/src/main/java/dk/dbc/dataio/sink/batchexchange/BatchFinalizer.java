package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.log.DBCTrackedLogContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Reports the delivery of items whose batches the consumer system has finished with.
 * <p>
 * One batch holds one item's records, and the outcome it carries is the verdict
 * {@code BatchExchangeMessageConsumer} could not give when it accepted the item. This class
 * is therefore where the item is reported, and it is what
 * {@code SinkMessageConsumerAdapter.defersDeliveryResult} refers to.
 * <p>
 * Finishing with a batch also releases the record it belonged to. A version of that record
 * held back while this one was in flight is staged here, in the transaction that removes the
 * completed batch, so that a record never has two versions staged at once.
 */
public class BatchFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchFinalizer.class);
    private final EntityManagerFactory entityManagerFactory;
    private final DeliveryReporter deliveryReporter;

    public BatchFinalizer(EntityManagerFactory entityManagerFactory,
                          JobStoreServiceConnector jobStoreServiceConnector, String fqn) {
        this.entityManagerFactory = entityManagerFactory;
        this.deliveryReporter = new DeliveryReporter(jobStoreServiceConnector, fqn);
    }

    /**
     * Reports the item of the next completed batch in the batch-exchange (if any).
     * <p>
     * This method runs in its own transactional scope to avoid
     * tearing down any controlling timers.
     *
     * @return true if batch was finalized, false if not.
     */
    public boolean finalizeNextCompletedBatch() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            /* The row lock taken by get_completed_batch is what stops two instances finalizing
               the same batch, and it lasts only as long as the transaction holding it. Claiming
               the batch outside this transaction would release it again immediately. */
            Batch batch = findCompletedBatch(entityManager);
            if (batch == null) {
                return false;
            }
            finalizeBatch(batch, entityManager);
            transaction.commit();
            return true;
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    /**
     * Reports one completed batch's item, removes the batch, and stages whatever its record
     * was holding
     * <p>
     * Everything after the record is locked runs with no other instance deciding anything for
     * that record.
     * <p>
     * A batch with no bookkeeping row is removed without being reported. Its item cannot be
     * addressed, so there is nothing to report it against, and leaving it in place would have
     * the finalizer meet it again on every pass and reach nothing behind it.
     */
    private void finalizeBatch(Batch batch, EntityManager entityManager) {
        StagedItem stagedItem = entityManager.find(StagedItem.class, batch.getId());
        if (stagedItem == null) {
            LOGGER.warn("Discarding batch {} named {}, no item is recorded as staged for it",
                    batch.getId(), batch.getName());
            entityManager.remove(batch);
            return;
        }
        BatchName batchName = stagedItem.batchName();
        /* Taken before anything is changed, so that a consumer deciding what to do with a new
           version of this record is either finished or has not started. Without it the held
           version can be staged here while a consumer is midway through replacing it, and the
           consumer then reports as superseded a version already on its way to the target. */
        RecordLock.acquire(entityManager, batchName);

        List<BatchEntry> batchEntries = getBatchEntries(batch, entityManager);
        LOGGER.info("Finalizing batch {} for item {}", batch.getId(), batchName);
        try {
            DBCTrackedLogContext.setTrackingId(trackingId(batchEntries));
            ChunkItem outcome = createOutcome(batchName, stagedItem, batchEntries);
            deliveryReporter.report(batchName, verdict(outcome), outcome);
        } finally {
            DBCTrackedLogContext.remove();
        }
        entityManager.remove(batch);
        /* Removing the batch cascades to the bookkeeping row, which is what frees the record
           for the held version staged below to take its place. */
        entityManager.flush();
        stageHeldItem(batchName, entityManager);

        for (BatchEntry batchEntry : batchEntries) {
            if (batchEntry.getTimeOfCompletion() != null) {
                Metric.dataio_batch_entry_timer.timer().update(Duration.ofMillis(
                        batchEntry.getTimeOfCompletion().getTime() - batchEntry.getTimeOfCreation().getTime()));
            }
        }
    }

    /**
     * Stages the version of the record that was held back while the finalized batch was in
     * flight, if there is one
     */
    private void stageHeldItem(BatchName batchName, EntityManager entityManager) {
        if (batchName.getRecordKey() == null) {
            /* An item with no record key has no record identity, so no version of it can have
               been held behind this batch. */
            return;
        }
        TypedQuery<HeldItem> query = entityManager.createQuery(
                "select d from HeldItem d where d.sinkId = :sinkId and d.recordKey = :recordKey",
                HeldItem.class);
        query.setParameter("sinkId", batchName.getSinkId());
        query.setParameter("recordKey", batchName.getRecordKey());
        HeldItem held = query.getResultStream().findFirst().orElse(null);
        if (held == null) {
            return;
        }
        int batch = BatchStager.stageHeldItem(entityManager, held);
        LOGGER.info("Staged held item {} as batch {}", held.batchName(), batch);
    }

    private Batch findCompletedBatch(EntityManager entityManager) {
        @SuppressWarnings("unchecked") List<Batch> batch = entityManager
                .createNamedQuery(Batch.GET_COMPLETED_BATCH_QUERY_NAME)
                .getResultList();
        if (batch.isEmpty()) {
            return null;
        }
        return entityManager.merge(batch.get(0));
    }

    @SuppressWarnings("unchecked")
    private List<BatchEntry> getBatchEntries(Batch batch, EntityManager entityManager) {
        /* The eclipselink.refresh hint below breaks portability, the
           alternative is to do a refresh on each entity returned, but
           this entails suboptimal performance.
           Note: javax.persistence.cache.retrieveMode hint does not
           currently work on native queries in eclipselink */
        return (List<BatchEntry>) entityManager
                .createNamedQuery(BatchEntry.GET_BATCH_ENTRIES_QUERY_NAME)
                .setParameter(1, batch.getId())
                .setHint("eclipselink.refresh", true)
                .getResultList();
    }

    /**
     * Builds the item's delivering outcome from everything the consumer system said about its
     * records
     * <p>
     * All of a batch's entries belong to one item, so their status messages are concatenated
     * into one outcome and their diagnostics appended to it. Versions of the record skipped to
     * arrive at this one are named alongside them, so that an outcome can be read without
     * reconstructing what happened from other jobs' items.
     */
    private ChunkItem createOutcome(BatchName batchName, StagedItem stagedItem, List<BatchEntry> batchEntries) {
        ChunkItemDataBuffer dataBuffer = new ChunkItemDataBuffer();
        ChunkItem chunkItem = new ChunkItem()
                .withId(batchName.getItemId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
        for (BatchEntry batchEntry : batchEntries) {
            // appendDiagnostics ensures that status is set to FAILURE if any FATAL level diagnostics are appended
            // After adding the diagnostics, also update the errors metrics
            List<Diagnostic> diagnostics = extractBatchEntryData(batchEntry, dataBuffer);
            chunkItem.appendDiagnostics(diagnostics);
            if (diagnostics.stream().anyMatch(diagnostic -> diagnostic.getLevel() != Diagnostic.Level.WARNING)) {
                Metric.dataio_batch_error_counter.counter().inc();
            }
        }
        if (stagedItem.getCollapsedCount() > 0) {
            dataBuffer.add(String.format(
                    "%d version(s) of this record were skipped to arrive at this one, from version %s",
                    stagedItem.getCollapsedCount(), stagedItem.getCollapsedFrom()));
        }
        if (chunkItem.getStatus() == null) {
            chunkItem.withStatus(entryStatus(batchEntries));
        }
        chunkItem.withData(dataBuffer.getBytes()).withTrackingId(trackingId(batchEntries));
        LOGGER.info("Result of downstream processing was {}", chunkItem.getStatus());
        return chunkItem;
    }

    /**
     * Gives the status the consumer system's answers add up to, for an item no entry carried
     * an error diagnostic for
     * <p>
     * The worst of the entries decides, so an item only some of whose records the consumer
     * system accepted is failed rather than delivered.
     */
    private ChunkItem.Status entryStatus(List<BatchEntry> batchEntries) {
        boolean allIgnored = true;
        for (BatchEntry batchEntry : batchEntries) {
            if (batchEntry.getStatus() == BatchEntry.Status.FAILED) {
                return ChunkItem.Status.FAILURE;
            }
            if (batchEntry.getStatus() != BatchEntry.Status.IGNORED) {
                allIgnored = false;
            }
        }
        if (allIgnored) {
            return ChunkItem.Status.IGNORE;
        }
        return ChunkItem.Status.SUCCESS;
    }

    /**
     * Decides how job-store counts the item and whether the record's delivery watermark moves
     * <p>
     * Read off the outcome item rather than off the entry statuses alone, because an entry the
     * consumer system accepted can still carry an error diagnostic, and
     * {@code ChunkItem.appendDiagnostics} has already turned that into a failure on the
     * outcome. Taking the verdict from anywhere else would count such an item as succeeded and
     * advance the record's watermark for a version the consumer system objected to.
     * <p>
     * A failed item leaves the watermark where it was, which keeps a later version of the
     * record free to reach the consumer system.
     */
    private ItemDeliveryResult.Status verdict(ChunkItem outcome) {
        return switch (outcome.getStatus()) {
            case FAILURE -> ItemDeliveryResult.Status.FAILED;
            case IGNORE -> ItemDeliveryResult.Status.IGNORED;
            case SUCCESS -> ItemDeliveryResult.Status.DELIVERED;
        };
    }

    private String trackingId(List<BatchEntry> batchEntries) {
        if (batchEntries.isEmpty()) {
            return null;
        }
        return batchEntries.get(0).getTrackingId();
    }

    private List<Diagnostic> extractBatchEntryData(BatchEntry batchEntry, ChunkItemDataBuffer dataBuffer) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (dk.dbc.batchexchange.dto.Diagnostic entryDiag : batchEntry.getDiagnostics()) {
            switch (entryDiag.getLevel()) {
                case ERROR:
                    diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, entryDiag.getMessage()));
                    dataBuffer.add(getStatusMessage(entryDiag));
                    break;
                case WARNING:
                    diagnostics.add(new Diagnostic(Diagnostic.Level.WARNING, entryDiag.getMessage()));
                    dataBuffer.add(getStatusMessage(entryDiag));
                    break;
                case OK:
                    dataBuffer.add(getStatusMessage(entryDiag));
                    break;
                default:
                    throw new IllegalStateException("Unknown batch entry diagnostic level: " + entryDiag.getLevel());
            }
        }
        return diagnostics;
    }

    private String getStatusMessage(dk.dbc.batchexchange.dto.Diagnostic entryDiag) {
        return String.format("Consumer system responded with %s: %s", entryDiag.getLevel(), entryDiag.getMessage());
    }

    private static class ChunkItemDataBuffer {
        private final StringBuilder buffer = new StringBuilder();

        ChunkItemDataBuffer add(String s) {
            buffer.append(s);
            buffer.append("\n");
            return this;
        }

        byte[] getBytes() {
            return buffer.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
}
