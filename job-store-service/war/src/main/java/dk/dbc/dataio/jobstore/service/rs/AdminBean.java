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
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.glassfish.jersey.internal.guava.CacheBuilder;
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
    private MetricRegistry metricRegistry;

    AdminClient adminClient = AdminClientFactory.getAdminClient();
    private static final Map<String, AtomicInteger> staleChunks = new ConcurrentHashMap<>();
    private final org.glassfish.jersey.internal.guava.Cache<Integer, Sink> sinkMap = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

    @SuppressWarnings("unused")
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void updateStaleChunks() {
        try {
            Stream<DependencyTrackingEntity> delStream = jobStoreRepository.getStaleDependencies(QUEUED_FOR_DELIVERY, Duration.ofHours(1)).stream().filter(this::isTimeout);
            Stream<DependencyTrackingEntity> procStream = jobStoreRepository.getStaleDependencies(QUEUED_FOR_PROCESSING, processorTimeout).stream();
            List<DependencyTrackingEntity> list = Stream.concat(delStream, procStream).collect(Collectors.toList());
            retryIfNeeded(list);
            list.stream().map(s -> getSinkName(s.getSinkid())).distinct().filter(s -> staleChunks.putIfAbsent(s, new AtomicInteger(0)) == null).forEach(this::registerChunkMetric);
            Map<Integer, List<DependencyTrackingEntity>> map = list.stream().collect(Collectors.groupingBy(DependencyTrackingEntity::getSinkid));
            Map<String, Integer> counters = map.entrySet().stream().collect(Collectors.toMap(e -> getSinkName(e.getKey()), e -> e.getValue().size()));
            staleChunks.forEach((k, v) -> v.set(counters.getOrDefault(k, 0)));
            LOGGER.info("Stale chunks alert set for jobs: " + list.stream().map(e -> e.getKey().getJobId()).distinct().map(i -> Integer.toString(i)).collect(Collectors.joining(", ")));
        } catch (RuntimeException e) {
            LOGGER.error("Caught runtime exception un update stale chunks", e);
            throw e;
        }
    }

    public void retryIfNeeded(List<DependencyTrackingEntity> list) {
        Set<Integer> retries = list.stream()
                .filter(de -> de.getRetries() < 1)
                .filter(de -> de.getWaitingOn().isEmpty())
                .map(de -> de.getKey().getJobId())
                .collect(Collectors.toSet());
        if(retries.isEmpty()) return;
        LOGGER.warn("Retrying stale jobs: {}", retries.stream().map(Object::toString).collect(Collectors.joining(", ")));
        retransmitJobs(retries);
        list.forEach(DependencyTrackingEntity::incRetries);
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
    public Response retransmit(@PathParam("jobIds") String jobIds) {
        Set<Integer> ids = Arrays.stream(jobIds.split(" *, *")).map(Integer::valueOf).collect(Collectors.toSet());
        return retransmitJobs(ids);
    }

    private Response retransmitJobs(Set<Integer> jobIds) {
        Set<Integer> sinkId = jobIds.stream().map(id -> entityManager.find(JobEntity.class, id))
                .map(JobEntity::getCachedSink)
                .map(SinkCacheEntity::getSink)
                .map(Sink::getId)
                .map(Long::intValue)
                .collect(Collectors.toSet());
        int rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_PROCESSING, READY_FOR_PROCESSING);
        LOGGER.info("Reset dependency tracking states. Sets status = 1 for status = 2 for {} entities", rowsUpdated);
        rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_DELIVERY, READY_FOR_DELIVERY);
        LOGGER.info("Reset dependency tracking states. Sets status = 4 for status = 5 for {} entities", rowsUpdated);
        sinkId.forEach(jobSchedulerBean::loadSinkStatusOnBootstrap);
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

    boolean isTimeout(DependencyTrackingEntity de) {
        if(de.getLastModified() == null) return false;
        Optional<Sink> sink = Optional.ofNullable(getSink(de.getSinkid()));
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
