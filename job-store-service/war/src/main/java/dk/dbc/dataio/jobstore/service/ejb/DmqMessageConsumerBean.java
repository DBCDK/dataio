package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.nio.charset.StandardCharsets;

/**
 * This message driven bean monitors the DMQ for dead chunks
 * ensuring that they are marked as completed with failures in
 * the underlying store
 */
@MessageDriven(name = "dmqListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/dmq"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis")
})
public class DmqMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmqMessageConsumerBean.class);

    @EJB
    PgJobStore jobStoreBean;
    @EJB
    JobSchedulerBean jobSchedulerBean;

    JSONBContext jsonbContext = new JSONBContext();

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, JobStoreException {
        try {
            final Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            LOGGER.info("Received dead message for chunk {} of type {} in job {}",
                    chunk.getChunkId(), chunk.getType(), chunk.getJobId());
            if (chunk.getType() == Chunk.Type.PARTITIONED) {
                final Chunk deadChunk = createDeadChunk(Chunk.Type.PROCESSED, chunk);
                jobSchedulerBean.chunkProcessingDone(deadChunk);
                jobStoreBean.addChunk(deadChunk);
            } else {
                final Chunk deadChunk = createDeadChunk(Chunk.Type.DELIVERED, chunk);
                jobSchedulerBean.chunkDeliveringDone(deadChunk);
                jobStoreBean.addChunk(deadChunk);
            }
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid %s type",
                    consumedMessage.getMessageId(), consumedMessage.getMessagePayload()), e);
        } catch (DuplicateChunkException e) {
            LOGGER.error("Jobstore DuplicateChunkException from DMQMessagebean:", e);
        }
    }

    private Chunk createDeadChunk(Chunk.Type chunkType, Chunk originatingChunk) {
        final Chunk deadChunk = new Chunk(originatingChunk.getJobId(), originatingChunk.getChunkId(), chunkType);
        deadChunk.setEncoding(StandardCharsets.UTF_8);
        for (ChunkItem chunkItem : originatingChunk) {
            deadChunk.insertItem(new ChunkItem()
                    .withId(chunkItem.getId())
                    .withData(StringUtil.asBytes(String.format(
                            "Item was failed due to dead %s chunk", originatingChunk.getType())))
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withType(originatingChunk.isTerminationChunk() ?
                            ChunkItem.Type.JOB_END : ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId()));
        }
        return deadChunk;
    }
}
