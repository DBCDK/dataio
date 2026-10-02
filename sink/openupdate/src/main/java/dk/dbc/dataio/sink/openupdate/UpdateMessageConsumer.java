package dk.dbc.dataio.sink.openupdate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class UpdateMessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final OpenUpdateConfig openUpdateConfig;
    private final AddiRecordPreprocessor addiRecordPreprocessor;
    private final UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();
    static final Cache<Long, FlowBinder> cachedFlowBinders = CacheBuilder.newBuilder().maximumSize(10).expireAfterAccess(Duration.ofHours(1)).build();

    OpenUpdateSinkConfig config;
    OpenUpdateServiceConnector connector;

    public UpdateMessageConsumer(ServiceHub serviceHub, FlowStoreServiceConnector flowStoreServiceConnector, OpenUpdateConfig openUpdateConfig, AddiRecordPreprocessor addiRecordPreprocessor) {
        super(serviceHub);
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.openUpdateConfig = openUpdateConfig;
        this.addiRecordPreprocessor = addiRecordPreprocessor;
    }

    public UpdateMessageConsumer(ServiceHub serviceHub, FlowStoreServiceConnector flowStoreServiceConnector) {
        this(serviceHub, flowStoreServiceConnector, new OpenUpdateConfig(flowStoreServiceConnector), new AddiRecordPreprocessor());
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
                return deliverToUpdateService(message, item, sinkConfig);
            case FAILURE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        passedThroughItem(item, "Failed by processor"));
            case IGNORE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        passedThroughItem(item, "Ignored by processor"));
            default:
                throw new RuntimeException("Unknown chunk item state: " + item.getStatus().name());
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

    private ItemDeliveryResult deliverToUpdateService(ConsumedMessage message, ChunkItem item,
                                                     OpenUpdateSinkConfig sinkConfig) {
        String queueProvider = getQueueProvider(message);
        LOGGER.debug("Using queue-provider {}", queueProvider);
        ChunkItemProcessor chunkItemProcessor = new ChunkItemProcessor(item, addiRecordPreprocessor,
                connector, updateRecordResultMarshaller,
                new UpdateRecordErrorInterpreter(sinkConfig.getIgnoredValidationErrors()));
        return chunkItemProcessor.processForQueueProvider(queueProvider);
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
        OpenUpdateSinkConfig latestConfig = openUpdateConfig.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating connector");
            connector = getOpenUpdateServiceConnector(latestConfig);
            config = latestConfig;
        }
        return config;
    }

    private OpenUpdateServiceConnector getOpenUpdateServiceConnector(OpenUpdateSinkConfig config) {
        return new OpenUpdateServiceConnector(config.getEndpoint(), config.getUserId(), config.getPassword());
    }

    private String getQueueProvider(ConsumedMessage message) {
        try {
            long flowBinderIdFromMessage = JMSHeader.flowBinderId.getHeader(message, Long.class);
            long flowBinderVersionFromMessage = JMSHeader.flowBinderVersion.getHeader(message, Long.class);
            FlowBinder flowBinder = cachedFlowBinders.getIfPresent(flowBinderIdFromMessage);
            if (flowBinder == null || flowBinder.getVersion() < flowBinderVersionFromMessage) {
                flowBinder = flowStoreServiceConnector.getFlowBinder(flowBinderIdFromMessage);
                LOGGER.info("Caching version {} of flow-binder {}", flowBinder.getVersion(), flowBinder.getContent().getName());
                cachedFlowBinders.put(flowBinderIdFromMessage, flowBinder);
            }
            return flowBinder.getContent().getQueueProvider();
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
