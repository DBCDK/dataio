package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.JetTestSupport;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.WaitFor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingServiceTest.TestSet.T1_1;
import static dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingServiceTest.TestSet.T1_2;
import static dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingServiceTest.TestSet.T1_3;
import static dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingServiceTest.TestSet.T2_1;
import static dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingServiceTest.TestSet.T2_4;
import static dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingServiceTest.TestSet.T2_5;
import static dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingServiceTest.TestSet.makeTestSet;
import static java.util.Set.of;

public class DependencyTrackingServiceTest extends JetTestSupport {
    HazelcastInstance hz;

    public enum TestSet {
        N0_1(0, 1, 1, 0, of("K1")),
        N0_2(0, 2, 0, 1, of("K2")),
        T1_1(1, 1, of("K1", "K3")),
        T1_2(1, 2, of("K2")),
        T1_3(1, 3, of()),
        T2_1(2, 1, of("K1", "K2"), T1_1, T1_2),
        T2_2(2, 2, of("K1"), T2_1),
        T2_3(2, 3, of("K2"), T2_1),
        T2_4(2, 4, of("K3"), T1_1),
        T2_5(2, 5, of("K1", "K2", "K3"), T2_2, T2_3, T2_4);

        TestSet(int jobId, int chunkId, Set<String> matchKeys, TestSet... expected) {
            this(jobId, chunkId, 0, 0, matchKeys, expected);
        }

        TestSet(int jobId, int chunkId, int sinkId, int submitter, Set<String> matchKeys, TestSet... expected) {
            dt = new DependencyTracking(new TrackingKey(jobId, chunkId), sinkId, submitter, matchKeys);
            expectedWo = Arrays.stream(expected).map(t -> t.dt.getKey()).collect(Collectors.toSet());
        }

        public DependencyTracking dt;
        public final Set<TrackingKey> expectedWo;

        public static List<TestSet> makeTestSet(TestSet... ts) {
            List<TestSet> values = ts.length == 0 ? List.of(values()) : Arrays.asList(ts);
            values.forEach(v -> v.dt.setWaitingOn(new HashSet<>()));
            return values;
        }
    }

    @Test
    public void testWaitingOn() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        List<TestSet> trackers = makeTestSet();
        trackers.forEach(tracker -> service.addAndBuildDependencies(tracker.dt, null));
        trackers.forEach(tracker -> Assert.assertEquals(
                "Tracker " + tracker.dt.getKey() + " should have the expected waitingOn keys",
                tracker.expectedWo,
                service.get(tracker.dt.getKey()).getWaitingOn()));
        service.removeFromWaitingOn(T1_1.dt.getKey());
        Assert.assertTrue("When T1_1 completed, T2_4 should have an empty waitingOn", service.get(T2_4.dt.getKey()).getWaitingOn().isEmpty());
        service.removeFromWaitingOn(T1_2.dt.getKey());
        Assert.assertTrue("When T1_1 and T1_2 are completed, T2_1 should have an empty waitingOn", service.get(T2_1.dt.getKey()).getWaitingOn().isEmpty());
        service.remove(T2_5.dt.getKey());
        Set<WaitFor> trackerKeySet = service.getTrackerMapSnapshot().keySet();
        Assert.assertTrue("When T2_5 is removed the tracker map should not contain entries for sink/submitter 0",
                trackerKeySet.stream().noneMatch(wf -> wf.sinkId() == 0 && wf.submitter() == 0));
    }

    @Test
    public void testBarrierKey() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        List<TestSet> trackers = makeTestSet(T1_1, T1_2, T1_3);
        trackers.forEach(tracker -> service.addAndBuildDependencies(tracker.dt, "hest"));
        Map<TestSet, Set<TrackingKey>> expected = Map.of(T1_1, wo(), T1_2, wo(T1_1), T1_3, wo(T1_2));
        trackers.forEach(tracker -> Assert.assertEquals(
                "Tracker " + tracker.dt.getKey() + " should have the expected waitingOn keys",
                expected.get(tracker),
                service.get(tracker.dt.getKey()).getWaitingOn()));
    }

    @Test
    public void testBarrierKeyFail() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        List<TestSet> trackers = makeTestSet();
        trackers.forEach(tracker -> service.addAndBuildDependencies(tracker.dt, "hest"));
        Assert.assertNotEquals(T1_3.expectedWo, service.get(T1_3.dt.getKey()).getWaitingOn());
    }

    @Test
    public void testTrackerRebuild() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        List<TestSet> trackers = makeTestSet();
        trackers.forEach(tracker -> service.addAndBuildDependencies(tracker.dt, null));
        Map<WaitFor, TrackingKey> trackerMap = service.rebuildTrackerMap();
        Assert.assertEquals("A rebuild tracker map, must be identical to one that has developed over time", service.getTrackerMapSnapshot(), trackerMap);
    }

    @Test @Ignore
    public void testPerformance() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        Set<String> matchKeys = new HashSet<>();
        Random random = new Random();
        IntStream.range(0, 10000).forEach(i -> {
            TrackingKey key = new TrackingKey(1, i);
            if(i % 1000 == 0) matchKeys.add(Integer.toHexString(random.nextInt()));
            DependencyTracking dependencyTracking = new DependencyTracking(key, 0, 0, matchKeys);
            service.addAndBuildDependencies(dependencyTracking, null);
            if(i % 1000 == 0) System.out.println("Added " + i + " dependencies, current match keys " + matchKeys);
        });

    }

    @Before
    public void startHazelcast() {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("hz-data.xml")) {
            Config config = Hazelcast.makeConfig(is);
            config.getMapConfig("dependencies").getMapStoreConfig().setEnabled(false);
            hz = createHazelcastInstance(config);
            Hazelcast.testInstance(hz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void stopHazelcast() {
        hz.shutdown();
    }

    private Set<TrackingKey> wo(TestSet... ts) {
        return Stream.of(ts).map(t -> t.dt.getKey()).collect(Collectors.toSet());
    }
}
