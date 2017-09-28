package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class JobSchedulerTransactionsBeanIT extends AbstractJobStoreIT {
    @Test
    public void findChunksToWaitFor() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        final JobSchedulerTransactionsBean bean = new JobSchedulerTransactionsBean();
        bean.entityManager = entityManager;

        assertThat(bean.findChunksToWaitFor(new DependencyTrackingEntity()
                        .setSinkid(0)
                        .setMatchKeys(Collections.emptySet()), null),
                is(Collections.emptySet()));

        assertThat(bean.findChunksToWaitFor(new DependencyTrackingEntity()
                        .setSinkid(0)
                        .setMatchKeys(asSet("K1")), null),
                containsInAnyOrder(new Key(1,1)));

        assertThat(bean.findChunksToWaitFor(new DependencyTrackingEntity()
                        .setSinkid(0)
                        .setMatchKeys(asSet("C1")), null),
                containsInAnyOrder(new Key(1,1)));

        assertThat(bean.findChunksToWaitFor(new DependencyTrackingEntity()
                        .setSinkid(0)
                        .setMatchKeys(asSet("KK2")), null),
                containsInAnyOrder(
                        new Key(1,0),
                        new Key(1,1),
                        new Key(1,2),
                        new Key(1,3)));

        assertThat(bean.findChunksToWaitFor(new DependencyTrackingEntity()
                        .setSinkid(1)
                        .setMatchKeys(asSet("K4", "K6", "C4")), null),
                containsInAnyOrder(
                        new Key(2,0),
                        new Key(2,2),
                        new Key(2,4)));

        assertThat(bean.findChunksToWaitFor(new DependencyTrackingEntity()
                        .setSinkid(1)
                        .setMatchKeys(asSet("K4", "K6", "C4", "K5")), null),
                containsInAnyOrder(
                        new Key(2,1),
                        new Key(2,0),
                        new Key(2,2),
                        new Key(2,4)));
    }

    @Test
    public void boostPriorities() throws IOException, URISyntaxException {
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        final DependencyTrackingEntity entity = new DependencyTrackingEntity();
        entity.setKey(new Key(4, 2));
        entity.setPriority(Priority.HIGH.getValue());
        entity.setMatchKeys(Stream.of("4_1", "4_2").collect(Collectors.toSet()));
        entity.setSinkid(1);
        entity.setStatus(DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_PROCESSING);

        final JobSchedulerTransactionsBean bean = new JobSchedulerTransactionsBean();
        bean.entityManager = entityManager;

        persistenceContext.run(() -> bean.persistDependencyEntity(entity, null));

        // 4_2 is waiting for 4_1 => 4_1's default NORMAL priority is boosted to HIGH
        final DependencyTrackingEntity firstLevelDependency = entityManager.find(DependencyTrackingEntity.class, new Key(4, 1));
        assertThat(firstLevelDependency.getPriority(), is(Priority.HIGH.getValue()));

        // 4_1 is waiting for 4_0 => 4_0's default NORMAL priority is boosted to HIGH
        final DependencyTrackingEntity secondLevelDependency = entityManager.find(DependencyTrackingEntity.class, new Key(4, 0));
        assertThat(secondLevelDependency.getPriority(), is(Priority.HIGH.getValue()));
    }

    private Set<String> asSet(String... values) {
        return Arrays.stream(values).collect(Collectors.toSet());
    }
}

