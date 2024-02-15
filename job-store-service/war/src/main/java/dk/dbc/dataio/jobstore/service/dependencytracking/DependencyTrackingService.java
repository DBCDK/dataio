package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.Predicates;
import com.hazelcast.replicatedmap.ReplicatedMap;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries.BlockedCounter;
import dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries.ByStatusAndSinkId;
import dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries.ChunksToWaitFor;
import dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries.StatusCounter;
import dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries.WaitForKey;
import dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries.WaitingOn;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class DependencyTrackingService {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private HazelcastService hz;
    private IMap<DependencyTracking.Key, DependencyTracking> dependencyTracker;
    private ReplicatedMap<Integer, JobSchedulerSinkStatus> sinkStatusMap;

    @PostConstruct
    public void init() {
        dependencyTracker = hz.getInstance().getMap("dependencies");
        sinkStatusMap = hz.getInstance().getReplicatedMap("sink.status");
        dependencyTracker.addEntryListener(new MapListenerAdapter<DependencyTracking.Key, DependencyTracking>() {
            @Override
            public void entryAdded(EntryEvent<DependencyTracking.Key, DependencyTracking> event) {
                if(!hz.isMaster()) return;
                DependencyTracking dt = event.getValue();
                dt.getStatus().incSinkStatusCount(statusFor(dt));
            }

            @Override
            public void entryRemoved(EntryEvent<DependencyTracking.Key, DependencyTracking> event) {
                if(!hz.isMaster()) return;
                DependencyTracking dt = event.getValue();
                dt.getStatus().decSinkStatusCount(statusFor(dt));
            }

            @Override
            public void entryUpdated(EntryEvent<DependencyTracking.Key, DependencyTracking> event) {
                if(!hz.isMaster()) return;
                DependencyTracking old = event.getOldValue();
                DependencyTracking dt = event.getValue();
                if(old.getStatus() == dt.getStatus()) return;
                old.getStatus().decSinkStatusCount(statusFor(old));
                dt.getStatus().incSinkStatusCount(statusFor(dt));
            }

            private JobSchedulerSinkStatus statusFor(DependencyTracking dt) {
                return sinkStatusMap.computeIfAbsent(dt.getSinkId(), id -> new JobSchedulerSinkStatus());
            }
        }, true);
    }

    public DependencyTracking.Key add(DependencyTracking entity) {
        dependencyTracker.set(entity.getKey(), entity);
        return entity.getKey();
    }

    public void modify(DependencyTracking.Key key, Consumer<DependencyTracking> consumer) {
        try {
            dependencyTracker.tryLock(key, 2, TimeUnit.MINUTES);
            DependencyTracking entity = dependencyTracker.get(key);
            consumer.accept(entity);
            entity.updateLastModified();
            dependencyTracker.set(key, entity);
            dependencyTracker.unlock(key);
        } catch (InterruptedException ie) {
            try {
                dependencyTracker.unlock(key);
            } catch (Exception ignored) {}
        }
    }

    public Stream<DependencyTrackingRO> getStaleDependencies(ChunkSchedulingStatus status, Duration timeout) {
        PredicateBuilder.EntryObject e = Predicates.newPredicateBuilder().getEntryObject();
        Predicate<DependencyTracking.Key, DependencyTracking> p = e.get("status").equal(status).and(e.get("lastModified").lessThan(Instant.now().minus(timeout)));
        return dependencyTracker.values(p).stream().map(DependencyTrackingRO.class::cast);
    }

    public DependencyTrackingRO get(DependencyTracking.Key key) {
        return dependencyTracker.get(key);
    }

    public void remove(DependencyTracking.Key key) {
        dependencyTracker.remove(key);
    }

    public Map<Integer, JobSchedulerSinkStatus> getSinkStatusMap() {
        return sinkStatusMap;
    }

    public JobSchedulerSinkStatus getSinkStatus(int sinkId) {
        return sinkStatusMap.computeIfAbsent(sinkId, k -> new JobSchedulerSinkStatus());
    }

    public void boostPriorities(Set<DependencyTracking.Key> keys, int priority) {
        if (priority > Priority.LOW.getValue()) {
            for (DependencyTracking.Key key : keys) {
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

    public void recountSinkStatus(Set<Integer> sinkIds) {
        Map<Integer, JobSchedulerSinkStatus> map = statusCount(sinkIds);
        map.values().forEach(s -> {
            s.getDeliveringStatus().setMode(JobSchedulerBean.QueueSubmitMode.BULK);
            s.getProcessingStatus().setMode(JobSchedulerBean.QueueSubmitMode.BULK);
        });
        sinkStatusMap.clear();
        sinkStatusMap.putAll(map);
    }

    public Map<Integer, JobSchedulerSinkStatus> statusCount(Set<Integer> sinkIds) {
        StatusCounter statusCounter = new StatusCounter(sinkIds);
        return dependencyTracker.aggregate(statusCounter);
    }

    public Map<Integer, Integer> sinkStatusCount(ChunkSchedulingStatus status) {
        return dependencyTracker.aggregate(new BlockedCounter(status));
    }

    public Stream<DependencyTracking> findStream(ChunkSchedulingStatus status, int sinkId) {
        return dependencyTracker.values(new ByStatusAndSinkId(sinkId, status)).stream()
                .sorted(Comparator.comparing(DependencyTracking::getPriority).reversed());
    }

    public List<DependencyTracking.Key> find(ChunkSchedulingStatus status, int sinkId) {
        return dependencyTracker.values(new ByStatusAndSinkId(sinkId, status)).stream()
                .sorted(Comparator.comparing(DependencyTracking::getPriority).reversed())
                .map(DependencyTracking::getKey)
                .collect(Collectors.toList());
    }

    public List<DependencyTracking.Key> findChunksWaitingForMe(DependencyTracking.Key key, int sinkId) {
        return dependencyTracker.keySet(new WaitingOn(sinkId, key)).stream().
                sorted(Comparator.comparing(DependencyTracking.Key::getJobId).thenComparing(DependencyTracking.Key::getChunkId))
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
    public List<DependencyTracking.Key> findJobBarrier(int sinkId, int jobId, Set<String> waitForKey) {
        return dependencyTracker.keySet(new WaitForKey(sinkId, jobId, waitForKey)).stream()
                .sorted(Comparator.comparing(DependencyTracking.Key::getJobId).thenComparing(DependencyTracking.Key::getChunkId))
                .collect(Collectors.toList());
    }

    /**
     * Ascertains if a chunk is currently scheduled
     *
     * @param chunkEntity chunk entity representing the chunk
     * @return true if scheduled, false if not
     */
    public boolean isScheduled(ChunkEntity chunkEntity) {
        return dependencyTracker.containsKey(new DependencyTracking.Key(chunkEntity.getKey().getJobId(), chunkEntity.getKey().getId()));
    }

    /**
     * Finds chunks matching keys in given dependency tracking entity
     * and given barrier match key
     *
     * @param entity          dependency tracking entity
     * @param barrierMatchKey special key for barrier chunks
     * @return Returns set of chunks to wait for.
     */
    public Set<DependencyTracking.Key> findChunksToWaitFor(DependencyTracking entity, String barrierMatchKey) {
        if (entity.getMatchKeys().isEmpty() && barrierMatchKey == null) {
            return Collections.emptySet();
        }
        ChunksToWaitFor query = new ChunksToWaitFor(entity.getSinkId(), entity.getSubmitterNumber(), entity.getHashes(), barrierMatchKey);
        Collection<DependencyTracking> values = dependencyTracker.values(query);
        return optimizeDependencies(values);
    }

    public static Set<DependencyTracking.Key> optimizeDependencies(Collection<? extends DependencyTracking> dependencies) {
        if(dependencies.isEmpty()) return Set.of();
        Set<DependencyTracking.Key> keys = dependencies.stream()
                .map(DependencyTracking::getWaitingOn)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return dependencies.stream().map(DependencyTracking::getKey).filter(k -> !keys.contains(k)).collect(Collectors.toSet());
    }
}
