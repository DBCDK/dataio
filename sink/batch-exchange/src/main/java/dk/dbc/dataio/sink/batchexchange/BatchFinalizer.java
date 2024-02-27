package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
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

/**
 * This enterprise Java bean handles completion of batch-exchange batches.
 */
public class BatchFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchFinalizer.class);
    private final EntityManagerFactory entityManagerFactory;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    public BatchFinalizer(EntityManagerFactory entityManagerFactory, JobStoreServiceConnector jobStoreServiceConnector) {
        this.entityManagerFactory = entityManagerFactory;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    /**
     * Builds and uploads chunk for next completed batch in the
     * batch-exchange (if any).
     * <p>
     * This method runs in its own transactional scope to avoid
     * tearing down any controlling timers.
     *
     * @return true if batch was finalized, false if not.
     */
    public boolean finalizeNextCompletedBatch() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Batch batch = findCompletedBatch(entityManager);
        if (batch == null) {
            return false;
        }
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            BatchName batchName = BatchName.fromString(batch.getName());
            LOGGER.info("Finalizing batch {} for chunk {}/{}",
                    batch.getId(), batchName.getJobId(), batchName.getChunkId());

            List<BatchEntry> batchEntries = getBatchEntries(batch, entityManager);
            Chunk chunk = createChunkFromBatchEntries(batchName.getJobId(), batchName.getChunkId(), batchEntries);
            uploadChunk(chunk);
            entityManager.remove(batch);

            for (BatchEntry batchEntry : batchEntries) {
                if (batchEntry.getTimeOfCompletion() != null) {
                    Metric.dataio_batch_entry_timer.timer().update(Duration.ofMillis(
                            batchEntry.getTimeOfCompletion().getTime() - batchEntry.getTimeOfCreation().getTime()));
                }
            }
            transaction.commit();
        } finally {
            if(transaction.isActive()) transaction.rollback();
        }

        return true;
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

    private Chunk createChunkFromBatchEntries(int jobId, long chunkId, List<BatchEntry> batchEntries) {
        Chunk chunk = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        long chunkItemId = 0;
        ChunkItemDataBuffer dataBuffer = new ChunkItemDataBuffer();
        ChunkItem chunkItem = new ChunkItem();
        for (BatchEntry batchEntry : batchEntries) {
            DBCTrackedLogContext.setTrackingId(batchEntry.getTrackingId());
            try {
                // appendDiagnostics ensures that status is set to FAILURE if any FATAL level diagnostics are appended
                // After adding the diagnostics, also update the errors metrics
                List<Diagnostic> diagnostics = extractBatchEntryData(batchEntry, dataBuffer);
                chunkItem.appendDiagnostics(diagnostics);
                if (diagnostics.stream().anyMatch(diagnostic -> diagnostic.getLevel() != Diagnostic.Level.WARNING)) {
                    Metric.dataio_batch_error_counter.counter().inc();
                }

                if (!batchEntry.getContinued()) {
                    if (chunkItem.getStatus() == null) {
                        chunkItem.withStatus(convertBatchEntryStatus(batchEntry.getStatus()));
                    }
                    chunk.insertItem(chunkItem
                            .withId(chunkItemId++)
                            .withType(ChunkItem.Type.STRING)
                            .withData(dataBuffer.getBytes())
                            .withTrackingId(batchEntry.getTrackingId()));
                    LOGGER.info("Result of downstream processing was {}", chunkItem.getStatus());
                    dataBuffer = new ChunkItemDataBuffer();
                    chunkItem = new ChunkItem();
                }
            } finally {
                DBCTrackedLogContext.remove();
            }
        }
        return chunk;
    }

    private ChunkItem.Status convertBatchEntryStatus(BatchEntry.Status batchEntryStatus) {
        switch (batchEntryStatus) {
            case FAILED:
                return ChunkItem.Status.FAILURE;
            case IGNORED:
                return ChunkItem.Status.IGNORE;
            case OK:
                return ChunkItem.Status.SUCCESS;
            default:
                throw new IllegalStateException("illegal batch entry status " + batchEntryStatus);
        }
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

    private void uploadChunk(Chunk chunk) {
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (Exception e) {
            String message = String.format("Error in communication with job-store for chunk [%d, %d]",
                    chunk.getJobId(), chunk.getChunkId());
            throw new RuntimeException(message, e);
        }
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
