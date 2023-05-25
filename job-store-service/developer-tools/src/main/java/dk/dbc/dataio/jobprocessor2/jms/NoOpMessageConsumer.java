package dk.dbc.dataio.jobprocessor2.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class NoOpMessageConsumer extends  JobStoreMessageConsumer {
    Logger LOGGER = LoggerFactory.getLogger(NoOpMessageConsumer.class);
    public NoOpMessageConsumer(ServiceHub serviceHub) {
        super(serviceHub);
        LOGGER.info("Starting consumer.\nConsuming from '{}'", System.getenv()
                .getOrDefault("QUEUE", ""));
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            Chunk chunk = MAPPER.readValue(consumedMessage.getMessagePayload(), Chunk.class);
            LOGGER.info("Received chunk {} for job {} (returned with nodata items)", chunk.getChunkId(), chunk.getJobId());
            List<ChunkItem> items = chunk.getItems().stream()
                    .map(chunkItem -> chunkItem.withData("(no data: just testing)".getBytes()))
                    .collect(Collectors.toList());
            Chunk.Type stage = Chunk.Type.valueOf(System.getenv().getOrDefault("STAGE", "PROCESSED"));
            Chunk done = new Chunk(chunk.getJobId(), chunk.getChunkId(), stage);
            done.addAllItems(items, items);
            sendResultToJobStore(done);
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), JMSHeader.payload.getHeader(consumedMessage, String.class)), e);
        }
    }
}
