package dk.dbc.dataio.sink.dmat;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.JobError;

import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.RecordData;
import dk.dbc.dmat.service.persistence.DMatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.util.Timed;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    private final JSONBContext jsonbContext = new JSONBContext();

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Inject
    DMatServiceConnector connector;

    @Timed
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        // Give up if we have no dmat connector
        if (connector == null) {
            LOGGER.error("Connector of DMat sink is uninitialized!");
            throw new SinkException("Connector is uninitialized");
        }

        // Process all chunks
        final Chunk deliveredChunk = handleChunk(chunk);

        // Store delivered chunks
        try {
            jobStoreServiceConnectorBean.getConnector()
                    .addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            String message = String.format("Error in communication with job-store for chunk %d/%d",
                    deliveredChunk.getJobId(), deliveredChunk.getChunkId());
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    message += ": job-store returned error '" + jobError.getDescription() + "'";
                }
            }
            throw new SinkException(message, e);
        }
    }

    Chunk handleChunk(Chunk chunk) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                final String id = String.join(".",
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
        final ChunkItem result = new ChunkItem()
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
                    } catch (DMatServiceConnectorException e) {
                        LOGGER.info("DMat connector threw an DMatServiceConnectorException for chunk id {}: {}", id, e.getMessage());
                        return result
                                .withStatus(ChunkItem.Status.FAILURE)
                                .withData(e.getMessage());
                    } catch (DMatSinkException e) {
                        LOGGER.info("Processing of chunk id {} failed with reason: {}", id, e.getMessage());
                        return result
                                .withStatus(ChunkItem.Status.FAILURE)
                                .withData(e.getMessage());
                    } catch (Exception e) {
                        LOGGER.error("An unexpected exception was thrown for chunk id {}: {}", id, e.getMessage());
                        return result
                                .withStatus(ChunkItem.Status.FAILURE)
                                .withData(e.getMessage());
                    }
            }
        } catch (Exception e) {
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(
                            new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
    }

    private List<RecordData> getDMatDataRecords(ChunkItem chunkItem)
            throws IOException, JSONBException {
        final List<RecordData> dataRecords = new ArrayList<>();

        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        int cnt = 1;
        while (addiReader.hasNext()) {
            final AddiRecord addiRecord = addiReader.next();
            final RecordData recordData = jsonbContext.unmarshall(
                            StringUtil.asString(addiRecord.getContentData()), RecordData.class);
            dataRecords.add(recordData);
        }

        return dataRecords;
    }

    private String sendToDMat(List<RecordData> dataRecords, String id) throws Exception {
        List<String> upsertedRecords = new ArrayList<>();

        for (RecordData recordData : dataRecords) {

            // Check for obvious errors that would always make the record fail
            if (recordData.getDatestamp() == null || recordData.getDatestamp().isEmpty()) {
                throw new DMatSinkException("Null or empty datestamp field. Record will fail");
            }
            if (recordData.getRecordReference() == null || recordData.getRecordReference().isEmpty()) {
                throw new DMatSinkException("Null or empty record reference field. Record will fail");
            }

            // Post new/updated record to DMat
            DMatRecord dMatRecord = connector.upsertRecord(recordData);

            // Result. Status chunk/item id, record reference of processsed record and
            // the records seqno. (id) and current status (after processing)
            upsertedRecords.add(String.format("%s: %s@%s => seqno %d status %s", id,
                            recordData.getRecordReference(), recordData.getDatestamp(),
                            dMatRecord.getId(), dMatRecord.getStatus()));
        }
        return upsertedRecords.stream().collect(Collectors.joining("\n"));
    }
}
