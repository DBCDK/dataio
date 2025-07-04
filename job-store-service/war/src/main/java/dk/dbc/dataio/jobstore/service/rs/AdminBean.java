package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;

@Stateless
@Path("/")
public class AdminBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBean.class);
    @EJB
    JobSchedulerBean jobSchedulerBean;
    @EJB
    PgJobStoreRepository jobStoreRepository;
    private Instant nextJobCheckFrom = null;

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
        if(Hazelcast.isSlave()) return;
        try {
            Stream<DependencyTrackingRO> readyStream = dependencyTrackingService.getStaleDependencies(READY_FOR_DELIVERY, Duration.ofMinutes(5));
            readyStream.forEach(dt -> dependencyTrackingService.setStatus(dt.getKey(), SCHEDULED_FOR_DELIVERY));
            Stream<DependencyTrackingRO> delStream = dependencyTrackingService.getStaleDependencies(QUEUED_FOR_DELIVERY, Duration.ofHours(1)).filter(this::isTimeout);
            Stream<DependencyTrackingRO> procStream = dependencyTrackingService.getStaleDependencies(QUEUED_FOR_PROCESSING, processorTimeout);
            List<DependencyTrackingRO> list = Stream.concat(delStream, procStream).collect(Collectors.toList());
            resendIfNeeded(list);
            list.stream().map(s -> getSinkName(s.getSinkId())).distinct().filter(s -> staleChunks.putIfAbsent(s, new AtomicInteger(0)) == null).forEach(this::registerChunkMetric);
            Map<Integer, List<DependencyTrackingRO>> map = list.stream().collect(Collectors.groupingBy(DependencyTrackingRO::getSinkId));
            Map<String, Integer> counters = map.entrySet().stream().collect(Collectors.toMap(e -> getSinkName(e.getKey()), e -> e.getValue().size()));
            staleChunks.forEach((k, v) -> v.set(counters.getOrDefault(k, 0)));
            if(!list.isEmpty()) LOGGER.info("Stale chunks alert set for jobs: " + list.stream().map(e -> e.getKey().getJobId()).distinct().map(i -> Integer.toString(i)).collect(Collectors.joining(", ")));
        } catch (RuntimeException e) {
            LOGGER.error("Caught runtime exception un update stale chunks", e);
            throw e;
        }
    }

    @Schedule(minute = "10", hour = "*", persistent = false)
    public void recheckBlocks() {
        if(Hazelcast.isSlave()) return;
        Set<Integer> trackedJobIds = dependencyTrackingService.getAllJobIs();
        for (Integer jobId : trackedJobIds) {
            JobEntity entity = jobStoreRepository.getJobEntityById(jobId);
            if(entity == null || entity.getTimeOfCompletion() != null) {
                dependencyTrackingService.removeJobId(jobId);
                LOGGER.info("Trackers for finished Job id: {} was removed", jobId);
            }
        }
        Set<TrackingKey> keys = dependencyTrackingService.recheckBlocks();
        if(!keys.isEmpty()) LOGGER.info("Hourly blocked check has released {}", keys);
    }

    @Schedule(minute = "15", hour = "*", persistent = false)
    public void completeFinishedJobs() {
        if(Hazelcast.isSlave()) return;
        Instant from = nextJobCheckFrom == null ? Instant.now().minus(Duration.ofHours(2)) : nextJobCheckFrom;
        Instant to = Instant.now().minus(Duration.ofMinutes(1));
        nextJobCheckFrom = to;
        completeFinishedJobs(from, to);
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

        retries.forEach(dt -> dependencyTrackingService.modify(dt.getKey(), DependencyTracking::resend));
        Set<Integer> sinks = list.stream().map(DependencyTrackingRO::getSinkId).collect(Collectors.toSet());
        jobSchedulerBean.loadSinkStatusOnBootstrap(sinks);
    }

    @SuppressWarnings("unused")
    @Schedule(minute = "5", hour = "*", persistent = false)
    public void cleanStaleJMSConnections() {
        if(Hazelcast.isSlave()) return;
        LOGGER.info("Cleaning stale artemis connections");
        Instant i = Instant.now().minus(Duration.ofMinutes(15));
        adminClient.closeConsumerConnections(c -> i.isAfter(c.getLastAcknowledgedTime()) && c.getDeliveringCount() > 0);
    }

    @GET
    @Path(JobStoreServiceConstants.SINKS_STATUS_RECOUNT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response recountSinkStatus() throws JSONBException {
        dependencyTrackingService.recountSinkStatus(Set.of());
        return Response.ok(jsonbContext.marshall(dependencyTrackingService.getCountersForSinks())).build();
    }

    @GET
    @Path(JobStoreServiceConstants.DEPENDENCY_CHECK_BLOCKED)
    public Response checkBlocked() throws JSONBException {
        return Response.ok(jsonbContext.marshall(dependencyTrackingService.recheckBlocks())).build();
    }

    @GET
    @Path(JobStoreServiceConstants.DEPENDENCY_RELOAD)
    public Response reload() {
        dependencyTrackingService.reload();
        return Response.ok().build();
    }

    @GET
    @Path(JobStoreServiceConstants.DEPENDENCIES)
    public Response dependencies(@PathParam("jobId") int jobId) throws JSONBException {
        List<DependencyTracking> snapshot = dependencyTrackingService.getSnapshot(jobId);
        return Response.ok(jsonbContext.marshall(snapshot)).build();
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

    @SuppressWarnings("UnresolvedRestParam")
    @POST
    @Path(JobStoreServiceConstants.FORCE_DEPENDENCY_TRACKING_RETRANSMIT_ID)
    public Response retransmit(@PathParam("jobIds") String jobIds) {
        Set<Integer> ids = Arrays.stream(jobIds.split(" *, *")).map(Integer::valueOf).collect(Collectors.toSet());
        return retransmitJobs(ids);
    }

    @GET
    @Path(JobStoreServiceConstants.CHECK_INCOMPLETE)
    public Response completeFinishedJobs(@PathParam("days") int days) {
        Instant from = LocalDate.now().minusDays(days).atStartOfDay(Constants.ZONE_CPH).toInstant();
        Instant to = Instant.now().minus(Duration.ofMinutes(1)) ;
        completeFinishedJobs(from, to);
        return Response.ok().build();
    }

    private void completeFinishedJobs(Instant from, Instant to) {
        List<JobInfoSnapshot> jobs = jobStoreRepository.listJobs(new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.GREATER_THAN_OR_EQUAL_TO, new Timestamp(from.toEpochMilli())))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.LESS_THAN, new Timestamp(to.toEpochMilli())))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL)));
        for (JobInfoSnapshot job : jobs) {
            JobEntity entity = jobStoreRepository.getJobEntityById(job.getJobId());
            if(entity.getState().phaseIsDone(State.Phase.PARTITIONING)) {
                List<Timestamp> chunks = jobStoreRepository.listTimeOfChunkCompletion(job.getJobId());
                if(job.getNumberOfChunks() <= chunks.size() && chunks.stream().noneMatch(Objects::isNull)) {
                    Arrays.stream(State.Phase.values())
                            .filter(p -> entity.getState().getPhase(p).getEndDate() == null)
                            .forEach(p -> entity.getState().getPhase(p).withEndDate(new Date()));
                    entity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                    LOGGER.info("completeFinishedJobs marked {} as completed, all chunks are accounted for", job.getJobId());
                }
            }
        }
    }

    private Response retransmitJobs(Set<Integer> jobIds) {
        Set<Integer> sinkId = jobIds.stream().map(id -> entityManager.find(JobEntity.class, id))
                .map(JobEntity::getCachedSink)
                .map(SinkCacheEntity::getSink)
                .map(Sink::getId)
                .collect(Collectors.toSet());
        int rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_PROCESSING, SCHEDULED_FOR_PROCESSING);
        LOGGER.info("Reset dependency tracking states. Sets status QUEUED_FOR_PROCESSING -> SCHEDULED_FOR_PROCESSING for {} entities", rowsUpdated);
        rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_DELIVERY, SCHEDULED_FOR_DELIVERY);
        LOGGER.info("Reset dependency tracking states. Sets status QUEUED_FOR_DELIVERY -> SCHEDULED_FOR_DELIVERY for {} entities", rowsUpdated);
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
