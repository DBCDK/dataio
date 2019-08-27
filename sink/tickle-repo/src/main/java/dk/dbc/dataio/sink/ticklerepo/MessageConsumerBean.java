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
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@MessageDriven
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

    // cached mappings of job-ID to Batch
    Cache<Long, Batch> batchCache = CacheManager.createLRUCache(50);

    @Override
    @Stopwatch
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, ServiceException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Handling chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        final Batch batch = getBatch(chunk);

        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        if (chunk.isJobEnd()) {
            result.insertItem(handleJobEnd(chunk.getItems().get(0), batch));
        } else {
            chunk.getItems().forEach(
                    chunkItem -> result.insertItem(handleChunkItem(chunkItem, batch)));
        }

        uploadChunk(result);
    }

    private Batch getBatch(Chunk chunk) {
        if (batchCache.containsKey(chunk.getJobId())) {
            // we already have the batch cached
            return batchCache.get(chunk.getJobId());
        }

        final Optional<Batch> batch = tickleRepo.lookupBatch(new Batch().withBatchKey((int) chunk.getJobId()));
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
                    .withBatchKey((int) chunk.getJobId())
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
                .withType(ChunkItem.Type.STRING)
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
                    return putInTickleBatch(batch, chunkItem);
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
