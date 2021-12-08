package dk.dbc.dataio.sink.dmat;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobError;

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
import java.nio.charset.StandardCharsets;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Timed
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        final Chunk deliveredChunk = handleChunk(chunk);
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
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
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData("{}");  // Todo: Handle incoming chunk data
            }
        } catch (Exception e) { // Todo: If possible, replace with more specific exceptions
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(
                            new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
    }
}
