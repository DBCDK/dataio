package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class UpdateMessageConsumer extends SinkMessageConsumerAdapter {
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

    /**
     * Sends a successfully processed item to the update service, and passes any other item through
     * as ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered, so
     * that it counts towards the job's ignored items and advances no delivery watermark for a
     * record nothing was sent for.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        OpenUpdateSinkConfig sinkConfig = getConfig(message);
        switch (item.getStatus()) {
            case SUCCESS:
                return new ChunkItemProcessor(connector, sinkConfig).process(item);
            case FAILURE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        passedThroughItem(item, "Failed by processor"));
            case IGNORE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        passedThroughItem(item, "Ignored by processor"));
            default:
                throw new RuntimeException("Unknown chunk item status: " + item.getStatus().name());
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

    private ChunkItem passedThroughItem(ChunkItem item, String reason) {
        return ChunkItem.ignoredChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withData(reason)
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
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
