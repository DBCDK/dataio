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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.destination;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.status;
import static dk.dbc.dataio.jse.artemis.common.Metric.dataio_item_delivery_count;

/**
 * Reports the delivery of items whose batches the consumer system has finished with.
 * <p>
 * One batch holds one item's records, and the outcome it carries is the verdict
 * {@code BatchExchangeMessageConsumer} could not give when it accepted the item. This class
 * is therefore where the item is reported, and it is what
 * {@code SinkMessageConsumerAdapter.defersDeliveryResult} refers to.
 */
public class BatchFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchFinalizer.class);
    private final EntityManagerFactory entityManagerFactory;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final String fqn;

    public BatchFinalizer(EntityManagerFactory entityManagerFactory,
                          JobStoreServiceConnector jobStoreServiceConnector, String fqn) {
        this.entityManagerFactory = entityManagerFactory;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.fqn = fqn;
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
        try {
            Batch batch = findCompletedBatch(entityManager);
            if (batch == null) {
                return false;
            }
            EntityTransaction transaction = entityManager.getTransaction();
            try {
                transaction.begin();
                finalizeBatch(batch, entityManager);
                transaction.commit();
            } finally {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
            }
            return true;
        } finally {
            entityManager.close();
        }
    }

    /**
     * Reports one completed batch's item and removes the batch
     * <p>
     * A batch whose name does not name an item is removed without being reported. Its item
     * cannot be addressed, so there is nothing to report it against, and leaving it in place
     * would have the finalizer meet it again on every pass and reach nothing behind it.
     */
    private void finalizeBatch(Batch batch, EntityManager entityManager) {
        BatchName batchName;
        try {
            batchName = BatchName.fromString(batch.getName());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Discarding batch {} , its name {} does not name an item",
                    batch.getId(), batch.getName(), e);
            entityManager.remove(batch);
            return;
        }

        List<BatchEntry> batchEntries = getBatchEntries(batch, entityManager);
        LOGGER.info("Finalizing batch {} for item {}", batch.getId(), batchName);
        try {
            DBCTrackedLogContext.setTrackingId(trackingId(batchEntries));
            report(batchName, createOutcome(batchName, batchEntries));
        } finally {
            DBCTrackedLogContext.remove();
        }
        entityManager.remove(batch);

        for (BatchEntry batchEntry : batchEntries) {
            if (batchEntry.getTimeOfCompletion() != null) {
                Metric.dataio_batch_entry_timer.timer().update(Duration.ofMillis(
                        batchEntry.getTimeOfCompletion().getTime() - batchEntry.getTimeOfCreation().getTime()));
            }
        }
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
     * into one outcome and their diagnostics appended to it.
     */
    private ChunkItem createOutcome(BatchName batchName, List<BatchEntry> batchEntries) {
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

    /**
     * Reports the item's delivery to job-store, naming the watermark row it may advance
     * <p>
     * The sink id and record key come from the batch name, which is where the message that
     * staged the item put them. A record key the item arrived without stays null, which is
     * what tells job-store the item has no watermark row rather than that this outcome must
     * not advance one.
     */
    private void report(BatchName batchName, ChunkItem outcome) {
        ItemDeliveryResult.Status verdict = verdict(outcome);
        ItemDeliveryResult result = ItemDeliveryResult.of(verdict, outcome)
                .withWatermarkKey(batchName.getSinkId(), batchName.getRecordKey());
        try {
            jobStoreServiceConnector.addItemDelivered(result,
                    batchName.getJobId(), (int) batchName.getChunkId(), batchName.getItemId());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error in communication with job-store for item %s", batchName), e);
        }
        dataio_item_delivery_count.counter(destination.is(fqn), status.is(verdict.name())).inc();
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
