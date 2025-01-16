package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.JetTestSupport;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.WaitFor;
import dk.dbc.dataio.jobstore.distributed.hz.store.DependencyTrackingStore;
import dk.dbc.dataio.jobstore.service.ejb.DatabaseMigrator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

public class DependencyTrackingServiceTest extends JetTestSupport implements PostgresContainerJPAUtils {
    private HazelcastInstance hz;
    private static final DataSource datasource = dbContainer.bindDatasource(DependencyTrackingStore.DS_JNDI).datasource();


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

    @org.junit.Test
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
        Set<WaitFor> LastTrackerKeySet = service.getLastTrackerMapSnapshot().keySet();
        Assert.assertTrue("When T2_5 is removed the tracker map should not contain entries for sink/submitter 0",
                LastTrackerKeySet.stream().noneMatch(wf -> wf.sinkId() == 0 && wf.submitter() == 0));
    }

    @org.junit.Test
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

    @org.junit.Test
    public void testBarrierKeyFail() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        List<TestSet> trackers = makeTestSet();
        trackers.forEach(tracker -> service.addAndBuildDependencies(tracker.dt, "hest"));
        Assert.assertNotEquals(T1_3.expectedWo, service.get(T1_3.dt.getKey()).getWaitingOn());
    }

    @org.junit.Test
    public void testTrackerRebuild() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        List<TestSet> trackers = makeTestSet();
        trackers.forEach(tracker -> service.addAndBuildDependencies(tracker.dt, null));
        Map<WaitFor, TrackingKey> trackerMap = service.rebuildTrackerMap();
        Assert.assertEquals("A rebuild tracker map, must be identical to one that has developed over time", service.getLastTrackerMapSnapshot(), trackerMap);
    }

    @org.junit.Test
    public void testNovemberCrash() {
        DependencyTrackingService service = new DependencyTrackingService().init(true);
        ObjectMapper mapper = new ObjectMapper();
        try(InputStream is = ResourceReader.getResourceAsStream(getClass(), "dependencytracking_20241125.json")) {
            List<Map> list = mapper.readValue(is, List.class);
            List<DependencyTracking> trackings = list.stream().map(this::createTracker).toList();
            trackings.forEach(dt -> service.addAndBuildDependencies(dt, "test"));
            trackings.forEach(dt -> service.setStatus(dt.getKey(), ChunkSchedulingStatus.READY_FOR_DELIVERY));
            Set<Integer> sinks = service.getActiveSinks(ChunkSchedulingStatus.READY_FOR_DELIVERY);
            while(!sinks.isEmpty()) {
                for (Integer sink : sinks) {
                    Set<TrackingKey> keys = service.find(ChunkSchedulingStatus.READY_FOR_DELIVERY, sink, Integer.MAX_VALUE);
                    keys.forEach(k -> {
                        service.remove(k);
                        service.removeFromWaitingOn(k);
                    });
                    Assert.assertFalse("Keys removed must not be empty", keys.isEmpty());
                }
                sinks = service.getActiveSinks(ChunkSchedulingStatus.READY_FOR_DELIVERY);
            }
            Assert.assertTrue("All tracker should be resolved and removed", service.getLastTrackerMapSnapshot().isEmpty());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DependencyTracking createTracker(Map e) {
        TrackingKey key = new TrackingKey((int) e.get("jobid"), (int) e.get("chunkid"));
        Set<String> matchKeys = new HashSet<>((List)e.get("matchkeys"));
        DependencyTracking dt = new DependencyTracking(key, (int) e.get("sinkid"), (int) e.get("submitter"), matchKeys);
        return dt;
    }

    @org.junit.Test @Ignore("Performant test to be run explicitly, and not as part of the build test set")
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

    @BeforeClass
    public static void createDb() {
        DatabaseMigrator databaseMigrator = new DatabaseMigrator().withDataSource(datasource).onStartup();
        try(Connection c = datasource.getConnection(); Statement s = c.createStatement()) {
            // Drop foreign key to chunks so we can create dependencies undisturbed
            s.execute("alter table dependencytracking drop constraint dependencytracking_jobid_fkey");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void enableFK() {
        try(Connection c = datasource.getConnection(); Statement s = c.createStatement()) {
            s.execute("truncate table dependencytracking");
            // Re-add foreign key to chunks
            s.execute("ALTER TABLE ONLY dependencytracking ADD CONSTRAINT dependencytracking_jobid_fkey FOREIGN KEY (jobid, chunkid) REFERENCES chunk(jobid, id) ON DELETE CASCADE;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void startHazelcast() {
        try(Connection c = datasource.getConnection(); Statement s = c.createStatement()) {
            s.execute("truncate table dependencytracking");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("hz-data.xml")) {
            Config config = Hazelcast.makeConfig(is);
            config.getMapConfig("dependencies");
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
