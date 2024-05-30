package dk.dbc.dataio.jobstore.distributed.hz.aggregator;

import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.map.IMap;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static org.junit.Assert.assertEquals;

public class AggregatorsTest extends JetTestSupport {
    private static final int SINKS = 3;
    private static final int JOBS = 5;

    private IMap<TrackingKey, DependencyTracking> map;
    @Before
    public void init() {
        map = createHazelcastInstance().getMap("dependencies");
    }

    @Test
    public void blockedCounter() {
        addTrackers(20);
        Map<Integer, Integer> aggregate = map.aggregate(new BlockedCounter());
        Map<Integer, Integer> expected = Map.of(0, 3, 1, 2, 2, 2);
        assertEquals("We should have 3 sinks with 3, 3 and 4 blocked", expected, aggregate);
    }

    @Test
    public void jobCounter() {
        List<DependencyTracking> list = addTrackers(50);
        Integer[] aggregate = map.aggregate(new JobCounter(0));
        long jobs = list.stream().mapToInt(dt -> dt.getKey().getJobId()).distinct().count();
        long chunks = list.stream().filter(dt -> dt.getSinkId() == 0).count();
        assertEquals("There should be " + jobs + "jobs for sink 0", jobs, aggregate[0].longValue());
        assertEquals("There should be " + chunks + "chunks for sink 0", chunks, aggregate[1].longValue());
    }

    @Test
    public void sinkStatusCounter() {
        List<DependencyTracking> list = addTrackers(50);
        int aggregate = map.aggregate(new SinkStatusCounter(0, QUEUED_FOR_PROCESSING));
        long expected = list.stream().filter(dt -> dt.getSinkId() == 0).filter(dt -> dt.getStatus() == QUEUED_FOR_PROCESSING).count();
        assertEquals("There should be " + expected + " with status queued for processing in sink 0", expected, aggregate);
    }

    @Test
    public void statusCounter() {
        List<DependencyTracking> list = addTrackers(50);
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> aggregateAll = map.aggregate(new StatusCounter(Set.of()));
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> expect = Map.of(
                0, countSink(0, list),
                1, countSink(1, list),
                2, countSink(2, list));
        assertEquals("Aggregating all sinks", expect, aggregateAll);
        Map<Integer, Map<ChunkSchedulingStatus, Integer>> aggregateSink0 = map.aggregate(new StatusCounter(Set.of(0)));
        assertEquals("Aggregating sink 0", Map.of(0, countSink(0, list)), aggregateSink0);
    }

    private Map<ChunkSchedulingStatus, Integer> countSink(int sink, List<DependencyTracking> list) {
        Map<ChunkSchedulingStatus, AtomicInteger> counter = new EnumMap<>(ChunkSchedulingStatus.class);
        list.stream().filter(dt -> dt.getSinkId() == sink).forEach(dt -> counter.computeIfAbsent(dt.getStatus(), k -> new AtomicInteger(0)).incrementAndGet());
        return counter.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private List<DependencyTracking> addTrackers(int count) {
        List<DependencyTracking> list = IntStream.range(0, 50).mapToObj(this::makeTracker).collect(Collectors.toList());
        list.forEach(dt -> map.put(dt.getKey(), dt));
        return list;
    }

    private DependencyTracking makeTracker(int i) {
        return new DependencyTracking(new TrackingKey(i % JOBS, i / JOBS), i % SINKS).setStatus(ChunkSchedulingStatus.values()[i % ChunkSchedulingStatus.values().length]);
    }
}
