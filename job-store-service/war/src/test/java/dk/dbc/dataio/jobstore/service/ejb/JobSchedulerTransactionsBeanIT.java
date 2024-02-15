package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTracking;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class JobSchedulerTransactionsBeanIT extends AbstractJobStoreIT {
    @Test
    public void findChunksToWaitFor() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        final JobSchedulerTransactionsBean bean = new JobSchedulerTransactionsBean();
        bean.entityManager = entityManager;
        bean.enableOptimizer =false;

        assertThat(bean.findChunksToWaitFor(new DependencyTracking()
                        .setSubmitterNumber(123456)
                        .setSinkId(0)
                        .setMatchKeys(Collections.emptySet()), null),
                is(Collections.emptySet()));

        assertThat(bean.findChunksToWaitFor(new DependencyTracking()
                        .setSubmitterNumber(123456)
                        .setSinkId(0)
                        .setMatchKeys(asSet("K1")), null),
                containsInAnyOrder(new TrackingKey(1, 1)));

        assertThat(bean.findChunksToWaitFor(new DependencyTracking()
                        .setSubmitterNumber(123456)
                        .setSinkId(0)
                        .setMatchKeys(asSet("C1")), null),
                containsInAnyOrder(new TrackingKey(1, 1)));

        assertThat(bean.findChunksToWaitFor(new DependencyTracking()
                        .setSubmitterNumber(123456)
                        .setSinkId(0)
                        .setMatchKeys(asSet("KK2")), null),
                containsInAnyOrder(
                        new TrackingKey(1, 0),
                        new TrackingKey(1, 1),
                        new TrackingKey(1, 2),
                        new TrackingKey(1, 3)));

        assertThat(bean.findChunksToWaitFor(new DependencyTracking()
                        .setSubmitterNumber(123456)
                        .setSinkId(1)
                        .setMatchKeys(asSet("K4", "K6", "C4")), null),
                containsInAnyOrder(
                        new TrackingKey(2, 0),
                        new TrackingKey(2, 2),
                        new TrackingKey(2, 4)));

        assertThat(bean.findChunksToWaitFor(new DependencyTracking()
                        .setSubmitterNumber(123456)
                        .setSinkId(1)
                        .setMatchKeys(asSet("K4", "K6", "C4", "K5")), null),
                containsInAnyOrder(
                        new TrackingKey(2, 1),
                        new TrackingKey(2, 0),
                        new TrackingKey(2, 2),
                        new TrackingKey(2, 4)));
    }

    @Test
    public void boostPriorities() throws IOException, URISyntaxException {
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        final DependencyTracking entity = new DependencyTracking();
        entity.setKey(new TrackingKey(4, 2));
        entity.setPriority(Priority.HIGH.getValue());
        entity.setMatchKeys(Stream.of("4_1", "4_2").collect(Collectors.toSet()));
        entity.setSinkId(1);
        entity.setStatus(ChunkSchedulingStatus.READY_FOR_PROCESSING);

        final JobSchedulerTransactionsBean bean = new JobSchedulerTransactionsBean();
        bean.entityManager = entityManager;
        bean.enableOptimizer =false;

        persistenceContext.run(() -> bean.persistDependencyEntity(entity, null));

        // 4_2 is waiting for 4_1 => 4_1's default NORMAL priority is boosted to HIGH
        final DependencyTracking firstLevelDependency = entityManager.find(DependencyTracking.class, new TrackingKey(4, 1));
        assertThat(firstLevelDependency.getPriority(), is(Priority.HIGH.getValue()));

        // 4_1 is waiting for 4_0 => 4_0's default NORMAL priority is boosted to HIGH
        final DependencyTracking secondLevelDependency = entityManager.find(DependencyTracking.class, new TrackingKey(4, 0));
        assertThat(secondLevelDependency.getPriority(), is(Priority.HIGH.getValue()));
    }

    private Set<String> asSet(String... values) {
        return Arrays.stream(values).collect(Collectors.toSet());
    }
}

