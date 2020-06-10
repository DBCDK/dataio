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
import dk.dbc.batchexchange.dto.Diagnostic;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.MetaData;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @PersistenceContext(unitName = "batchExchangePU")
    EntityManager entityManager;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    static final Metadata exceptionCounterMetadata = Metadata.builder()
            .withName("dataio_sink_batch_exchange_message_consumer_exception_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build();

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, ServiceException {
        try {

            final Chunk chunk = unmarshallPayload(consumedMessage);
            LOGGER.info("Handling chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

            final Batch batch = createBatch(chunk);
            LOGGER.info("Adding chunk {}/{} to batch {}", chunk.getJobId(), chunk.getChunkId(), batch);

            try {
                for(ChunkItem chunkItem : chunk) {
                    final String trackingId = getTrackingId(chunkItem, batch);
                    DBCTrackedLogContext.setTrackingId(trackingId);

                    createBatchEntries(chunkItem)
                            .forEach(entry ->
                                    entityManager.persist(entry
                                            .withBatch(batch.getId())
                                            .withTrackingId(trackingId)
                                            .withPriority(consumedMessage.getPriority().getValue())));

                    LOGGER.info("Adding chunk item {} to batch {}", chunkItem.getId(), batch.getId());
                }
                completeIfBatchHasNoPendingEntries(batch);
            } finally {
                DBCTrackedLogContext.remove();
            }

        } catch( Exception any ) {
            LOGGER.error("Caught unhandled exception: " + any.getMessage());
            metricRegistry.counter(exceptionCounterMetadata).inc();
            throw any;
        }
    }

    private Batch createBatch(Chunk chunk) {
        final Batch batch = new Batch()
                .withName(BatchName.fromChunk(chunk).toString());
        entityManager.persist(batch);
        entityManager.flush();
        entityManager.refresh(batch);
        return batch;
    }

    private List<BatchEntry> createBatchEntries(ChunkItem chunkItem) throws SinkException {
        final List<BatchEntry> entries = new ArrayList<>();
        switch (chunkItem.getStatus()) {
            case SUCCESS:
                try {
                    final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
                    while (addiReader.hasNext()) {
                        final BatchEntry entry = createPendingBatchEntry(addiReader.next());
                        if (addiReader.hasNext()) {
                            entry.withIsContinued(true);
                        }
                        entries.add(entry);
                    }
                } catch (RuntimeException | IOException e) {
                    entries.add(createFailedBatchEntry(e));
                }
                break;
            case FAILURE:
                entries.add(createIgnoredBatchEntry("Failed by processor"));
                break;
            case IGNORE:
                entries.add(createIgnoredBatchEntry("Ignored by processor"));
                break;
            default:
                throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
        }
        return entries;
    }

    private BatchEntry createPendingBatchEntry(AddiRecord addiRecord) throws IOException {
        return new BatchEntry()
                .withContent(addiRecord.getContentData())
                .withMetadata(MetaData.fromXml(addiRecord.getMetaData()).getInfoJson());
    }

    private BatchEntry createFailedBatchEntry(Exception cause) {
        return new BatchEntry()
                .withStatus(BatchEntry.Status.FAILED)
                .withContent(StringUtil.asBytes(StringUtil.getStackTraceString(cause)))
                .withDiagnostics(Collections.singletonList(Diagnostic.createError(cause.getMessage())));
    }

    private BatchEntry createIgnoredBatchEntry(String reason) {
        return new BatchEntry()
                .withStatus(BatchEntry.Status.IGNORED)
                .withContent(StringUtil.asBytes(reason))
                .withDiagnostics(Collections.singletonList(Diagnostic.createOk(reason)));
    }

    private String getTrackingId(ChunkItem chunkItem, Batch batch) {
        String trackingId = chunkItem.getTrackingId();
        if (trackingId == null) {
            trackingId = String.format("io:%s-%d", batch.getName(), chunkItem.getId());
        }
        return trackingId;
    }

    private void completeIfBatchHasNoPendingEntries(Batch batch) {
        entityManager.flush();
        entityManager.refresh(batch);
        if (batch.getIncompleteEntries() == 0) {
            batch.withStatus(Batch.Status.COMPLETED);
        }
    }
}
