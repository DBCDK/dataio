package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class JobSchedulerTransactionsBeanIT extends AbstractJobStoreIT {
    @org.junit.Test
    public void findChunksToWaitFor() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");

        DependencyTrackingService service = new DependencyTrackingService().init();

        assertThat(service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(0, 0), 0, 123456)
                        .setMatchKeys(Collections.emptySet()), null),
                is(Collections.emptySet()));

        assertThat(service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(0, 0), 0, 123456)
                        .setMatchKeys(asSet("K1")), null),
                containsInAnyOrder(new TrackingKey(1, 1)));

        assertThat(service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(0, 0), 0, 123456)
                        .setMatchKeys(asSet("C1")), null),
                containsInAnyOrder(new TrackingKey(1, 1)));

        assertThat(service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(0, 0), 0, 123456)
                        .setMatchKeys(asSet("KK2")), null),
                containsInAnyOrder(
                        new TrackingKey(1, 0),
                        new TrackingKey(1, 1),
                        new TrackingKey(1, 2),
                        new TrackingKey(1, 3)));

        assertThat(service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(0, 0), 1, 123456)
                        .setMatchKeys(asSet("K4", "K6", "C4")), null),
                containsInAnyOrder(
                        new TrackingKey(2, 0),
                        new TrackingKey(2, 2),
                        new TrackingKey(2, 4)));

        assertThat(service.findChunksToWaitFor(new DependencyTracking(new TrackingKey(0, 0), 1, 123456)
                        .setMatchKeys(asSet("K4", "K6", "C4", "K5")), null),
                containsInAnyOrder(
                        new TrackingKey(2, 1),
                        new TrackingKey(2, 0),
                        new TrackingKey(2, 2),
                        new TrackingKey(2, 4)));
    }

    @org.junit.Test
    public void boostPriorities() throws IOException {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        DependencyTrackingService service = new DependencyTrackingService().init();

        final DependencyTracking entity = new DependencyTracking(new TrackingKey(4, 2), 1, 123456);
        entity.setPriority(Priority.HIGH.getValue());
        entity.setMatchKeys(Set.of("KK2"));
        entity.setStatus(ChunkSchedulingStatus.READY_FOR_PROCESSING);

        JobSchedulerTransactionsBean bean = new JobSchedulerTransactionsBean();
        bean.dependencyTrackingService = service;

        bean.persistDependencyEntity(entity, null);

        // 4_2 is waiting for 2_0 => 2_0's default NORMAL priority is boosted to HIGH

        final DependencyTrackingRO firstLevelDependency = service.get(new TrackingKey(2, 0));
        assertThat(firstLevelDependency.getPriority(), is(Priority.HIGH.getValue()));

        // 2_1 is waiting for 3_0 => 0_0's default NORMAL priority is boosted to HIGH
        final DependencyTrackingRO secondLevelDependency = service.get(new TrackingKey(1, 0));
        assertThat(secondLevelDependency.getPriority(), is(Priority.HIGH.getValue()));
    }

    private Set<String> asSet(String... values) {
        return Set.of(values);
    }
}

