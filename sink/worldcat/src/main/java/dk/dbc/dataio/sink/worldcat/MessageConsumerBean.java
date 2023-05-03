package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

@MessageDriven(name = "worldcatListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "resource = '${ENV=MESSAGE_NAME_FILTER}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryBackOffMultiplier", propertyValue = "2"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryBackOffMultiplier", propertyValue = "4"),
        @ActivationConfigProperty(propertyName = "maximumRedeliveries", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "MaxSession", propertyValue = "6")
})
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @EJB
    WorldCatConfigBean worldCatConfigBean;
    @EJB
    OcnRepo ocnRepo;

    WorldCatSinkConfig config;
    WciruServiceConnector connector;
    WciruServiceBroker wciruServiceBroker;

    @Inject
    MetricsHandlerBean metricsHandler;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, NullPointerException, ServiceException {
        try {
            final Chunk chunk = unmarshallPayload(consumedMessage);
            LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

            refreshConfigIfOutdated(consumedMessage);

            final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
            try {
                Instant chunkStart = Instant.now();
                for (ChunkItem chunkItem : chunk.getItems()) {
                    DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                    switch (chunkItem.getStatus()) {
                        case FAILURE:
                            result.insertItem(ChunkItem.ignoredChunkItem()
                                    .withId(chunkItem.getId())
                                    .withTrackingId(chunkItem.getTrackingId())
                                    .withType(ChunkItem.Type.STRING)
                                    .withEncoding(StandardCharsets.UTF_8)
                                    .withData("Failed by job-processor"));
                            break;
                        case IGNORE:
                            result.insertItem(ChunkItem.ignoredChunkItem()
                                    .withId(chunkItem.getId())
                                    .withTrackingId(chunkItem.getTrackingId())
                                    .withType(ChunkItem.Type.STRING)
                                    .withEncoding(StandardCharsets.UTF_8)
                                    .withData("Ignored by job-processor"));
                            break;
                        default:
                            result.insertItem(handleChunkItem(chunkItem));
                    }
                }
                Duration duration = Duration.between(chunkStart, Instant.now());
                metricsHandler.update(WorldcatTimerMetrics.WCIRU_CHUNK_UPDATE, duration);
                metricsHandler.increment(WorldcatCounterMetrics.WCIRU_CHUNK_UPDATE);
                LOGGER.info("{} upload to worldcat took {}", chunk, duration);
            } finally {
                DBCTrackedLogContext.remove();
            }
            Instant start = Instant.now();
            uploadChunk(result);
            LOGGER.info("Upload {} to jobstore took {}", result, Duration.between(start, Instant.now()));
        } catch (Exception e) {
            LOGGER.error("Caught unhandled exception while processing jobId: {}, chunkId: {}", JMSHeader.jobId.getHeader(consumedMessage, Integer.class), JMSHeader.chunkId.getHeader(consumedMessage, Long.class), e);
            metricsHandler.increment(WorldcatCounterMetrics.UNHANDLED_EXCEPTIONS);
            throw e;
        }
    }

    private void refreshConfigIfOutdated(ConsumedMessage consumedMessage) throws SinkException {
        final WorldCatSinkConfig latestConfig = worldCatConfigBean.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating WCIRU connector");
            connector = getWciruServiceConnector(latestConfig);
            wciruServiceBroker = new WciruServiceBroker(connector);
            config = latestConfig;
        }
    }

    private WciruServiceConnector getWciruServiceConnector(WorldCatSinkConfig config) {
        final WciruServiceConnector.RetryScheme retryScheme = new WciruServiceConnector.RetryScheme(
                1,              // maxNumberOfRetries
                1000, // milliSecondsToSleepBetweenRetries
                new HashSet<>(config.getRetryDiagnostics()));

        return new WciruServiceConnector(
                config.getEndpoint(),
                config.getUserId(),
                config.getPassword(),
                config.getProjectId(),
                retryScheme);
    }

    ChunkItem handleChunkItem(ChunkItem chunkItem) {
        try {
            final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                    ChunkItemWithWorldCatAttributes.of(chunkItem);
            final Pid pid = Pid.of(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getPid());
            final WorldCatEntity worldCatEntity = getWorldCatEntity(pid);

            chunkItemWithWorldCatAttributes.addDiscontinuedHoldings(worldCatEntity.getActiveHoldingSymbols());

            final String checksum = Checksum.of(chunkItemWithWorldCatAttributes);
            if (checksum.equals(worldCatEntity.getChecksum())) {
                return ChunkItem.ignoredChunkItem()
                        .withId(chunkItem.getId())
                        .withTrackingId(chunkItem.getTrackingId())
                        .withType(ChunkItem.Type.STRING)
                        .withEncoding(StandardCharsets.UTF_8)
                        .withData("Checksum indicated no change");
            }

            long handleChunkItemStartTime = System.currentTimeMillis();
            WciruServiceBroker.Result brokerResult = null;
            try {
                brokerResult = wciruServiceBroker.push(chunkItemWithWorldCatAttributes, worldCatEntity);
                if (!brokerResult.isFailed()) {
                    if (brokerResult.getLastEvent().getAction() == WciruServiceBroker.Event.Action.DELETE) {
                        LOGGER.info("Deletion of PID '{}' triggered WorldCat entry removal in repository", pid);
                        ocnRepo.getEntityManager().remove(worldCatEntity);
                    } else {
                        worldCatEntity.withOcn(brokerResult.getOcn()).withChecksum(checksum).withActiveHoldingSymbols(chunkItemWithWorldCatAttributes.getActiveHoldingSymbols()).setHasLHR(chunkItemWithWorldCatAttributes.getWorldCatAttributes().hasLhr());
                    }
                }

                return FormattedOutput.of(pid, brokerResult).withId(chunkItem.getId()).withTrackingId(chunkItem.getTrackingId());
            } finally {
                Tag tag = new Tag("status", brokerResult == null ? "timeout" : brokerResult.isFailed() ? "failed" : "success");
                metricsHandler.increment(WorldcatCounterMetrics.WCIRU_UPDATE, tag);
                metricsHandler.update(WorldcatTimerMetrics.WCIRU_SERVICE_REQUESTS, Duration.ofMillis(System.currentTimeMillis() - handleChunkItemStartTime), tag);
            }
        } catch (IllegalArgumentException e) {
            return FormattedOutput.of(e)
                    .withId(chunkItem.getId())
                    .withTrackingId(chunkItem.getTrackingId());
        }
    }

    private WorldCatEntity getWorldCatEntity(Pid pid) {
        final WorldCatEntity worldCatEntity = new WorldCatEntity().withPid(pid.toString());
        final List<WorldCatEntity> worldCatEntities = ocnRepo.lookupWorldCatEntity(worldCatEntity);
        if (worldCatEntities == null || worldCatEntities.isEmpty()) {
            // create new entry in the OCN repository
            worldCatEntity
                    .withAgencyId(pid.getAgencyId())
                    .withBibliographicRecordId(pid.getBibliographicRecordId());
            ocnRepo.getEntityManager().persist(worldCatEntity);
            return worldCatEntity;
        }

        if (worldCatEntities.size() > 1) {
            throw new IllegalStateException("PID '" + pid + "' resolved to more than one WorldCat entity");
        }
        return worldCatEntities.get(0);
    }
}
