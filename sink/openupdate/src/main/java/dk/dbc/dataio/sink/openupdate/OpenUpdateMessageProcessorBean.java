package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.openupdate.metrics.CounterMetrics;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

@MessageDriven(name = "openUpdateListener", activationConfig = {
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "resource = '${ENV=MESSAGE_NAME_FILTER}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true")
})
public class OpenUpdateMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateMessageProcessorBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @EJB
    OpenUpdateConfigBean openUpdateConfigBean;

    @Inject
    MetricsHandlerBean metricsHandler;

    AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();
    Cache<Long, FlowBinder> cachedFlowBinders = CacheManager.createLRUCache(10);

    OpenUpdateSinkConfig config;
    OpenUpdateServiceConnector connector;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException, NullPointerException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        final String queueProvider = getQueueProvider(consumedMessage);
        LOGGER.debug("Using queue-provider {}", queueProvider);

        try {

            final OpenUpdateSinkConfig latestConfig = openUpdateConfigBean.getConfig(consumedMessage);
            if (!latestConfig.equals(config)) {
                LOGGER.debug("Updating connector");
                connector = getOpenUpdateServiceConnector(latestConfig);
                config = latestConfig;
            }

            final Chunk outcome = buildOutcomeFromProcessedChunk(chunk);
            try {
                for (ChunkItem chunkItem : chunk) {
                    DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                    LOGGER.info("Handling item {}/{}/{}", chunk.getJobId(), chunk.getChunkId(), chunkItem.getId());
                    final ChunkItemProcessor chunkItemProcessor = new ChunkItemProcessor(chunkItem,
                            addiRecordPreprocessor, connector, updateRecordResultMarshaller,
                            new UpdateRecordErrorInterpreter(config.getIgnoredValidationErrors()), metricsHandler);

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
                            throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
                    }
                }
            } finally {
                DBCTrackedLogContext.remove();
            }
            addOutcomeToJobStore(outcome);

            metricsHandler.increment(CounterMetrics.CHUNK_ITEMS, chunk.size(),
                    new Tag("queueProvider", queueProvider));

        } catch (Exception any) {
            LOGGER.error("Caught unhandled exception: " + any.getMessage());
            metricsHandler.increment(CounterMetrics.UNHANDLED_EXCEPTIONS,
                    new Tag("queueProvider", queueProvider));
            throw any;
        }
    }

    private OpenUpdateServiceConnector getOpenUpdateServiceConnector(OpenUpdateSinkConfig config) {
        return new OpenUpdateServiceConnector(
                config.getEndpoint(),
                config.getUserId(),
                config.getPassword());
    }

    private void addOutcomeToJobStore(Chunk outcome) throws SinkException {
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(outcome, outcome.getJobId(), outcome.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            logJobStoreError(e);
            // Throw SinkException to force transaction rollback
            throw new SinkException("Error in communication with job-store", e);
        }
    }

    private void logJobStoreError(JobStoreServiceConnectorException e) {
        if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
            final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
            if (jobError != null) {
                LOGGER.error("job-store returned error: {}", jobError.getDescription());
            }
        }
    }

    private Chunk buildOutcomeFromProcessedChunk(Chunk processedChunk) {
        final Chunk outcome = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        outcome.setEncoding(processedChunk.getEncoding());
        return outcome;
    }

    private String getQueueProvider(ConsumedMessage message) throws SinkException {
        try {
            final long flowBinderIdFromMessage = message.getHeaderValue(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME, Long.class);
            final long flowBinderVersionFromMessage = message.getHeaderValue(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME, Long.class);
            FlowBinder flowBinder = cachedFlowBinders.get(flowBinderIdFromMessage);
            if (flowBinder == null || flowBinder.getVersion() < flowBinderVersionFromMessage) {
                flowBinder = flowStoreServiceConnectorBean.getConnector().getFlowBinder(flowBinderIdFromMessage);
                LOGGER.info("Caching version {} of flow-binder {}",
                        flowBinder.getVersion(), flowBinder.getContent().getName());
                cachedFlowBinders.put(flowBinderIdFromMessage, flowBinder);
            }
            return flowBinder.getContent().getQueueProvider();
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }
}
