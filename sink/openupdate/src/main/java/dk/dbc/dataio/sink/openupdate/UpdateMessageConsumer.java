package dk.dbc.dataio.sink.openupdate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class UpdateMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final OpenUpdateConfig openUpdateConfig;
    private final AddiRecordPreprocessor addiRecordPreprocessor;
    private final UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();
    static final Cache<Long, FlowBinder> cachedFlowBinders = CacheBuilder.newBuilder().maximumSize(10).expireAfterAccess(Duration.ofHours(1)).build();

    OpenUpdateSinkConfig config;
    OpenUpdateServiceConnector connector;

    public UpdateMessageConsumer(ServiceHub serviceHub, FlowStoreServiceConnector flowStoreServiceConnector, OpenUpdateConfig openUpdateConfig, AddiRecordPreprocessor addiRecordPreprocessor) {
        super(serviceHub);
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.openUpdateConfig = openUpdateConfig;
        this.addiRecordPreprocessor = addiRecordPreprocessor;
    }

    public UpdateMessageConsumer(ServiceHub serviceHub, FlowStoreServiceConnector flowStoreServiceConnector) {
        this(serviceHub, flowStoreServiceConnector, new OpenUpdateConfig(flowStoreServiceConnector), new AddiRecordPreprocessor());
    }

    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        String queueProvider = getQueueProvider(consumedMessage);
        LOGGER.debug("Using queue-provider {}", queueProvider);
        try {
            OpenUpdateSinkConfig sinkConfig = getConfig(consumedMessage);

            Chunk outcome = buildOutcomeFromProcessedChunk(chunk);
            try {
                for (ChunkItem chunkItem : chunk) {
                    DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                    ChunkItemProcessor chunkItemProcessor = new ChunkItemProcessor(chunkItem,
                            addiRecordPreprocessor, connector, updateRecordResultMarshaller,
                            new UpdateRecordErrorInterpreter(sinkConfig.getIgnoredValidationErrors()));

                    switch (chunkItem.getStatus()) {
                        case SUCCESS:
                            outcome.insertItem(chunkItemProcessor.processForQueueProvider(queueProvider));
                            break;
                        case FAILURE:
                            outcome.insertItem(
                                    ChunkItem.ignoredChunkItem()
                                            .withId(chunkItem.getId())
                                            .withTrackingId(chunkItem.getTrackingId())
                                            .withData("Failed by processor")
                                            .withType(ChunkItem.Type.STRING)
                                            .withEncoding(StandardCharsets.UTF_8));
                            break;
                        case IGNORE:
                            outcome.insertItem(
                                    ChunkItem.ignoredChunkItem()
                                            .withId(chunkItem.getId())
                                            .withTrackingId(chunkItem.getTrackingId())
                                            .withData("Ignored by processor")
                                            .withType(ChunkItem.Type.STRING)
                                            .withEncoding(StandardCharsets.UTF_8));
                            break;
                        default:
                            throw new RuntimeException("Unknown chunk item state: " + chunkItem.getStatus().name());
                    }
                }
            } finally {
                DBCTrackedLogContext.remove();
            }
            addOutcomeToJobStore(outcome);
        } catch (Exception any) {
            LOGGER.error("Caught unhandled exception: " + any.getMessage());
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

    private void addOutcomeToJobStore(Chunk outcome) {
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(outcome, outcome.getJobId(), outcome.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            throw new RuntimeException("Error in communication with job-store", e);
        }
    }

    private Chunk buildOutcomeFromProcessedChunk(Chunk processedChunk) {
        Chunk outcome = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        outcome.setEncoding(processedChunk.getEncoding());
        return outcome;
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
