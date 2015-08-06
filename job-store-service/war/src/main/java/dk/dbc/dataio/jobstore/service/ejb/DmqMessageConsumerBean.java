package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.nio.charset.StandardCharsets;

/**
 * This message driven bean monitors the DMQ for dead chunks
 * ensuring that they are marked as completed with failures in
 * the underlying store
 */
@MessageDriven
public class DmqMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmqMessageConsumerBean.class);

    @EJB
    PgJobStore jobStoreBean;

    JSONBContext jsonbContext = new JSONBContext();

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, JobStoreException {
        try {
            final ExternalChunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), ExternalChunk.class);
            LOGGER.info("Received dead message for chunk {} of type {} in job {}",
                    chunk.getChunkId(), chunk.getType(), chunk.getJobId());
            if (chunk.getType() == ExternalChunk.Type.PARTITIONED) {
                jobStoreBean.addChunk(createDeadChunk(ExternalChunk.Type.PROCESSED, chunk));
            }
            jobStoreBean.addChunk(createDeadChunk(ExternalChunk.Type.DELIVERED, chunk));
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid %s type",
                    consumedMessage.getMessageId(), consumedMessage.getMessagePayload()), e);
        }
    }

    private ExternalChunk createDeadChunk(ExternalChunk.Type chunkType, ExternalChunk originatingChunk) {
        final ExternalChunk deadChunk = new ExternalChunk(originatingChunk.getJobId(), originatingChunk.getChunkId(), chunkType);
        deadChunk.setEncoding(StandardCharsets.UTF_8);
        for (ChunkItem chunkItem : originatingChunk) {
            deadChunk.insertItem(new ChunkItem(chunkItem.getId(), StringUtil.asBytes(String.format(
                    "Item was failed due to dead %s chunk", originatingChunk.getType())), ChunkItem.Status.FAILURE));
        }
        return deadChunk;
    }
}
