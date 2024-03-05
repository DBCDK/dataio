package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JobSchedulerTransactionsBeanTest {
    @Test
    public void jobPyramidTest() {
        List<Dep> list = List.of(
                new Dep(0, 0),
                new Dep(1, 0).wo(0, 0),
                new Dep(2, 0).wo(1, 0).wo(0, 0),
                new Dep(3, 0).wo(2, 0).wo(1, 0).wo(0, 0).remain()
        );
        Set<TrackingKey> result = DependencyTrackingService.optimizeDependencies(list);
        Set<TrackingKey> expected = list.stream().filter(d -> d.mustRemain).map(DependencyTracking::getKey).collect(Collectors.toSet());
        assertEquals(expected, result, "All dependencies marked to remain should be in the result and nothing else");
    }

    @Test
    public void cleanTreeTest() {
        List<Dep> list = List.of(
                new Dep(0, 0),
                new Dep(0, 1).wo(0, 0),
                new Dep(0, 2).wo(0, 1).remain(),
                new Dep(0, 3).wo(0, 0),
                new Dep(0, 4).wo(0, 3).remain(),
                new Dep(1, 0).wo(0, 0),
                new Dep(0, 5).wo(1, 0),
                new Dep(0, 6).wo(0, 5).remain(),
                new Dep(2, 0).remain()
        );
        Set<TrackingKey> result = DependencyTrackingService.optimizeDependencies(list);
        Set<TrackingKey> expected = list.stream().filter(d -> d.mustRemain).map(DependencyTracking::getKey).collect(Collectors.toSet());
        assertEquals(expected, result, "All dependencies marked to remain should be in the result and nothing else");
    }

    @Test
    public void overpopulatedTreeTest() {
        List<Dep> list = List.of(
                new Dep(0, 0),
                new Dep(0, 1).wo(0, 0),
                new Dep(0, 2).wo(0, 0, 1).remain(),
                new Dep(0, 3).wo(0, 0),
                new Dep(0, 4).wo(0, 0, 3).remain(),
                new Dep(1, 0).wo(0, 0),
                new Dep(0, 5).wo(0, 0).wo(1, 0),
                new Dep(0, 6).wo(0, 0, 5).wo(1, 0).remain(),
                new Dep(2, 0).remain()
        );
        Set<TrackingKey> result = DependencyTrackingService.optimizeDependencies(list);
        Set<TrackingKey> expected = list.stream().filter(d -> d.mustRemain).map(DependencyTracking::getKey).collect(Collectors.toSet());
        assertEquals(expected, result, "All dependencies marked to remain should be in the result and nothing else");
    }

    public static class Dep extends DependencyTracking {
        public boolean mustRemain;

        public Dep(int jobId, int chunkId) {
            super(new TrackingKey(jobId, chunkId), 0);
            setWaitingOn(new HashSet<>());
        }

        public Dep wo(int jobId, int... chunkIds) {
            getWaitingOn().addAll(Arrays.stream(chunkIds).mapToObj(cid -> new TrackingKey(jobId, cid)).collect(Collectors.toSet()));
            return this;
        }

        public Dep remain() {
            mustRemain = true;
            return this;
        }
    }
}
