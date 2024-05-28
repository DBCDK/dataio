package dk.dbc.dataio.jobstore.distributed.hz.query;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.commons.utils.lang.Hashcode;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.RemoveWaitingOnSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.StatusChangeSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.TrackingKeySer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.UpdateCounterSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.UpdateStatusSer;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueriesTest extends JetTestSupport implements PostgresContainerJPAUtils {
    @Test
    public void waitingOn() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        addToTrackersToMap(dependencies, 1, 0, 10000, (i, dt)-> dt);
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

    @Test
    public void byStatusAndSinkId() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        addToTrackersToMap(dependencies, 1, 0, 20, this::setStatusByChunkMod);
        addToTrackersToMap(dependencies, 2, 1, 20, this::setStatusByChunkMod);
        addToTrackersToMap(dependencies, 3, 0, 20, this::setStatusByChunkMod);
        Collection<DependencyTracking> result = dependencies.values(new ByStatusAndSinkId(1, ChunkSchedulingStatus.BLOCKED));
        assertEquals("There should be 3 hits", 3, result.size());
        assertTrue("Jobs for sink 0 should not have any hits", result.stream().mapToInt(DependencyTracking::getSinkId).noneMatch(i -> i == 0));
    }

    @Test
    public void chunksToWaitFor() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        Set<String> matchKeys = Set.of("hest", "lasagne", "pizza");
        Set<String> falseKeys = Set.of("bulgur", "linser");
        addToTrackersToMap(dependencies, 1, 0, 20, (i, dt) -> dt.setMatchKeys(matchKeys).setSubmitter(123456));
        addToTrackersToMap(dependencies, 2, 0, 20, (i, dt) -> dt.setMatchKeys(falseKeys));
        addToTrackersToMap(dependencies, 3, 0, 20, (i, dt) -> dt);
        addToTrackersToMap(dependencies, 4, 0, 20, (i, dt) -> dt.setMatchKeys(matchKeys));
        addToTrackersToMap(dependencies, 5, 0, 20, (i, dt) -> dt.setMatchKeys(matchKeys).setSubmitter(123456));
        addToTrackersToMap(dependencies, 6, 1, 20, (i, dt) -> dt.setMatchKeys(matchKeys).setSubmitter(123456));
        Integer[] hashes = matchKeys.stream().limit(1).map(Hashcode::of).toArray(Integer[]::new);
        Collection<DependencyTracking> result = dependencies.values(new ChunksToWaitFor(0, 123456, hashes, ""));
        assertEquals(40, result.size());
        assertTrue(result.stream().map(DependencyTracking::getKey).mapToInt(TrackingKey::getJobId).allMatch(jobId -> jobId == 1 || jobId == 5));
    }

    @Override
    protected HazelcastInstance createHazelcastInstance() {
        return createHazelcastInstance(makeConfig());
    }

    private Config makeConfig() {
        Config config = smallInstanceConfig();
        List<CompactSerializer<?>> compactSerializers = List.of(new RemoveWaitingOnSer(), new StatusChangeSer(), new TrackingKeySer(), new UpdateCounterSer(), new UpdateStatusSer());
        compactSerializers.forEach(ser -> config.getSerializationConfig().getCompactSerializationConfig().addSerializer(ser));
        return config;
    }

    @Test
    public void jobChunksWaitForKey() {
        IMap<TrackingKey, DependencyTracking> dependencies = createHazelcastInstance().getMap("dependencies");
        Set<String> matchKeys = Set.of("hest", "lasagne", "pizza");
        addToTrackersToMap(dependencies, 1, 0, 20, (i, dt) -> dt.setMatchKeys(i < 10 ? Set.of("hest") : Set.of("bulgur")));
        addToTrackersToMap(dependencies, 2, 0, 20, (i, dt) -> dt.setMatchKeys(i < 10 ? Set.of("hest") : Set.of("bulgur")));
        addToTrackersToMap(dependencies, 3, 0, 20, (i, dt) -> dt.setMatchKeys(Set.of("bulgur")));
        addToTrackersToMap(dependencies, 4, 1, 20, (i, dt) -> dt.setMatchKeys(Set.of("hest")));
        Collection<DependencyTracking> result = dependencies.values(new JobChunksWaitForKey(0, 1, matchKeys));
        assertEquals("There should be 20 hits from job 1 and 10 from job 2", 30, result.size());
        assertTrue(result.stream().map(DependencyTracking::getKey).mapToInt(TrackingKey::getJobId).allMatch(jobId -> jobId == 1 || jobId == 2));
    }

    private DependencyTracking setStatusByChunkMod(Integer chunkId, DependencyTracking dt) {
        return dt.setStatus(ChunkSchedulingStatus.values()[chunkId % ChunkSchedulingStatus.values().length]);
    }


    private void addToTrackersToMap(Map<TrackingKey, DependencyTracking> map, int jobId, int sinkId, int chunks, BiFunction<Integer, DependencyTracking, DependencyTracking> dtModifier) {
        IntStream.range(0, chunks)
                .mapToObj(chunkId -> new TrackingKey(jobId, chunkId))
                .map(key -> new DependencyTracking(key, sinkId))
                .map(dt -> dtModifier.apply(dt.getKey().getChunkId(), dt))
                .forEach(dt -> map.put(dt.getKey(), dt));
    }
}
