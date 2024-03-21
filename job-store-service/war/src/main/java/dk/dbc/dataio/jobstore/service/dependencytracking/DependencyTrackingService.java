package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.Predicates;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.aggregator.BlockedCounter;
import dk.dbc.dataio.jobstore.distributed.hz.aggregator.JobCounter;
import dk.dbc.dataio.jobstore.distributed.hz.aggregator.SinkStatusCounter;
import dk.dbc.dataio.jobstore.distributed.hz.aggregator.StatusCounter;
import dk.dbc.dataio.jobstore.distributed.hz.processor.AddTerminationWaitingOn;
import dk.dbc.dataio.jobstore.distributed.hz.processor.RemoveWaitingOn;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateCounter;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdatePriority;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateStatus;
import dk.dbc.dataio.jobstore.distributed.hz.query.ByStatusAndSinkId;
import dk.dbc.dataio.jobstore.distributed.hz.query.ChunksToWaitFor;
import dk.dbc.dataio.jobstore.distributed.hz.query.JobChunksWaitForKey;
import dk.dbc.dataio.jobstore.distributed.hz.query.WaitingOn;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Startup
@DependsOn("DatabaseMigrator")
public class DependencyTrackingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTrackingService.class);
    private final IMap<TrackingKey, DependencyTracking> dependencyTracker = Hazelcast.Objects.DEPENDENCY_TRACKING.get();
    private final IMap<Integer, Map<ChunkSchedulingStatus, Integer>> countersMap = Hazelcast.Objects.SINK_STATUS.get();
    private final Map<Integer, JobSchedulerSinkStatus> sinkStatusMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void config() {
        init();
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Commence Hazelcast node shutdown");
        Hazelcast.shutdownNode();
        LOGGER.info("Hazelcast node shutdown completed");
    }

    public DependencyTrackingService init() {
        recountSinkStatus(Set.of());
        return this;
    }

    public TrackingKey add(DependencyTracking entity) {
        int sinkId = entity.getSinkId();
        Set<TrackingKey> waitingOn = entity.getWaitingOn();
        TrackingKey key = entity.getKey();
        dependencyTracker.set(key, entity);
        countersMap.putIfAbsent(sinkId, new EnumMap<>(ChunkSchedulingStatus.class));
        countersMap.executeOnKey(entity.getSinkId(), new UpdateCounter(entity.getStatus(), 1));
        removeDeadWOs(key, waitingOn);
        return key;
    }

    public void lock(TrackingKey key, Consumer<Void> block) {
        try {
            dependencyTracker.lock(key);
            block.accept(null);
        } finally {
            dependencyTracker.unlock(key);
        }
    }

    public int capacity(int sinkId, ChunkSchedulingStatus status) {
        if(status.getMax() == null) throw new IllegalArgumentException("This status does not have a capacity");
        return status.getMax() - getCount(sinkId, status);
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
            removeDeadWOs(key, entity.getWaitingOn());
        } catch (InterruptedException ignored) {
        } finally {
            try {
                dependencyTracker.unlock(key);
            } catch (Exception ignored) {}
        }
    }

    public List<DependencyTracking> getSnapshot(int jobId) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        Predicate<TrackingKey, DependencyTracking> p = e.key().get("jobId").equal(jobId);
        Collection<DependencyTracking> values = dependencyTracker.values(p);
        return values.stream().sorted(Comparator.comparing(k -> k.getKey().getChunkId())).collect(Collectors.toList());
    }

    public Stream<DependencyTrackingRO> getStaleDependencies(ChunkSchedulingStatus status, Duration timeout) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        @SuppressWarnings("unchecked")
        Predicate<TrackingKey, DependencyTracking> p = e.get("status").equal(status).and(e.get("lastModified").lessThan(Instant.now().minus(timeout)));
        return dependencyTracker.values(p).stream().map(DependencyTrackingRO.class::cast);
    }

    public DependencyTrackingRO get(TrackingKey key) {
        return dependencyTracker.get(key);
    }

    public int resetStatus(ChunkSchedulingStatus from, ChunkSchedulingStatus to, Integer... jobIds) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        @SuppressWarnings("unchecked")
        Predicate<TrackingKey, DependencyTracking> p = e.get("status").equal(from).and(e.key().get("jobId").in(jobIds));
        Set<TrackingKey> entries = dependencyTracker.keySet(p);
        entries.forEach(key -> setStatus(key, to));
        return entries.size();
    }

    public void removeJobId(int jobId) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        @SuppressWarnings("unchecked")
        Predicate<TrackingKey, DependencyTracking> p = e.key().get("jobId").equal(jobId);
        remove(p);
        recountSinkStatus(Set.of());
    }

    public void addToChunksToWaitFor(TrackingKey key, Set<TrackingKey> chunksToWaitFor) {
        Set<DependencyTrackingRO> allWOs = chunksToWaitFor.stream().filter(k -> !key.equals(k)).map(this::get).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<TrackingKey> reducedWOs = optimizeDependencies(allWOs);
        StatusChangeEvent changeEvent = dependencyTracker.executeOnKey(key, new AddTerminationWaitingOn(reducedWOs));
        updateCounters(Stream.of(changeEvent));
        removeDeadWOs(key, reducedWOs);
    }

    public Set<TrackingKey> removeFromWaitingOn(TrackingKey key) {
        RemoveWaitingOn processor = new RemoveWaitingOn(key);
        Map<TrackingKey, StatusChangeEvent> map = dependencyTracker.executeOnEntries(processor, processor);
        updateCounters(map.values().stream());
        return map.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public void setStatus(TrackingKey key, ChunkSchedulingStatus status) {
        StatusChangeEvent statusChangeEvent = dependencyTracker.executeOnKey(key, new UpdateStatus(status));
        updateCounters(Stream.of(statusChangeEvent));
    }

    public void remove(TrackingKey key) {
        DependencyTracking removed = dependencyTracker.remove(key);
        countersMap.executeOnKey(removed.getSinkId(), new UpdateCounter(removed.getStatus(), -1));
    }

    public void remove(Predicate<TrackingKey, DependencyTracking> predicate) {
        dependencyTracker.removeAll(predicate);
        recountSinkStatus(Set.of());
    }

    public Map<Integer, JobSchedulerSinkStatus> getSinkStatusMap() {
        return sinkStatusMap;
    }

    public JobSchedulerSinkStatus getSinkStatus(int sinkId) {
        return sinkStatusMap.computeIfAbsent(sinkId, k -> new JobSchedulerSinkStatus());
    }

    public void boostPriorities(Set<TrackingKey> keys, int priority) {
        if (priority > Priority.LOW.getValue()) {
            try {
                Map<TrackingKey, Set<TrackingKey>> map = dependencyTracker.executeOnKeys(keys, new UpdatePriority(priority));
                Set<TrackingKey> waitingOn = map.values().stream().filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toSet());
                if(!waitingOn.isEmpty()) boostPriorities(waitingOn, priority);
            } catch (Exception e) {
                LOGGER.error("Got exception while boosting key", e);
            }
        }
    }

    public void reload() {
        dependencyTracker.loadAll(true);
    }

    @Stopwatch
    public void recountSinkStatus(Set<Integer> sinkIds) {
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> map = statusCount(sinkIds);
        Set<Integer> resetSinks = sinkIds.isEmpty() ? map.keySet() : sinkIds;
        resetSinks.forEach(s -> sinkStatusMap.put(s, new JobSchedulerSinkStatus().bulk()));
        if(sinkIds.isEmpty()) countersMap.clear();
        else sinkIds.forEach(countersMap::remove);
        countersMap.putAll(map);
        LOGGER.info("Completed status map recount for {}", sinkIds);
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

    public Map<Integer, Integer> sinkStatusCount(ChunkSchedulingStatus status) {
        return dependencyTracker.aggregate(new BlockedCounter());
    }

    public Stream<DependencyTracking> findStream(ChunkSchedulingStatus status, Integer sinkId) {
        return dependencyTracker.values(new ByStatusAndSinkId(sinkId, status)).stream()
                .sorted(Comparator.comparing(DependencyTracking::getPriority).reversed());
    }

    public List<TrackingKey> find(ChunkSchedulingStatus status, int sinkId) {
        return dependencyTracker.values(new ByStatusAndSinkId(sinkId, status)).stream()
                .sorted(Comparator.comparing(DependencyTracking::getPriority).reversed())
                .map(DependencyTracking::getKey)
                .collect(Collectors.toList());
    }

    public List<TrackingKey> findChunksWaitingForMe(TrackingKey key, int sinkId) {
        return dependencyTracker.keySet(new WaitingOn(sinkId, key)).stream()
                .sorted(Comparator.comparing(TrackingKey::getJobId).thenComparing(TrackingKey::getChunkId))
                .collect(Collectors.toList());
    }

    /**
     * Finding lists of chunks in dependency Tracking with this jobId and MatchKey.
     * Note only First key in waitForKey is checked.
     *
     * @param sinkId     sinkId
     * @param jobId      jobId for which chunks to wait for barrier
     * @param waitForKey dataSetID
     * @return Returns List of Chunks To wait for.
     */
    public Set<TrackingKey> findJobBarrier(int sinkId, int jobId, Set<String> waitForKey) {
        return dependencyTracker.keySet(new JobChunksWaitForKey(sinkId, jobId, waitForKey));
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

    @Stopwatch
    public Set<TrackingKey> recheckBlocks() {
        Stream<DependencyTracking> stream = findStream(ChunkSchedulingStatus.BLOCKED, null);
        return stream.flatMap(this::checkBlocks).collect(Collectors.toSet());
    }

    /**
     * Finds chunks matching keys in given dependency tracking entity
     * and given barrier match key
     *
     * @param entity          dependency tracking entity
     * @param barrierMatchKey special key for barrier chunks
     * @return Returns set of chunks to wait for.
     */
    public Set<TrackingKey> findChunksToWaitFor(DependencyTracking entity, String barrierMatchKey) {
        if (entity.getMatchKeys().isEmpty() && barrierMatchKey == null) {
            return Collections.emptySet();
        }
        ChunksToWaitFor query = new ChunksToWaitFor(entity.getSinkId(), entity.getSubmitter(), entity.getHashes(), barrierMatchKey);
        Collection<DependencyTracking> values = dependencyTracker.values(query);
        return optimizeDependencies(values);
    }

    public static Set<TrackingKey> optimizeDependencies(Collection<? extends DependencyTrackingRO> dependencies) {
        if(dependencies.isEmpty()) return Set.of();
        Set<TrackingKey> keys = dependencies.stream()
                .map(DependencyTrackingRO::getWaitingOn)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return dependencies.stream().map(DependencyTrackingRO::getKey).filter(k -> !keys.contains(k)).collect(Collectors.toSet());
    }

    public Integer[] jobCount(int sinkId) {
        return dependencyTracker.aggregate(new JobCounter(sinkId));
    }

    @Readiness
    public HealthCheck readyCheck() {
        return () -> HealthCheckResponse.named("hazelcast-ready").status(Hazelcast.isReady()).build();
    }

    private Stream<TrackingKey> checkBlocks(DependencyTracking dt) {
        boolean unblock = dt.getWaitingOn().stream().anyMatch(d -> !dependencyTracker.containsKey(d));
        if(unblock) {
            dt.setWaitingOn(dt.getWaitingOn().stream().filter(dependencyTracker::containsKey).collect(Collectors.toList()));
            if(dt.getWaitingOn().isEmpty()) dt.setStatus(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING);
            dependencyTracker.set(dt.getKey(), dt);
            return Stream.of(dt.getKey());
        }
        return Stream.of();
    }

    private void removeDeadWOs(TrackingKey key, Set<TrackingKey> waitingOn) {
        Stream<StatusChangeEvent> changes = waitingOn.stream()
                .filter(k -> !dependencyTracker.containsKey(k))
                .map(k -> dependencyTracker.executeOnKey(key, new RemoveWaitingOn(k)));
        updateCounters(changes);
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
