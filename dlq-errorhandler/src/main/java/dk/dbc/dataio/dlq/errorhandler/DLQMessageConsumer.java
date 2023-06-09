package dk.dbc.dataio.dlq.errorhandler;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * This message driven bean monitors the DMQ for dead chunks
 * ensuring that they are marked as completed with failures in
 * the underlying store
 */
public class DLQMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DLQMessageConsumer.class);
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private static final String QUEUE = DLQConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = DLQConfig.QUEUE.fqnAsAddress();

    JSONBContext jsonbContext = new JSONBContext();

    public DLQMessageConsumer(ServiceHub serviceHub) {
        super(serviceHub);
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
        initMetrics(PrometheusMetricRegistry.create());
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            LOGGER.info("Received dead message for chunk {} of type {} in job {}", chunk.getChunkId(), chunk.getType(), chunk.getJobId());
            Chunk deadChunk = createDeadChunk(chunk);
            jobStoreServiceConnector.addChunk(deadChunk, chunk.getJobId(), chunk.getChunkId());
        } catch (JSONBException je) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid %s type", consumedMessage.getMessageId(), consumedMessage.getMessagePayload()), je);
        } catch (Exception e) {
            throw new InvalidMessageException("Message<" + consumedMessage.getMessageId() +
                    "> with jobId/chunkId<" + JMSHeader.jobId.getHeader(consumedMessage, Integer.class) +
                    "/" + JMSHeader.chunkId.getHeader(consumedMessage, Long.class) + "> could not be updated in jobstore", e);
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

    private Chunk createDeadChunk(Chunk originatingChunk) {
        Chunk.Type chunkType = originatingChunk.getType() == Chunk.Type.PARTITIONED ? Chunk.Type.PROCESSED : Chunk.Type.DELIVERED;
        Chunk deadChunk = new Chunk(originatingChunk.getJobId(), originatingChunk.getChunkId(), chunkType);
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
