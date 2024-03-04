package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.Predicates;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.distributed.QueueSubmitMode;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hzqueries.BlockedCounter;
import dk.dbc.dataio.jobstore.distributed.hzqueries.ByStatusAndSinkId;
import dk.dbc.dataio.jobstore.distributed.hzqueries.ChunksToWaitFor;
import dk.dbc.dataio.jobstore.distributed.hzqueries.JobCounter;
import dk.dbc.dataio.jobstore.distributed.hzqueries.RemoveWaitingOnProcessor;
import dk.dbc.dataio.jobstore.distributed.hzqueries.SinkStatusCounter;
import dk.dbc.dataio.jobstore.distributed.hzqueries.StatusCounter;
import dk.dbc.dataio.jobstore.distributed.hzqueries.UpdateStatusProcessor;
import dk.dbc.dataio.jobstore.distributed.hzqueries.WaitForKey;
import dk.dbc.dataio.jobstore.distributed.hzqueries.WaitingOn;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Startup
public class DependencyTrackingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTrackingService.class);
    private final IMap<TrackingKey, DependencyTracking> dependencyTracker = Hazelcast.Objects.DEPENDENCY_TRACKING.get();
    private Map<Integer, JobSchedulerSinkStatus> sinkStatusMap;

    @PostConstruct
    public void init() {
        sinkStatusMap = new ConcurrentHashMap<>(); //hc.getInstance().getReplicatedMap("sink.status");
        dependencyTracker.addEntryListener(new MapListenerAdapter<TrackingKey, DependencyTracking>() {
            @Override
            public void entryAdded(EntryEvent<TrackingKey, DependencyTracking> event) {
//                if(!Hazelcast.isMaster()) return;
                DependencyTracking dt = event.getValue();
                JobSchedulerSinkStatus status = statusFor(dt);
                dt.getStatus().incSinkStatusCount(status);
                LOGGER.debug("Map listener added tracker {} with status {}", dt.getKey(), status);
            }

            @Override
            public void entryRemoved(EntryEvent<TrackingKey, DependencyTracking> event) {
//                if(!Hazelcast.isMaster()) return;
                DependencyTracking dt = event.getOldValue();
                JobSchedulerSinkStatus status = statusFor(dt);
                dt.getStatus().decSinkStatusCount(status);
                LOGGER.debug("Map listener removed tracker {} with status {}", dt.getKey(), status);
            }

            @Override
            public void entryUpdated(EntryEvent<TrackingKey, DependencyTracking> event) {
//                if(!Hazelcast.isMaster()) return;
                DependencyTracking old = event.getOldValue();
                DependencyTracking dt = event.getValue();
                if(old.getStatus() == dt.getStatus()) return;
                JobSchedulerSinkStatus status = statusFor(dt);
                old.getStatus().decSinkStatusCount(status);
                dt.getStatus().incSinkStatusCount(status);
                LOGGER.debug("Map listener updated tracker {}: {} -> {}, status: {}", dt.getKey(), old.getStatus().name(), dt.getStatus().name(), status);
            }

            private JobSchedulerSinkStatus statusFor(DependencyTracking dt) {
                return sinkStatusMap.computeIfAbsent(dt.getSinkId(), id -> new JobSchedulerSinkStatus());
            }
        }, true);
        recountSinkStatus(Set.of());
    }

    public TrackingKey add(DependencyTracking entity) {
        Set<TrackingKey> waitingOn = entity.getWaitingOn();
        dependencyTracker.set(entity.getKey(), entity);
        waitingOn.stream()
                .filter(k -> !dependencyTracker.containsKey(k))
                .forEach(k -> dependencyTracker.executeOnKey(k, new RemoveWaitingOnProcessor(k)));
        return entity.getKey();
    }

    public void lock(TrackingKey key, Consumer<Void> block) {
        try {
            dependencyTracker.lock(key);
            block.accept(null);
        } finally {
            dependencyTracker.unlock(key);
        }
    }


    public void modify(TrackingKey key, Consumer<DependencyTracking> consumer) {
        try {
            dependencyTracker.tryLock(key, 2, TimeUnit.MINUTES);
            DependencyTracking entity = dependencyTracker.get(key);
            if(entity == null) {
                LOGGER.info("Unable to modify tracker {} as i has been deleted", key);
                return;
            }
            consumer.accept(entity);
            entity.updateLastModified();
            entity.setWaitingOn(entity.getWaitingOn().stream().filter(dependencyTracker::containsKey).collect(Collectors.toList()));
            dependencyTracker.set(key, entity);
        } catch (InterruptedException ignored) {
        } finally {
            try {
                dependencyTracker.unlock(key);
            } catch (Exception ignored) {}
        }
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
    }

    public Set<TrackingKey> removeFromWaitingOn(TrackingKey key) {
        RemoveWaitingOnProcessor processor = new RemoveWaitingOnProcessor(key);
        Map<TrackingKey, Boolean> map = dependencyTracker.executeOnEntries(processor, processor);
        return map.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public void setStatus(TrackingKey key, ChunkSchedulingStatus status) {
        dependencyTracker.executeOnKey(key, new UpdateStatusProcessor(status));
    }

    public void remove(TrackingKey key) {
        dependencyTracker.remove(key);
    }

    public void remove(Predicate<TrackingKey, DependencyTracking> predicate) {
        dependencyTracker.removeAll(predicate);
    }

    public Map<Integer, JobSchedulerSinkStatus> getSinkStatusMap() {
        return sinkStatusMap;
    }

    public JobSchedulerSinkStatus getSinkStatus(int sinkId) {
        return sinkStatusMap.computeIfAbsent(sinkId, k -> new JobSchedulerSinkStatus());
    }

    public void boostPriorities(Set<TrackingKey> keys, int priority) {
        if (priority > Priority.LOW.getValue()) {
            for (TrackingKey key : keys) {
                modify(key, dependency -> {
                    if (dependency.getPriority() < priority) {
                        dependency.setPriority(priority);
                        dependencyTracker.set(key, dependency);
                        boostPriorities(dependency.getWaitingOn(), priority);
                    }
                });
            }
        }
    }

    public void reload() {
        dependencyTracker.loadAll(true);
    }

    public void recountSinkStatus(Set<Integer> sinkIds) {
        Map<Integer, JobSchedulerSinkStatus> map = statusCount(sinkIds);
        map.values().forEach(s -> {
            s.getDeliveringStatus().setMode(QueueSubmitMode.BULK);
            s.getProcessingStatus().setMode(QueueSubmitMode.BULK);
        });
        sinkStatusMap.clear();
        sinkStatusMap.putAll(map);
    }

    public int statusCount(int sinkId, ChunkSchedulingStatus status) {
        return dependencyTracker.aggregate(new SinkStatusCounter(sinkId, status));
    }

    public Map<Integer, JobSchedulerSinkStatus> statusCount(Set<Integer> sinkIds) {
        StatusCounter statusCounter = new StatusCounter(sinkIds);
        return dependencyTracker.aggregate(statusCounter);
    }

    public Map<Integer, Integer> sinkStatusCount(ChunkSchedulingStatus status) {
        return dependencyTracker.aggregate(new BlockedCounter(status));
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
     * @param jobId      jobId for witch chunks to wait for barrier
     * @param waitForKey dataSetID
     * @return Returns List of Chunks To wait for.
     */
    public List<TrackingKey> findJobBarrier(int sinkId, int jobId, Set<String> waitForKey) {
        return dependencyTracker.keySet(new WaitForKey(sinkId, jobId, waitForKey)).stream()
                .sorted(Comparator.comparing(TrackingKey::getJobId).thenComparing(TrackingKey::getChunkId))
                .collect(Collectors.toList());
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

    public static Set<TrackingKey> optimizeDependencies(Collection<? extends DependencyTracking> dependencies) {
        if(dependencies.isEmpty()) return Set.of();
        Set<TrackingKey> keys = dependencies.stream()
                .map(DependencyTracking::getWaitingOn)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return dependencies.stream().map(DependencyTracking::getKey).filter(k -> !keys.contains(k)).collect(Collectors.toSet());
    }

    public Integer[] jobCount(int sinkId) {
        return dependencyTracker.aggregate(new JobCounter(sinkId));
    }
}
