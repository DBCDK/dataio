package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyMessageConsumer.class);

    public DummyMessageConsumer(ServiceHub serviceHub) {
        super(serviceHub);
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk processedChunk = unmarshallPayload(consumedMessage);
        Chunk deliveredChunk = processPayload(processedChunk);
        if(true) throw new RuntimeException("test");
//        sendResultToJobStore(deliveredChunk);
    }

    @Override
    public String getQueue() {
        return SinkConfig.QUEUE.asString();
    }

    @Override
    public String getFilter() {
        return SinkConfig.MESSAGE_FILTER.asString();
    }

    Chunk processPayload(Chunk processedChunk) {
        Chunk deliveredChunk = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem item : processedChunk) {
                String trackingId = item.getTrackingId();
                DBCTrackedLogContext.setTrackingId(trackingId);
                // Set new-item-status to success if chunkResult-item was success - else set new-item-status to ignore:
                ChunkItem.Status status = item.getStatus() == ChunkItem.Status.SUCCESS ? ChunkItem.Status.SUCCESS : ChunkItem.Status.IGNORE;
                ChunkItem chunkItem = new ChunkItem().withId(item.getId()).withStatus(status).withTrackingId(item.getTrackingId()).withData("Set by DummySink").withType(ChunkItem.Type.STRING);
                deliveredChunk.insertItem(chunkItem);
                LOGGER.debug("Handled chunk item {} for chunk {} in job {}", chunkItem.getId(), processedChunk.getChunkId(), processedChunk.getJobId());
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return deliveredChunk;
    }
}
