package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.jms.artemis.AdminClient;
import dk.dbc.jms.artemis.AdminClientFactory;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.glassfish.jersey.internal.guava.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;

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
    DependencyTrackingService dependencyTrackingService;

    @Inject
    @ConfigProperty(name = "PROCESSOR_TIMEOUT", defaultValue = "PT1H")
    private Duration processorTimeout;

    JSONBContext jsonbContext = new JSONBContext();

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Inject
    private MetricRegistry metricRegistry;

    AdminClient adminClient = AdminClientFactory.getAdminClient();
    private static final Map<String, AtomicInteger> staleChunks = new ConcurrentHashMap<>();
    private final org.glassfish.jersey.internal.guava.Cache<Integer, Sink> sinkMap = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

    @SuppressWarnings("unused")
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void updateStaleChunks() {
        try {
            Stream<DependencyTrackingRO> delStream = dependencyTrackingService.getStaleDependencies(QUEUED_FOR_DELIVERY, Duration.ofHours(1)).filter(this::isTimeout);
            Stream<DependencyTrackingRO> procStream = dependencyTrackingService.getStaleDependencies(QUEUED_FOR_PROCESSING, processorTimeout);
            List<DependencyTrackingRO> list = Stream.concat(delStream, procStream).collect(Collectors.toList());
            resendIfNeeded(list);
            list.stream().map(s -> getSinkName(s.getSinkId())).distinct().filter(s -> staleChunks.putIfAbsent(s, new AtomicInteger(0)) == null).forEach(this::registerChunkMetric);
            Map<Integer, List<DependencyTrackingRO>> map = list.stream().collect(Collectors.groupingBy(DependencyTrackingRO::getSinkId));
            Map<String, Integer> counters = map.entrySet().stream().collect(Collectors.toMap(e -> getSinkName(e.getKey()), e -> e.getValue().size()));
            staleChunks.forEach((k, v) -> v.set(counters.getOrDefault(k, 0)));
            LOGGER.info("Stale chunks alert set for jobs: " + list.stream().map(e -> e.getKey().getJobId()).distinct().map(i -> Integer.toString(i)).collect(Collectors.joining(", ")));
        } catch (RuntimeException e) {
            LOGGER.error("Caught runtime exception un update stale chunks", e);
            throw e;
        }
    }

    public void resendIfNeeded(List<DependencyTrackingRO> list) {
        Set<DependencyTrackingRO> retries = list.stream()
                .filter(de -> de.getRetries() < 1)
                .filter(de -> de.getWaitingOn().isEmpty())
                .collect(Collectors.toSet());
        if(retries.isEmpty()) return;
        LOGGER.warn("Retrying stale trackers: {}", retries.stream()
                .map(e -> e.getKey().toChunkIdentifier())
                .collect(Collectors.joining(", ")));
        list.forEach(dt -> dependencyTrackingService.modify(dt.getKey(), DependencyTracking::resend));
        Set<Integer> sinks = list.stream().map(DependencyTrackingRO::getSinkId).collect(Collectors.toSet());
        jobSchedulerBean.loadSinkStatusOnBootstrap(sinks);
    }

    @SuppressWarnings("unused")
    @Schedule(minute = "5", hour = "*", persistent = false)
    public void cleanStaleJMSConnections() {
        LOGGER.info("Cleaning stale artemis connections");
        Instant i = Instant.now().minus(Duration.ofMinutes(15));
        adminClient.closeConsumerConnections(c -> i.isAfter(c.getLastAcknowledgedTime()) && c.getDeliveringCount() > 0);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response recountSinkStatus() throws JSONBException {
        dependencyTrackingService.recountSinkStatus(Set.of());
        return Response.ok(jsonbContext.marshall(dependencyTrackingService.getSinkStatusMap())).build();
    }

    @GET
    @Path(JobStoreServiceConstants.CLEAR_HZ)
    @Produces({MediaType.TEXT_PLAIN})
    public Response clearHazelcastCache(@PathParam("name") String cacheName) {
        Map<?, ?> map = Hazelcast.Objects.valueOf(cacheName.toUpperCase()).get();
        map.clear();
        return Response.ok().build();
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
    public Response retransmit(@PathParam("jobIds") String jobIds) {
        Set<Integer> ids = Arrays.stream(jobIds.split(" *, *")).map(Integer::valueOf).collect(Collectors.toSet());
        return retransmitJobs(ids);
    }

    private Response retransmitJobs(Set<Integer> jobIds) {
        Set<Integer> sinkId = jobIds.stream().map(id -> entityManager.find(JobEntity.class, id))
                .map(JobEntity::getCachedSink)
                .map(SinkCacheEntity::getSink)
                .map(Sink::getId)
                .collect(Collectors.toSet());
        int rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_PROCESSING, READY_FOR_PROCESSING);
        LOGGER.info("Reset dependency tracking states. Sets status = 1 for status = 2 for {} entities", rowsUpdated);
        rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_DELIVERY, READY_FOR_DELIVERY);
        LOGGER.info("Reset dependency tracking states. Sets status = 4 for status = 5 for {} entities", rowsUpdated);
        jobSchedulerBean.loadSinkStatusOnBootstrap(sinkId);
        return Response.ok().build();
    }

    private void registerChunkMetric(String sinkName) {
        MetricID metricID = new MetricID("dataio_stale_chunks", new Tag("sink", sinkName));
        LOGGER.info("Registering metric: {}", metricID);
        metricRegistry.gauge(metricID, () -> staleChunks.get(sinkName));
    }

    private String getSinkName(int id) {
        return getSink(id).getContent().getName();
    }

    Sink getSink(int id) {
        if(id == 1) return Sink.DIFF;
        Sink sink = sinkMap.getIfPresent(id);
        if(sink == null) {
            sink = getFromFlowstore(id);
            sinkMap.put(id, sink);
        }
        return sink;
    }

    private Sink getFromFlowstore(int id) {
        try {
            return flowstore.getConnector().getSink(id);
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException("Found no sink with id " + id);
        }
    }

    boolean isTimeout(DependencyTrackingRO de) {
        if(de.getLastModified() == null) return false;
        Optional<Sink> sink = Optional.ofNullable(getSink(de.getSinkId()));
        Instant lm = de.getLastModified();
        Instant now = Instant.now();
        return sink.map(Sink::getContent)
                .map(SinkContent::getTimeout)
                .map(Duration::ofHours)
                .map(now::minus)
                .map(lm::isBefore)
                .orElse(false);
    }
}
