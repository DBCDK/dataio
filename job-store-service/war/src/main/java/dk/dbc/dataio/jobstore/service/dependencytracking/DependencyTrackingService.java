package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.Predicates;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.aggregator.JobCounter;
import dk.dbc.dataio.jobstore.distributed.hz.aggregator.SinkStatusCounter;
import dk.dbc.dataio.jobstore.distributed.hz.aggregator.StatusCounter;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateCounter;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateStatus;
import dk.dbc.dataio.jobstore.distributed.hz.store.DependencyTrackingStore;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants.JOB_ID;
import static dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants.STATUS;

@Singleton
@Startup
@DependsOn("DatabaseMigrator")
public class DependencyTrackingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTrackingService.class);
    private final IMap<TrackingKey, DependencyTracking> dependencyTracker = Hazelcast.Objects.DEPENDENCY_TRACKING.get();
    private final IMap<Integer, Map<ChunkSchedulingStatus, Integer>> countersMap = Hazelcast.Objects.SINK_STATUS.get();
    @Inject
    private MetricRegistry metricRegistry;

    @PostConstruct
    public void config() {
        DependencyTrackingStore.setMetricRegistry(metricRegistry);
        init();
    }

    public DependencyTrackingService init() {
        recountSinkStatus(Set.of());
        return this;
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Commence Hazelcast node shutdown");
        Hazelcast.shutdownNode();
        LOGGER.info("Hazelcast node shutdown completed");
    }


    @Timed
    public TrackingKey add(DependencyTracking entity) {
        int sinkId = entity.getSinkId();
        TrackingKey key = entity.getKey();
        dependencyTracker.set(key, entity);
        countersMap.putIfAbsent(sinkId, new EnumMap<>(ChunkSchedulingStatus.class));
        countersMap.executeOnKey(sinkId, new UpdateCounter(entity.getStatus(), 1));
        return key;
    }

    @Timed
    public int capacity(int sinkId, ChunkSchedulingStatus status) {
        if(status.getMax() == null) throw new IllegalArgumentException("This status does not have a capacity");
        return status.getMax() - getCount(sinkId, status);
    }

    public boolean isEmpty() {
        return dependencyTracker.isEmpty();
    }

    public void modify(TrackingKey key, Consumer<DependencyTracking> consumer) {
        try {
            dependencyTracker.tryLock(key, 2, TimeUnit.MINUTES);
            DependencyTracking entity = dependencyTracker.get(key);
            if(entity == null) {
                LOGGER.info("Unable to modify tracker {} as it has been deleted", key);
                return;
            }
            ChunkSchedulingStatus oldStatus = entity.getStatus();
            consumer.accept(entity);
            ChunkSchedulingStatus status = entity.getStatus();
            if(oldStatus != status) {
                countersMap.executeOnKey(entity.getSinkId(), new UpdateCounter(Map.of(oldStatus, -1, status, 1)));
            }
            entity.updateLastModified();
            dependencyTracker.set(key, entity);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                dependencyTracker.unlock(key);
            } catch (Exception ignored) {}
        }
    }

    public List<DependencyTracking> getSnapshot(int jobId) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        Predicate<TrackingKey, DependencyTracking> p = e.key().get(JOB_ID).equal(jobId);
        Collection<DependencyTracking> values = dependencyTracker.values(p);
        return values.stream().sorted(Comparator.comparing(k -> k.getKey().getChunkId())).collect(Collectors.toList());
    }

    public Stream<DependencyTrackingRO> getStaleDependencies(ChunkSchedulingStatus status, Duration timeout) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        @SuppressWarnings("unchecked")
        Predicate<TrackingKey, DependencyTracking> p = e.get(STATUS).equal(status).and(e.get("lastModified").lessThan(Instant.now().minus(timeout)));
        return dependencyTracker.values(p).stream().map(DependencyTrackingRO.class::cast);
    }

    @Timed
    public DependencyTrackingRO get(TrackingKey key) {
        return dependencyTracker.get(key);
    }

    public boolean contains(TrackingKey key) {
        return dependencyTracker.containsKey(key);
    }

    public int resetStatus(ChunkSchedulingStatus from, ChunkSchedulingStatus to, Integer... jobIds) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        @SuppressWarnings("unchecked")
        Predicate<TrackingKey, DependencyTracking> p = e.get(STATUS).equal(from).and(e.key().get(JOB_ID).in(jobIds));
        Set<TrackingKey> entries = dependencyTracker.keySet(p);
        entries.forEach(key -> setStatus(key, to));
        return entries.size();
    }

    @Timed
    @SuppressWarnings("unchecked")
    public void removeJobId(int jobId) {
        remove(Predicates.newPredicateBuilder().getEntryObject().key().get(JOB_ID).equal(jobId));
    }

    @Timed
    public StatusChangeEvent setStatus(TrackingKey key, ChunkSchedulingStatus status) {
        return setStatus(key, status, false);
    }

    @Timed
    public StatusChangeEvent setValidatedStatus(TrackingKey key, ChunkSchedulingStatus status) {
        return setStatus(key, status, true);
    }

    private StatusChangeEvent setStatus(TrackingKey key, ChunkSchedulingStatus newStatus, boolean validate) {
        StatusChangeEvent statusChangeEvent = dependencyTracker.executeOnKey(key, new UpdateStatus(newStatus, validate));
        updateCounters(Stream.of(statusChangeEvent));
        return statusChangeEvent;
    }

    /**
     * Removes a chunk's entry and hands the removed entry to the caller that removed it.
     * <p>
     * The return value is the once-only token for this removal. {@code IMap.remove} is atomic per
     * key across the cluster, so however many callers ask to remove one chunk, exactly one is given
     * the entry and every other is given null. A caller that has to act once per chunk asks that
     * question here rather than by reading the entry first, which two callers can both pass.
     * {@code JobSchedulerBean.chunkDeliveringDone} counts a delivered data chunk on that basis.
     *
     * @param key chunk to remove
     * @return the removed entry, or null if this call did not remove it
     */
    public DependencyTracking remove(TrackingKey key) {
        DependencyTracking removed = dependencyTracker.remove(key);
        if(removed == null) return null;
        countersMap.executeOnKey(removed.getSinkId(), new UpdateCounter(removed.getStatus(), -1));
        LOGGER.info("Removed tracking key {} from dependency tracker", key.toChunkIdentifier());
        return removed;
    }

    public void remove(Predicate<TrackingKey, DependencyTracking> predicate) {
        dependencyTracker.removeAll(predicate);
        recountSinkStatus(Set.of());
        LOGGER.info("Removed tracking keys matched by predicate: {}", predicate);
    }

    public void reload() {
        dependencyTracker.loadAll(true);
        recountSinkStatus(Set.of());
    }

    public void recountSinkStatus(Set<Integer> sinkIds) {
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> map = statusCount(sinkIds);
        if(sinkIds.isEmpty()) countersMap.clear();
        else sinkIds.forEach(countersMap::remove);
        countersMap.putAll(map);
        LOGGER.info("Completed status map recount for {}", sinkIds);
    }

    public Set<Integer> getActiveSinks(ChunkSchedulingStatus status) {
        return countersMap.entrySet().stream()
                .filter(e -> e.getValue().get(status) != null && e.getValue().get(status) > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Map<Integer, Map<ChunkSchedulingStatus, Integer>> getCountersForSinks() {
        return countersMap.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Collections.unmodifiableMap(e.getValue())));
    }

    public Integer getCount(int sinkId, ChunkSchedulingStatus cs) {
        return Optional.ofNullable(countersMap.get(sinkId)).map(m -> m.get(cs)).orElse(0);
    }

    public int statusCount(int sinkId, ChunkSchedulingStatus status) {
        return dependencyTracker.aggregate(new SinkStatusCounter(sinkId, status));
    }

    public Map<Integer, Map<ChunkSchedulingStatus, Integer>> statusCount(Set<Integer> sinkIds) {
        StatusCounter statusCounter = new StatusCounter(sinkIds);
        return dependencyTracker.aggregate(statusCounter);
    }

    @Timed
    public Collection<DependencyTracking> findDependencies(ChunkSchedulingStatus status, Integer sinkId, Integer limit) {
        return dependencyTracker.values(makeDependencyPredicate(status, sinkId, limit));
    }

    @SuppressWarnings("unchecked")
    private Predicate<TrackingKey, DependencyTracking> makeDependencyPredicate(ChunkSchedulingStatus status, Integer sinkId, Integer limit) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        Predicate<TrackingKey, DependencyTracking> p;
        if(sinkId != null) p = e.get("sinkId").equal(sinkId).and(e.get(STATUS).equal(status));
        else p = e.get(STATUS).equal(status);
        return Predicates.pagingPredicate(p, limit == null ? Integer.MAX_VALUE : limit);
    }

    /**
     * Ascertains if a chunk is currently scheduled
     *
     * @param chunkEntity chunk entity representing the chunk
     * @return true if scheduled, false if not
     */
    public boolean isScheduled(ChunkEntity chunkEntity) {
        return dependencyTracker.containsKey(new TrackingKey(chunkEntity.getKey().getJobId(), chunkEntity.getKey().getId()));
    }

    public Set<Integer> getAllJobIs() {
        return dependencyTracker.keySet().stream().map(TrackingKey::getJobId).collect(Collectors.toSet());
    }

    public Integer[] jobCount(int sinkId) {
        return dependencyTracker.aggregate(new JobCounter(sinkId));
    }

    @Readiness
    public HealthCheck readyCheck() {
        return () -> HealthCheckResponse.named("hazelcast-ready").status(Hazelcast.isReady()).build();
    }

    private void updateCounters(Stream<StatusChangeEvent> changes) {
        Map<Integer, List<StatusChangeEvent>> bySink = changes.filter(Objects::nonNull).collect(Collectors.groupingBy(StatusChangeEvent::getSinkId));
        bySink.forEach(this::updateCounters);
    }

    private void updateCounters(Integer sinkId, List<StatusChangeEvent> statusChangeEvents) {
        EnumMap<ChunkSchedulingStatus, Integer> deltas = new EnumMap<>(ChunkSchedulingStatus.class);
        statusChangeEvents.forEach(e -> e.apply(deltas));
        countersMap.executeOnKey(sinkId, new UpdateCounter(deltas));
    }
}
