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
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Both payload types reach this queue, since it is where every queue dead letters to:
     * whole chunks from the processors, and individual items from the sinks. They are told
     * apart by the payload header rather than by trying to parse the body as a chunk and
     * falling back, which would report a genuinely malformed chunk as an item.
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        if (JMSHeader.ITEM_PAYLOAD_TYPE.equals(JMSHeader.payload.getHeader(consumedMessage, String.class))) {
            handleDeadItem(consumedMessage);
        } else {
            handleDeadChunk(consumedMessage);
        }
    }

    private void handleDeadChunk(ConsumedMessage consumedMessage) throws InvalidMessageException {
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

    /**
     * Fails a single dead item, so that its job can complete
     * <p>
     * Under per item delivery a sink reports the outcome of every item it is sent, and the
     * job-store phase counters, and with them job completion, wait for those reports. An
     * item that ends up here has exhausted its delivery attempts and no sink will ever
     * report it, so without this the job would never complete and nothing would say why.
     * <p>
     * The result names whatever watermark row the item belongs to, as every result does.
     * A failure never advances one, but that is the delivery endpoint's rule to apply, and
     * a null key here would mean the item has no record identity at all.
     */
    private void handleDeadItem(ConsumedMessage consumedMessage) throws InvalidMessageException {
        int jobId = JMSHeader.jobId.getHeader(consumedMessage, Integer.class);
        long chunkId = JMSHeader.chunkId.getHeader(consumedMessage, Long.class);
        short itemId = JMSHeader.itemId.getHeader(consumedMessage, Short.class);
        try {
            ChunkItem item = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), ChunkItem.class);
            LOGGER.info("Received dead message for item {} of chunk {} in job {}", itemId, chunkId, jobId);
            ItemDeliveryResult deliveryResult = ItemDeliveryResult
                    .of(ItemDeliveryResult.Status.FAILED, createDeadItem(item, itemId))
                    .withWatermarkKey(JMSHeader.sinkId.getHeader(consumedMessage, Long.class),
                            JMSHeader.recordKey.getHeader(consumedMessage, String.class));
            jobStoreServiceConnector.addItemDelivered(deliveryResult, jobId, (int) chunkId, itemId);
        } catch (JSONBException je) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid %s type", consumedMessage.getMessageId(), consumedMessage.getMessagePayload()), je);
        } catch (Exception e) {
            throw new InvalidMessageException("Message<" + consumedMessage.getMessageId() +
                    "> with jobId/chunkId/itemId<" + jobId + "/" + chunkId + "/" + itemId +
                    "> could not be updated in jobstore", e);
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

    private ChunkItem createDeadItem(ChunkItem originatingItem, short itemId) {
        return new ChunkItem()
                .withId(itemId)
                .withData(StringUtil.asBytes("Item was failed due to dead item message"))
                .withStatus(ChunkItem.Status.FAILURE)
                .withType(isTerminationItem(originatingItem) ? ChunkItem.Type.JOB_END : ChunkItem.Type.STRING)
                .withTrackingId(originatingItem.getTrackingId());
    }

    /**
     * Recognizes the job termination item the same way {@link Chunk#isTerminationChunk()}
     * recognizes the chunk carrying it, so that a dead termination item still reaches the
     * job-store handling that marks its job as fatally failed.
     */
    private boolean isTerminationItem(ChunkItem item) {
        return item.isTyped() && item.getType().getFirst() == ChunkItem.Type.JOB_END;
    }

    private Chunk createDeadChunk(Chunk originatingChunk) {
        Chunk.Type chunkType = originatingChunk.getType() == Chunk.Type.PARTITIONED ? Chunk.Type.PROCESSED : Chunk.Type.DELIVERED;
        Chunk deadChunk = new Chunk(originatingChunk.getJobId(), originatingChunk.getChunkId(), chunkType);
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
