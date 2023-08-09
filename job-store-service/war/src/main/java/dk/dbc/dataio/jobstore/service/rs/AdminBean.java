package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.jms.artemis.AdminClient;
import dk.dbc.jms.artemis.AdminClientFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_PROCESSING;

@Stateless
@Path("/")
public class AdminBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBean.class);
    @EJB
    JobSchedulerBean jobSchedulerBean;
    @EJB
    PgJobStoreRepository jobStoreRepository;

    @EJB
    FlowStoreServiceConnectorBean flowstore;

    @Inject
    @ConfigProperty(name = "PROCESSOR_TIMEOUT", defaultValue = "PT1H")
    private Duration processorTimeout;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Inject
    private MetricRegistry metricRegistry;;

    AdminClient adminClient = AdminClientFactory.getAdminClient();
    private final Map<String, Counter> staleChunks = new HashMap<>();

    @SuppressWarnings("unused")
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void updateStaleChunks() throws FlowStoreServiceConnectorException {
        Map<Integer, Sink> allSinks = flowstore.getConnector().findAllSinks().stream().collect(Collectors.toMap(s -> (int)s.getId(), s -> s));
        Duration minDuration = findMinTimeout(allSinks.values());
        Stream<DependencyTrackingEntity> delStream = jobStoreRepository.getStaleDependencies(QUEUED_FOR_DELIVERY, minDuration).stream().filter(d -> isTimeout(d, allSinks));
        Stream<DependencyTrackingEntity> procStream = jobStoreRepository.getStaleDependencies(QUEUED_FOR_PROCESSING, processorTimeout).stream();
        Stream.concat(delStream, procStream).forEach(d -> staleChunks.computeIfAbsent(getSinkName(d, allSinks), this::registerChunkMetric).inc());
    }

    @SuppressWarnings("unused")
    @Schedule(minute = "5", hour = "*", persistent = false)
    public void cleanStaleJMSConnections() {
        LOGGER.info("Cleaning stale artemis connections");
        Instant i = Instant.now().minus(Duration.ofMinutes(15));
        adminClient.closeConsumerConnections(c -> i.isAfter(c.getLastAcknowledgedTime()) && c.getDeliveringCount() > 0);
    }

    @GET
    @Path(JobStoreServiceConstants.CLEAR_CACHE)
    @Produces({MediaType.TEXT_PLAIN})
    public Response clearJobstoreCache() {
        Cache cache = entityManager.getEntityManagerFactory().getCache();
        cache.evictAll();
        LOGGER.info("Evicted jpa cache");
        return Response.ok("ok").build();
    }

    @POST
    @Path(JobStoreServiceConstants.FORCE_DEPENDENCY_TRACKING_RETRANSMIT)
    public Response retransmit() {
        return retransmitJobs(null);
    }

    @SuppressWarnings("UnresolvedRestParam")
    @POST
    @Path(JobStoreServiceConstants.FORCE_DEPENDENCY_TRACKING_RETRANSMIT_ID)
    public Response retransmit(@PathParam("jobId") Integer jobId) {
        return retransmitJobs(jobId);
    }

    private Response retransmitJobs(Integer jobId) {
        Integer sinkId = Optional.ofNullable(jobId).map(id -> entityManager.find(JobEntity.class, id))
                .map(JobEntity::getCachedSink)
                .map(SinkCacheEntity::getSink)
                .map(Sink::getId)
                .map(Long::intValue)
                .orElse(null);
        int rowsUpdated = jobStoreRepository.resetStatus(jobId, QUEUED_FOR_PROCESSING, READY_FOR_PROCESSING);
        LOGGER.info("Reset dependency tracking states. Sets status = 1 for status = 2 for {} entities", rowsUpdated);
        rowsUpdated = jobStoreRepository.resetStatus(jobId, QUEUED_FOR_DELIVERY, READY_FOR_DELIVERY);
        LOGGER.info("Reset dependency tracking states. Sets status = 4 for status = 5 for {} entities", rowsUpdated);
        jobSchedulerBean.loadSinkStatusOnBootstrap(sinkId);
        return Response.ok().build();
    }

    static Duration findMinTimeout(Collection<Sink> allSinks) {
        int h = allSinks.stream()
                .map(Sink::getContent)
                .mapToInt(SinkContent::getTimeout)
                .min()
                .orElse(1);
        return Duration.ofHours(h);
    }

    private Counter registerChunkMetric(String sinkName) {
        MetricID metricID = new MetricID("dataio_stale_chunks", new Tag("sink", sinkName));
        LOGGER.info("Registering metric: {}", metricID);
        return metricRegistry.counter(metricID);
    }

    static String getSinkName(DependencyTrackingEntity d, Map<Integer, Sink> allSinks) {
        return Optional.ofNullable(allSinks.get(d.getSinkid()))
                .map(Sink::getContent)
                .map(SinkContent::getName)
                .orElse(Integer.toString(d.getSinkid()));
    }

    static boolean isTimeout(DependencyTrackingEntity de, Map<Integer, Sink> allSinks) {
        if(de.getLastModified() == null) return false;
        Optional<Sink> sink = Optional.ofNullable(allSinks.get(de.getSinkid()));
        Instant lm = Instant.ofEpochMilli(de.getLastModified().getTime());
        Instant now = Instant.now();
        return sink.map(Sink::getContent)
                .map(SinkContent::getTimeout)
                .map(Duration::ofHours)
                .map(now::minus)
                .map(lm::isBefore)
                .orElse(false);
    }
}
