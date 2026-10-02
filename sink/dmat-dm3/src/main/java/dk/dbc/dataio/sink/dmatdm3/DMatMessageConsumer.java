package dk.dbc.dataio.sink.dmatdm3;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.RecordData;
import dk.dbc.dmat.service.persistence.DMatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DMatMessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DMatMessageConsumer.class);
    private final DMatServiceConnector connector;
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    public DMatMessageConsumer(ServiceHub serviceHub, DMatServiceConnector connector) {
        super(serviceHub);
        this.connector = Objects.requireNonNull(connector, "DMAT connector");
    }

    /**
     * Upserts a successfully processed item's records into DMat, and passes any other item
     * through as ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered, so
     * that it counts towards the job's ignored items and advances no delivery watermark for a
     * record nothing was sent for.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        switch (item.getStatus()) {
            case FAILURE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        outcome(item)
                                .withStatus(ChunkItem.Status.IGNORE)
                                .withData("Failed by processor"));
            case IGNORE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        outcome(item)
                                .withStatus(ChunkItem.Status.IGNORE)
                                .withData("Ignored by processor"));
            default:
                return deliverToDMat(item, itemLabel(message));
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
     * Upserts the records of one item, reporting a record DMat did not accept as failed
     * <p>
     * A rejection is failed rather than retried. A failed item advances no delivery watermark,
     * leaving a later version of the record free to reach DMat.
     *
     * @param label names the item in the result lines and the log statements quoting it
     */
    private ItemDeliveryResult deliverToDMat(ChunkItem item, String label) {
        try {
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData(sendToDMat(getDMatDataRecords(item), label)));
        } catch (DMatSinkException e) {
            LOGGER.info("Processing of item {} failed with reason: {}", label, e.getMessage());
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.FAILURE)
                            .withData(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("An unexpected exception was thrown for item {}", label, e);
            DMatSinkMetrics.UNEXPECTED_EXCEPTIONS.counter().inc();
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.FAILURE)
                            .withDiagnostics(
                                    new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                            .withData(e.getMessage()));
        }
    }

    /**
     * Creates the delivering outcome item of a delivered item, without the status and data naming
     * what became of it
     */
    private ChunkItem outcome(ChunkItem item) {
        return new ChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
    }

    /**
     * Names the item being delivered as jobId.chunkId.itemId
     * <p>
     * Read off the message rather than off the item body, so that the label names the same
     * (jobId, chunkId, itemId) the watermark comparison and the delivery endpoint use.
     */
    private String itemLabel(ConsumedMessage message) {
        return String.format("%d.%d.%d",
                JMSHeader.jobId.getHeader(message, Integer.class),
                JMSHeader.chunkId.getHeader(message, Long.class),
                JMSHeader.itemId.getHeader(message, Short.class));
    }

    private List<RecordData> getDMatDataRecords(ChunkItem chunkItem)
            throws IOException, JSONBException {
        List<RecordData> dataRecords = new ArrayList<>();

        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        while (addiReader.hasNext()) {
            AddiRecord addiRecord = addiReader.next();
            RecordData recordData = RecordData.fromRaw(StringUtil.asString(addiRecord.getContentData()));
            dataRecords.add(recordData);
        }
        LOGGER.info("getDMatDataRecords: Received item with {} records ({})", dataRecords.size(),
                dataRecords.stream().map(RecordData::getId).collect(Collectors.joining(",")));

        return dataRecords;
    }

    private String sendToDMat(List<RecordData> dataRecords, String label) throws DMatSinkException {
        List<String> upsertedRecords = new ArrayList<>();

        for (RecordData recordData : dataRecords) {
            LOGGER.info("SendToDMat: record with id {} received", recordData.getId());

            // Check for obvious errors that would always make the record fail
            if (recordData.getDatestamp() == null || recordData.getDatestamp().isEmpty()) {
                LOGGER.info("Received record data without a datestamp");
                DMatSinkMetrics.SINK_FAILED_RECORDS.counter().inc();
                throw new DMatSinkException("Null or empty datestamp field. Record will fail");
            }
            if (recordData.getRecordReference() == null || recordData.getRecordReference().isEmpty()) {
                LOGGER.info("Received record data without a record reference");
                DMatSinkMetrics.SINK_FAILED_RECORDS.counter().inc();
                throw new DMatSinkException("Null or empty record reference field. Record will fail");
            }

            // Post new/updated record to DMat
            long handleChunkItemStartTime = System.currentTimeMillis();
            try {
                DMatRecord dMatRecord = connector.upsertRecord(recordData);
                DMatSinkMetrics.DMAT_SERVICE_REQUESTS_TIMER.timer().update(Duration.ofMillis(System.currentTimeMillis() - handleChunkItemStartTime));

                // Result. Status chunk/item id, record reference of processed record and
                // the records seqno. (id) and current status (after processing)
                String result = String.format("%s: %s@%s => seqno %d status %s", label,
                        recordData.getRecordReference(), recordData.getDatestamp(),
                        dMatRecord.getId(), dMatRecord.getStatus());
                LOGGER.info("SendToDMat: result = {}", result);
                upsertedRecords.add(result);
            } catch (JSONBException | DMatServiceConnectorException e) {
                throw new RuntimeException(e);
            }
        }
        return String.join("\n", upsertedRecords);
    }
}
