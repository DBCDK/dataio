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

package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This enterprise Java bean handles completion of batch-exchange batches.
 */
@Singleton
public class BatchFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchFinalizerBean.class);

    @PersistenceContext(unitName = "batchExchangePU")
    EntityManager entityManager;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    /**
     * Builds and uploads chunk for next completed batch in the
     * batch-exchange (if any).
     *
     * This method runs in its own transactional scope to avoid
     * tearing down any controlling timers.
     *
     * @return true if batch was finalized, false if not.
     * @throws SinkException nn failure to communicate with the job-store
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean finalizeNextCompletedBatch() throws SinkException {
        final Batch batch = findCompletedBatch();
        if (batch == null) {
            return false;
        }
        final BatchName batchName = BatchName.fromString(batch.getName());
        LOGGER.info("Finalizing batch %d for chunk[%d, %d]",
                batch.getId(), batchName.getJobId(), batchName.getChunkId());

        final List<BatchEntry> batchEntries = getBatchEntries(batch);
        final Chunk chunk = createChunkFromBatchEntries(batchName.getJobId(), batchName.getChunkId(), batchEntries);
        uploadChunk(chunk);
        entityManager.remove(batch);
        return true;
    }

    private Batch findCompletedBatch() {
        @SuppressWarnings("unchecked")
        final List<Batch> batch = entityManager
                .createNamedQuery(Batch.GET_COMPLETED_BATCH_QUERY_NAME)
                .getResultList();
        if (batch.isEmpty()) {
            return null;
        }
        return entityManager.merge(batch.get(0));
    }

    @SuppressWarnings("unchecked")
    private List<BatchEntry> getBatchEntries(Batch batch) {
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

    private Chunk createChunkFromBatchEntries(long jobId, long chunkId, List<BatchEntry> batchEntries) {
        final Chunk chunk = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        long chunkItemId = 0;
        ChunkItem chunkItem = new ChunkItem().withId(chunkItemId);
        ChunkItemDataBuffer dataBuffer = new ChunkItemDataBuffer();
        for (BatchEntry batchEntry : batchEntries) {
            DBCTrackedLogContext.setTrackingId(batchEntry.getTrackingId());
            try {
                chunkItem.appendDiagnostics(extractBatchEntryData(batchEntry, dataBuffer));
                if (!batchEntry.getContinued()) {
                    chunk.insertItem(chunkItem
                            .withData(dataBuffer.getBytes())
                            .withTrackingId(batchEntry.getTrackingId()));
                    LOGGER.info("Result of downstream processing was {}", chunkItem.getStatus());
                    chunkItem = new ChunkItem().withId(++chunkItemId);
                    dataBuffer = new ChunkItemDataBuffer();
                }
            } finally {
                DBCTrackedLogContext.remove();
            }
        }
        return chunk;
    }

    private List<Diagnostic> extractBatchEntryData(BatchEntry batchEntry, ChunkItemDataBuffer dataBuffer) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        for (dk.dbc.batchexchange.dto.Diagnostic entryDiag : batchEntry.getDiagnostics()) {
            switch (entryDiag.getLevel()) {
                case ERROR:
                    diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, entryDiag.getMessage()));
                    dataBuffer.add(entryDiag.getMessage());
                    break;
                case WARNING:
                    diagnostics.add(new Diagnostic(Diagnostic.Level.WARNING, entryDiag.getMessage()));
                    dataBuffer.add(entryDiag.getMessage());
                    break;
                case OK:
                    dataBuffer.add(entryDiag.getMessage());
                    break;
                default:
                    throw new IllegalStateException("Unknown batch entry diagnostic level: " + entryDiag.getLevel());
            }
        }
        return diagnostics;
    }

    private void uploadChunk(Chunk chunk) throws SinkException {
        final JobStoreServiceConnector jobStoreServiceConnector = jobStoreServiceConnectorBean.getConnector();
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (Exception e) {
            final String message = String.format("Error in communication with job-store for chunk [%d, %d]",
                    chunk.getJobId(), chunk.getChunkId());
            logException(message, e);
            throw new SinkException(message, e);
        }
    }

    private void logException(String message, Exception e) {
        if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
            final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
            if (jobError != null) {
                message += ": job-store returned error '" + jobError.getDescription() + "'";
                LOGGER.error(message, e);
            }
        }
    }

    private static class ChunkItemDataBuffer {
        private final StringBuilder buffer = new StringBuilder("");

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
