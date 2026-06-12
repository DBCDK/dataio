package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnector;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class UpdateMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    private final ConfigRefresher configRefresher;

    OpenUpdateSinkConfig config;
    UpdateServiceConnector connector;

    public UpdateMessageConsumer(ServiceHub serviceHub, FlowStoreServiceConnector flowStoreServiceConnector) {
        this(serviceHub, new ConfigRefresher(flowStoreServiceConnector));
    }

    public UpdateMessageConsumer(ServiceHub serviceHub, ConfigRefresher configRefresher) {
        super(serviceHub);
        this.configRefresher = configRefresher;
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        try {
            OpenUpdateSinkConfig sinkConfig = getConfig(consumedMessage);
            Chunk outcome = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
            ChunkItemProcessor chunkItemProcessor = new ChunkItemProcessor(connector, sinkConfig);
            try {
                for (ChunkItem chunkItem : chunk) {
                    DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                    switch (chunkItem.getStatus()) {
                        case SUCCESS:
                            outcome.insertItem(chunkItemProcessor.process(chunkItem));
                            break;
                        case FAILURE:
                            outcome.insertItem(ChunkItem.ignoredChunkItem()
                                    .withId(chunkItem.getId())
                                    .withTrackingId(chunkItem.getTrackingId())
                                    .withData("Failed by processor")
                                    .withType(ChunkItem.Type.STRING)
                                    .withEncoding(StandardCharsets.UTF_8));
                            break;
                        case IGNORE:
                            outcome.insertItem(ChunkItem.ignoredChunkItem()
                                    .withId(chunkItem.getId())
                                    .withTrackingId(chunkItem.getTrackingId())
                                    .withData("Ignored by processor")
                                    .withType(ChunkItem.Type.STRING)
                                    .withEncoding(StandardCharsets.UTF_8));
                            break;
                        default:
                            throw new RuntimeException("Unknown chunk item status: " + chunkItem.getStatus().name());
                    }
                }
            } finally {
                DBCTrackedLogContext.remove();
            }
            sendResultToJobStore(outcome);
        } catch (Exception any) {
            LOGGER.error("Caught unhandled exception: {}", any.getMessage());
            throw any;
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

    private synchronized OpenUpdateSinkConfig getConfig(ConsumedMessage consumedMessage) {
        OpenUpdateSinkConfig latestConfig = configRefresher.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating connector for new config");
            connector = new UpdateServiceConnector(latestConfig.getEndpoint());
            config = latestConfig;
        }
        return config;
    }
}
