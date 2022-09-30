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

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@MessageDriven(name = "batchListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "resource = '${ENV=MESSAGE_NAME_FILTER}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true")
})
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
                for (ChunkItem chunkItem : chunk) {
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

        } catch (Exception any) {
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
