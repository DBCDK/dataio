package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.client.ClientBuilder;
import org.eclipse.microprofile.metrics.Tag;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;


public class WorldcatMessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldcatMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    FlowStoreServiceConnector flowStoreServiceConnector;
    EntityManagerFactory entityManagerFactory;
    WorldCatConfigBean worldCatConfigBean;
    WorldCatSinkConfig config;
    WciruServiceConnector connector;
    WciruServiceBroker wciruServiceBroker;

    @SuppressWarnings("java:S2095")
    public WorldcatMessageConsumer(ServiceHub serviceHub, EntityManagerFactory entityManagerFactory) {
        super(serviceHub);
        this.entityManagerFactory = entityManagerFactory;
        flowStoreServiceConnector = new FlowStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()),
                UserAgent.forInternalRequests(), SinkConfig.FLOWSTORE_URL.asString());
        worldCatConfigBean = new WorldCatConfigBean(flowStoreServiceConnector);
    }

    /**
     * Pushes the record of a successfully processed item to WorldCat through the WCIRU service
     * <p>
     * The entity manager is scoped to the item and closed on the way out, since the ocn-repo
     * transaction the push runs in is committed before this method returns and nothing reads
     * the persistence context afterwards.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            return deliverItem(message, item, new OcnRepo(entityManager));
        }
    }

    /**
     * Delivers one item against the given ocn-repo, so that a test can supply a repository of
     * its own and read back what the delivery wrote
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered,
     * so that it counts towards the job's ignored items and advances no delivery watermark for
     * a record nothing was sent for.
     */
    ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item, OcnRepo ocnRepo) {
        switch (item.getStatus()) {
            case FAILURE:
                return ignored(item, "Failed by job-processor");
            case IGNORE:
                return ignored(item, "Ignored by job-processor");
            default:
                return push(refreshState(worldCatConfigBean.getConfig(message)), item, ocnRepo);
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

    /**
     * Rebuilds the WCIRU connector and broker when the sink config has changed, and hands back
     * the broker to deliver with
     * <p>
     * Synchronized, and returning the broker rather than leaving the caller to read the field,
     * so that a delivery uses one broker for the whole item. Several consumer threads call this
     * concurrently, and reading the field would otherwise expose a broker built from one config
     * next to a config field already holding the next.
     */
    private synchronized WciruServiceBroker refreshState(WorldCatSinkConfig latestConfig) {
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating WCIRU connector");
            connector = getWciruServiceConnector(latestConfig);
            wciruServiceBroker = new WciruServiceBroker(connector);
            config = latestConfig;
        }
        return wciruServiceBroker;
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

    /**
     * Pushes one record to WorldCat and updates the ocn-repo entry for it
     * <p>
     * A record whose checksum matches the one held for it is reported as ignored, since nothing
     * is sent for it. Reporting it delivered would count it among the job's succeeded items and
     * advance the record's delivery watermark for a push that never happened.
     * <p>
     * A WCIRU rejection arrives as a failed broker result rather than as an exception, and fails
     * the item. The ocn-repo entry is then left as it was, so the next version of the record
     * pushes cleanly.
     */
    ItemDeliveryResult push(WciruServiceBroker broker, ChunkItem item, OcnRepo ocnRepo) {
        EntityTransaction transaction = ocnRepo.getEntityManager().getTransaction();
        try {
            transaction.begin();
            final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                    ChunkItemWithWorldCatAttributes.of(item);
            final Pid pid = Pid.of(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getPid());
            final WorldCatEntity worldCatEntity = getWorldCatEntity(pid, ocnRepo);

            chunkItemWithWorldCatAttributes.addDiscontinuedHoldings(worldCatEntity.getActiveHoldingSymbols());

            final String checksum = Checksum.of(chunkItemWithWorldCatAttributes);
            if (checksum.equals(worldCatEntity.getChecksum())) {
                return ignored(item, "Checksum indicated no change");
            }

            Instant pushStartTime = Instant.now();
            WciruServiceBroker.Result brokerResult = null;
            try {
                brokerResult = broker.push(chunkItemWithWorldCatAttributes, worldCatEntity);
                if (!brokerResult.isFailed()) {
                    if (brokerResult.getLastEvent().getAction() == WciruServiceBroker.Event.Action.DELETE) {
                        LOGGER.info("Deletion of PID '{}' triggered WorldCat entry removal in repository", pid);
                        ocnRepo.getEntityManager().remove(worldCatEntity);
                    } else {
                        worldCatEntity.withOcn(brokerResult.getOcn()).withChecksum(checksum).withActiveHoldingSymbols(chunkItemWithWorldCatAttributes.getActiveHoldingSymbols()).setHasLHR(chunkItemWithWorldCatAttributes.getWorldCatAttributes().hasLhr());
                    }
                }
                return ItemDeliveryResult.of(
                        brokerResult.isFailed()
                                ? ItemDeliveryResult.Status.FAILED
                                : ItemDeliveryResult.Status.DELIVERED,
                        FormattedOutput.of(pid, brokerResult)
                                .withId(item.getId())
                                .withTrackingId(item.getTrackingId()));
            } finally {
                transaction.commit();
                Tag tag = new Tag("status", brokerResult == null ? "timeout" : brokerResult.isFailed() ? "failed" : "success");
                Metric.WCIRU_UPDATE.counter(tag).inc();
                Metric.WCIRU_SERVICE_REQUESTS.timer().update(Duration.between(pushStartTime, Instant.now()));
            }
        } catch (IllegalArgumentException e) {
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                    FormattedOutput.of(e)
                            .withId(item.getId())
                            .withTrackingId(item.getTrackingId()));
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    private WorldCatEntity getWorldCatEntity(Pid pid, OcnRepo ocnRepo) {
        final WorldCatEntity worldCatEntity = new WorldCatEntity().withPid(pid.toString());
        final List<WorldCatEntity> worldCatEntities = ocnRepo.lookupWorldCatEntity(worldCatEntity);
        if (worldCatEntities == null || worldCatEntities.isEmpty()) {
            // create new entry in the OCN repository
            worldCatEntity
                    .withAgencyId(pid.getAgencyId())
                    .withBibliographicRecordId(pid.getBibliographicRecordId());
            return ocnRepo.getEntityManager().merge(worldCatEntity);
        }

        if (worldCatEntities.size() > 1) {
            throw new IllegalStateException("PID '" + pid + "' resolved to more than one WorldCat entity");
        }
        return worldCatEntities.get(0);
    }

    private ItemDeliveryResult ignored(ChunkItem item, String reason) {
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                ChunkItem.ignoredChunkItem()
                        .withId(item.getId())
                        .withTrackingId(item.getTrackingId())
                        .withType(ChunkItem.Type.STRING)
                        .withEncoding(StandardCharsets.UTF_8)
                        .withData(reason));
    }
}
