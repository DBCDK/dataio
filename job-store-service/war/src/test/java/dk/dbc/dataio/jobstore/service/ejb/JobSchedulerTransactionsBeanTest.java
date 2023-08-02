package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JobSchedulerTransactionsBeanTest {
    @Test
    public void jobPyramidTest() {
        List<Dep> list = List.of(
                new Dep(0, 0),
                new Dep(1, 0).wo(0, 0),
                new Dep(2, 0).wo(1, 0).wo(0, 0),
                new Dep(3, 0).wo(2, 0).wo(1, 0).wo(0, 0).remain()
        );
        Set<DependencyTrackingEntity.Key> result = JobSchedulerTransactionsBean.optimizeDependencies(list);
        Set<DependencyTrackingEntity.Key> expected = list.stream().filter(d -> d.mustRemain).map(DependencyTrackingEntity::getKey).collect(Collectors.toSet());
        Assert.assertEquals("All dependencies marked to remain should be in the result and nothing else", expected, result);
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
        Set<DependencyTrackingEntity.Key> result = JobSchedulerTransactionsBean.optimizeDependencies(list);
        Set<DependencyTrackingEntity.Key> expected = list.stream().filter(d -> d.mustRemain).map(DependencyTrackingEntity::getKey).collect(Collectors.toSet());
        Assert.assertEquals("All dependencies marked to remain should be in the result and nothing else", expected, result);
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
        Set<DependencyTrackingEntity.Key> result = JobSchedulerTransactionsBean.optimizeDependencies(list);
        Set<DependencyTrackingEntity.Key> expected = list.stream().filter(d -> d.mustRemain).map(DependencyTrackingEntity::getKey).collect(Collectors.toSet());
        Assert.assertEquals("All dependencies marked to remain should be in the result and nothing else", expected, result);
    }

    public static class Dep extends DependencyTrackingEntity {
        public boolean mustRemain;

        public Dep(int jobId, int chunkId) {
            setKey(new Key(jobId, chunkId));
            setWaitingOn(new HashSet<>());
        }

        public Dep wo(int jobId, int... chunkIds) {
            getWaitingOn().addAll(Arrays.stream(chunkIds).mapToObj(cid -> new DependencyTrackingEntity.Key(jobId, cid)).collect(Collectors.toSet()));
            return this;
        }

        public Dep remain() {
            mustRemain = true;
            return this;
        }
    }
}
