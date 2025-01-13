package dk.dbc.dataio.jobstore.distributed.hz.query;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.WaitFor;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.RemoveWaitingOnSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.StatusChangeSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.TrackingKeySer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.UpdateCounterSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.UpdateStatusSer;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueriesTest extends JetTestSupport implements PostgresContainerJPAUtils {
    @org.junit.Test
    public void waitingOn() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        addToTrackersToMap(dependencies, 1, 0, 10000, (i, dt) -> dt);
        addToTrackersToMap(dependencies, 2, 0, 10000, (i, dt) -> dt.setWaitingOn(Set.of(new TrackingKey(1, 9999))));
        addToTrackersToMap(dependencies, 3, 0, 10000, (i, dt) -> dt.setWaitingOn(
                i < 1000 ? Set.of(new TrackingKey(2, 9999))
                        : Set.of(new TrackingKey(1, 9999), new TrackingKey(2, 9999))
        ));
        addToTrackersToMap(dependencies, 4, 1, 5, (i, dt) -> dt.setWaitingOn(Set.of(new TrackingKey(1, 9999))));
        Collection<DependencyTracking> result = dependencies.values(new WaitingOn(0, new TrackingKey(1, 9999)));
        assertEquals("There should be 19000 hits", 19000, result.size());
        assertTrue("job 2, chunk 0 should be among them",
                result.stream().map(DependencyTracking::getKey).anyMatch(new TrackingKey(2, 0)::equals));
        assertTrue("job 4 chunks should not be among them",
                result.stream().map(DependencyTracking::getKey).map(TrackingKey::getJobId).noneMatch(jobId -> jobId == 4));
    }

    @org.junit.Test
    public void byStatusAndSinkId() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        addToTrackersToMap(dependencies, 1, 0, 20, this::setStatusByChunkMod);
        addToTrackersToMap(dependencies, 2, 1, 20, this::setStatusByChunkMod);
        addToTrackersToMap(dependencies, 3, 0, 20, this::setStatusByChunkMod);
        Collection<DependencyTracking> result = dependencies.values(new ByStatusAndSinkId(1, ChunkSchedulingStatus.BLOCKED));
        assertEquals("There should be 3 hits", 3, result.size());
        assertTrue("Jobs for sink 0 should not have any hits", result.stream().mapToInt(DependencyTracking::getSinkId).noneMatch(i -> i == 0));
    }

    @org.junit.Test
    public void chunksToWaitFor() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        DependencyTrackingService service = new DependencyTrackingService();
        Set<String> matchKeys = Set.of("hest", "lasagne", "pizza");
        addToTrackersWithMatchKey(dependencies, 1, 0, 4, (Integer i) -> i < 2 ? Set.of("hest") : Set.of("bulgur"));
        addToTrackersWithMatchKey(dependencies, 2, 0, 4, (Integer i) -> i < 2 ? Set.of("hest") : Set.of("bulgur"));
        addToTrackersWithMatchKey(dependencies, 3, 0, 4, i -> Set.of("bulgur"));
        addToTrackersWithMatchKey(dependencies, 4, 1, 4, i -> Set.of("hest"));
        Set<TrackingKey> waitForHorses = service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(5, 0), 0, 0, Set.of("hest")), null);
        System.out.println("Waiting for chunks to wait for " + waitForHorses.size() + " hits");
        Set<TrackingKey> expectHorses = dependencies.values().stream()
                .filter(dt -> dt.getSinkId() == 0)
                .filter(dt -> dt.getSubmitter() == 0)
                .filter(dt -> dt.getWaitFor().contains(new WaitFor(0, 0, "hest")))
                .map(DependencyTracking::getKey)
                .collect(Collectors.toSet());
        assertEquals(expectHorses, waitForHorses);
        Set<TrackingKey> waitForAll = service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(5, 0), 0, 0, Set.of("hest", "bulgur")), null);
        Set<TrackingKey> expectAll = dependencies.values().stream()
                .filter(dt -> dt.getSinkId() == 0)
                .filter(dt -> dt.getSubmitter() == 0)
                .filter(dt -> Set.of("hest", "bulgur").stream().anyMatch(s -> dt.getWaitFor().contains(new WaitFor(0, 0, s))))
                .map(DependencyTracking::getKey)
                .collect(Collectors.toSet());
        assertEquals(expectAll, waitForAll);
    }

    @Override
    protected HazelcastInstance createHazelcastInstance() {
        HazelcastInstance instance = createHazelcastInstance(makeConfig());
        Hazelcast.testInstance(instance);
        return instance;
    }

    private Config makeConfig() {
        Config config = smallInstanceConfig();
        List<CompactSerializer<?>> compactSerializers = List.of(new RemoveWaitingOnSer(), new StatusChangeSer(), new TrackingKeySer(), new UpdateCounterSer(), new UpdateStatusSer());
        compactSerializers.forEach(ser -> config.getSerializationConfig().getCompactSerializationConfig().addSerializer(ser));
        return config;
    }

    @org.junit.Test
    public void jobChunksWaitForKey() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        Set<String> matchKeys = Set.of("hest", "lasagne", "pizza");
        addToTrackersWithMatchKey(dependencies, 1, 0, 20, i -> i < 10 ? Set.of("hest") : Set.of("bulgur"));
        addToTrackersWithMatchKey(dependencies, 2, 0, 20, i -> i < 10 ? Set.of("hest") : Set.of("bulgur"));
        addToTrackersWithMatchKey(dependencies, 3, 0, 20, i -> Set.of("bulgur"));
        addToTrackersWithMatchKey(dependencies, 4, 1, 20, i -> Set.of("hest"));
        Collection<DependencyTracking> result = dependencies.values(new JobChunksWaitForKey(0, 1, matchKeys));
        assertEquals("There should be 20 hits from job 1 and 10 from job 2", 30, result.size());
        assertTrue(result.stream().map(DependencyTracking::getKey).mapToInt(TrackingKey::getJobId).allMatch(jobId -> jobId == 1 || jobId == 2));
    }

    @org.junit.Test
    public void find() {
        Hazelcast.testInstance(createHazelcastInstance());
        DependencyTrackingService service = new DependencyTrackingService();
        List<DependencyTracking> trackers = IntStream.range(0, 30)
                .mapToObj(i -> new DependencyTracking(new TrackingKey(i / 5, i % 5), 1, 0, Set.of())
                    .setPriority(i % 4)
                    .setStatus(i % 2 == 0 ? READY_FOR_PROCESSING : SCHEDULED_FOR_PROCESSING))
                .toList();
        trackers.forEach(service::add);
        Collection<DependencyTracking> result = service.findDependencies(SCHEDULED_FOR_PROCESSING, 1, 10);
        List<DependencyTracking> expected = trackers.stream()
                .filter(dt -> dt.getSinkId() == 1)
                .filter(dt -> dt.getStatus() == SCHEDULED_FOR_PROCESSING)
                .sorted(Comparator.comparing(DependencyTracking::getPriority).reversed().thenComparing(DependencyTracking::getKey))
                .limit(10)
                .toList();
        assertEquals(new HashSet<>(expected), new HashSet<>(result));
    }

    private DependencyTracking setStatusByChunkMod(Integer chunkId, DependencyTracking dt) {
        return dt.setStatus(ChunkSchedulingStatus.values()[chunkId % ChunkSchedulingStatus.values().length]);
    }

    private void addToTrackersWithMatchKey(Map<TrackingKey, DependencyTracking> map, int jobId, int sinkId, int chunks, Function<Integer, Set<String>> matchKeysFunc) {
        addToTrackersWithMatchKey(map, jobId, sinkId, 0, chunks, matchKeysFunc);
    }

    private void addToTrackersWithMatchKey(Map<TrackingKey, DependencyTracking> map, int jobId, int sinkId, int submitter, int chunks, Function<Integer, Set<String>> matchKeysFunc) {
        IntStream.range(0, chunks)
                .mapToObj(chunkId -> new TrackingKey(jobId, chunkId))
                .map(key -> new DependencyTracking(key, sinkId, submitter, matchKeysFunc.apply(key.getChunkId())))
                .forEach(dt -> map.put(dt.getKey(), dt));
    }

    private void addToTrackersToMap(Map<TrackingKey, DependencyTracking> map, int jobId, int sinkId, int chunks, BiFunction<Integer, DependencyTracking, DependencyTracking> dtModifier) {
        IntStream.range(0, chunks)
                .mapToObj(chunkId -> new TrackingKey(jobId, chunkId))
                .map(key -> new DependencyTracking(key, sinkId, 0))
                .map(dt -> dtModifier.apply(dt.getKey().getChunkId(), dt))
                .forEach(dt -> map.put(dt.getKey(), dt));
    }
}
