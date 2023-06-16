package dk.dbc.dataio.sink.dmat;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.RecordData;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);
    private final JobStoreServiceConnector jobStore;
    private final DMatServiceConnector connector;
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    public MessageConsumer(ServiceHub serviceHub, DMatServiceConnector connector) {
        super(serviceHub);
        jobStore = serviceHub.jobStoreServiceConnector;
        this.connector = connector;
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);

        // Give up if we have no dmat connector
        if (connector == null) {
            throw new RuntimeException("DMAT Connector is uninitialized");
        }

        // Process all chunks
        Chunk deliveredChunk = handleChunk(chunk);

        // Store delivered chunks
        try {
            jobStore.addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            String message = String.format("Error in communication with job-store for chunk %d/%d", deliveredChunk.getJobId(), deliveredChunk.getChunkId());
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    message += ": job-store returned error '" + jobError.getDescription() + "'";
                }
            }
            LOGGER.error(message);
            throw new RuntimeException(message, e);
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

    Chunk handleChunk(Chunk chunk) {
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                String id = String.join(".",
                        Long.toString(chunk.getJobId()),
                        Long.toString(chunk.getChunkId()),
                        Long.toString(chunkItem.getId()));
                result.insertItem(handleChunkItem(chunkItem, id));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem, String id) {
        ChunkItem result = new ChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);

        try {
            switch (chunkItem.getStatus()) {
                case FAILURE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Failed by processor");
                case IGNORE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Ignored by processor");
                default:
                    try {
                        return result
                                .withStatus(ChunkItem.Status.SUCCESS)
                                .withData(sendToDMat(getDMatDataRecords(chunkItem), id));
                    } catch (DMatSinkException e) {
                        LOGGER.info("Processing of chunk id {} failed with reason: {}", id, e.getMessage());
                        return result
                                .withStatus(ChunkItem.Status.FAILURE)
                                .withData(e.getMessage());
                    } // Other exceptions bubbles out to the outer try-catch
            }
        } catch (Exception e) {
            LOGGER.error("An unexpected exception was thrown for chunk id {}: {}", id, e.getMessage());
            DMatSinkMetrics.UNEXPECTED_EXCEPTIONS.counter().inc();
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(
                            new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
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
        LOGGER.info("getDMatDataRecords: Received chunkitem with {} records ({})", dataRecords.size(),
                dataRecords.stream().map(RecordData::getId).collect(Collectors.joining(",")));

        return dataRecords;
    }

    private String sendToDMat(List<RecordData> dataRecords, String id) throws DMatSinkException {
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
                DMatSinkMetrics.DMAT_SERVICE_REQUESTS_TIMER.simpleTimer().update(Duration.ofMillis(System.currentTimeMillis() - handleChunkItemStartTime));

                // Result. Status chunk/item id, record reference of processed record and
                // the records seqno. (id) and current status (after processing)
                String result = String.format("%s: %s@%s => seqno %d status %s", id,
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
