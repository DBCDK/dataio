package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;

@MessageDriven
public class DummyMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyMessageProcessorBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final Chunk processedChunk = unmarshallPayload(consumedMessage);
        final Chunk deliveredChunk = processPayload(processedChunk);
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new EJBException(e);
        }
    }

    Chunk processPayload(Chunk processedChunk) {
        final Chunk deliveredChunk = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (final ChunkItem item : processedChunk) {
                final String trackingId = item.getTrackingId();
                DBCTrackedLogContext.setTrackingId(trackingId);
                // Set new-item-status to success if chunkResult-item was success - else set new-item-status to ignore:
                ChunkItem.Status status = item.getStatus() == ChunkItem.Status.SUCCESS ? ChunkItem.Status.SUCCESS : ChunkItem.Status.IGNORE;
                ChunkItem chunkItem = new ChunkItem()
                    .withId(item.getId())
                    .withStatus(status)
                    .withTrackingId(item.getTrackingId())
                    .withData("Set by DummySink")
                    .withType(ChunkItem.Type.STRING);
                deliveredChunk.insertItem(chunkItem);
                LOGGER.debug("Handled chunk item {} for chunk {} in job {}",
                        chunkItem.getId(), processedChunk.getChunkId(),processedChunk.getJobId());
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return deliveredChunk;
    }
}
