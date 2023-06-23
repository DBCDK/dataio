package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.ws.rs.client.ClientBuilder;


public class MessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    FlowStoreServiceConnector flowStoreServiceConnector;
    EntityManager entityManager;


    public MessageConsumer(ServiceHub serviceHub, EntityManager entityManager) {
        super(serviceHub);
        this.entityManager = entityManager;
        ocnRepo = new OcnRepo(entityManager);
        flowStoreServiceConnector = new FlowStoreServiceConnector(ClientBuilder.newClient(), SinkConfig.FLOWSTORE_URL.asString());
        worldCatConfigBean = new WorldCatConfigBean(flowStoreServiceConnector);
    }

    WorldCatConfigBean worldCatConfigBean;

    OcnRepo ocnRepo;

    WorldCatSinkConfig config;
    WciruServiceConnector connector;
    WciruServiceBroker wciruServiceBroker;

    Metric WCIRU_CHUNK_UPDATE = Metric.WCIRU_CHUNK_UPDATE;
    Metric WCIRU_UPDATE = Metric.WCIRU_UPDATE;
    Metric UNHANDLED_EXCEPTIONS = Metric.UNHANDLED_EXCEPTIONS;
    Metric WCIRU_SERVICE_REQUESTS = Metric.WCIRU_SERVICE_REQUESTS;



    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, NullPointerException {
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
                WCIRU_CHUNK_UPDATE.simpleTimer().update(duration);
                LOGGER.info("{} upload to worldcat took {}", chunk, duration);
            } finally {
                DBCTrackedLogContext.remove();
            }
            Instant start = Instant.now();
            sendResultToJobStore(result);
            LOGGER.info("Upload {} to jobstore took {}", result, Duration.between(start, Instant.now()));
        } catch (Exception e) {
            LOGGER.error("Caught unhandled exception while processing jobId: {}, chunkId: {}", JMSHeader.jobId.getHeader(consumedMessage, Integer.class), JMSHeader.chunkId.getHeader(consumedMessage, Long.class), e);
            UNHANDLED_EXCEPTIONS.counter().inc();
            throw new InvalidMessageException(String.format("Uncaught exception: %s", e.getMessage()), e);
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
        EntityTransaction transaction = ocnRepo.getEntityManager().getTransaction();
        try {
            transaction.begin();
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

            Instant handleChunkItemStartTime = Instant.now();
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
                transaction.commit();
                Tag tag = new Tag("status", brokerResult == null ? "timeout" : brokerResult.isFailed() ? "failed" : "success");
                WCIRU_UPDATE.counter(tag).inc();
                WCIRU_SERVICE_REQUESTS.simpleTimer().update(Duration.between(handleChunkItemStartTime, Instant.now()));
            }
        } catch (IllegalArgumentException e) {
            return FormattedOutput.of(e)
                    .withId(chunkItem.getId())
                    .withTrackingId(chunkItem.getTrackingId());
        } finally {
            if (transaction.isActive()) transaction.rollback();
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
