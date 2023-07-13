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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BatchExchangeMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchExchangeMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final EntityManager entityManager;

    public BatchExchangeMessageConsumer(ServiceHub serviceHub, EntityManager entityManager) {
        super(serviceHub);
        this.entityManager = entityManager;
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Batch batch = createBatch(chunk);
            LOGGER.info("Adding chunk {}/{} to batch {}", chunk.getJobId(), chunk.getChunkId(), batch);
            for (ChunkItem chunkItem : chunk) {
                String trackingId = getTrackingId(chunkItem, batch);
                DBCTrackedLogContext.setTrackingId(trackingId);
                createBatchEntries(chunkItem).forEach(entry ->
                                entityManager.persist(entry
                                        .withBatch(batch.getId())
                                        .withTrackingId(trackingId)
                                        .withPriority(consumedMessage.getPriority().getValue())));

                LOGGER.info("Adding chunk item {} to batch {}", chunkItem.getId(), batch.getId());
            }
            completeIfBatchHasNoPendingEntries(batch);
            transaction.commit();
        } finally {
            DBCTrackedLogContext.remove();
            if(transaction.isActive()) transaction.rollback();
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

    private Batch createBatch(Chunk chunk) {
        Batch batch = new Batch().withName(BatchName.fromChunk(chunk).toString());
        entityManager.persist(batch);
        entityManager.flush();
        entityManager.refresh(batch);
        return batch;
    }

    private List<BatchEntry> createBatchEntries(ChunkItem chunkItem) throws InvalidMessageException {
        List<BatchEntry> entries = new ArrayList<>();
        switch (chunkItem.getStatus()) {
            case SUCCESS:
                try {
                    AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
                    while (addiReader.hasNext()) {
                        BatchEntry entry = createPendingBatchEntry(addiReader.next());
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
                throw new InvalidMessageException("Unknown chunk item state: " + chunkItem.getStatus().name());
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
                .withTimeOfCompletion(new Timestamp(new Date().getTime()))
                .withStatus(BatchEntry.Status.FAILED)
                .withContent(StringUtil.asBytes(StringUtil.getStackTraceString(cause)))
                .withDiagnostics(Collections.singletonList(Diagnostic.createError(cause.getMessage())));
    }

    private BatchEntry createIgnoredBatchEntry(String reason) {
        return new BatchEntry()
                .withTimeOfCompletion(new Timestamp(new Date().getTime()))
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
