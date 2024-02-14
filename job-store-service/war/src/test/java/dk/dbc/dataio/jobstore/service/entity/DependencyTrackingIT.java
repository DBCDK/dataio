package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.DependencyTracking.Key;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

public class DependencyTrackingIT extends AbstractJobStoreIT {

    @AfterEach
    public void cleanupEntityManager() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    @Test
    public void createEntities() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "dependencyTracking_initialTestData.sql");

        persistenceContext.run(() -> {
            final int[] i = {0};
            Arrays.stream(DependencyTracking.ChunkSchedulingStatus.values()).forEach(chunkSchedulingStatus -> {
                DependencyTracking entity = new DependencyTracking();
                entity.setStatus(chunkSchedulingStatus);
                entity.setKey(new Key(1, i[0]));
                entity.setMatchKeys(Stream.of(
                                "5 023 297 2",
                                "2 004 091 2",
                                "4 016 438 3",
                                "0 198 393 8",
                                "2 022 704 4",
                                "2 017 916 3",
                                "5 000 116 4",
                                "5 017 224 4",
                                "2 002 537 9",
                                "5 005 396 2",
                                "4 107 001 3",
                                "2 017 919 8",
                                "0 193 840 1",
                                "0 189 413 7",
                                "2 015 874 3",
                                "5 017 504 9",
                                "0 189 446 3",
                                "2 015 875 1",
                                "5 044 974 2",
                                "5 007 721 7",
                                "f" + i[0])
                        .collect(Collectors.toSet()));
                entity.setWaitingOn(Stream.of(
                                new Key(1, 1),
                                new Key(3, 0))
                        .collect(Collectors.toSet()));
                entity.setPriority(Priority.NORMAL.getValue());

                entityManager.persist(entity);
                ++i[0];
            });
        });

        JPATestUtils.clearEntityManagerCache(entityManager);

        final DependencyTracking entity = entityManager.find(DependencyTracking.class, new DependencyTracking.Key(1, 0));
        assertThat("status", entity.getStatus(), is(DependencyTracking.ChunkSchedulingStatus.READY_FOR_PROCESSING));
        assertThat("sink ID", entity.getSinkid(), is(0));
        assertThat("match keys", entity.getMatchKeys(), containsInAnyOrder("5 023 297 2", "2 004 091 2", "4 016 438 3", "0 198 393 8", "2 022 704 4", "2 017 916 3", "5 000 116 4", "5 017 224 4", "2 002 537 9", "5 005 396 2", "4 107 001 3", "2 017 919 8", "0 193 840 1", "0 189 413 7", "2 015 874 3", "5 017 504 9", "0 189 446 3", "2 015 875 1", "5 044 974 2", "5 007 721 7", "f0"));
        assertThat("waitingOn", entity.getWaitingOn(), containsInAnyOrder(new Key(1, 1), new Key(3, 0)));
        assertThat("priority", entity.getPriority(), is(Priority.NORMAL.getValue()));
    }

    @Test
    public void querySinkIdStatusCount() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "dependencyTracking_sinkStatusLoadTest.sql");

        final List<SinkIdStatusCountResult> result = persistenceContext.run(() -> entityManager
                .createNamedQuery(DependencyTracking.SINKID_STATUS_COUNT_QUERY_ALL, SinkIdStatusCountResult.class)
                .getResultList());

        assertThat("result size", result.size(), is(10));
        assertThat("result[0]", result.get(0), is(new SinkIdStatusCountResult(1, DependencyTracking.ChunkSchedulingStatus.READY_FOR_PROCESSING, 5)));
        assertThat("result[1]", result.get(1), is(new SinkIdStatusCountResult(1, DependencyTracking.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING, 4)));
        assertThat("result[2]", result.get(2), is(new SinkIdStatusCountResult(1, DependencyTracking.ChunkSchedulingStatus.BLOCKED, 2)));
        assertThat("result[3]", result.get(3), is(new SinkIdStatusCountResult(1, DependencyTracking.ChunkSchedulingStatus.READY_FOR_DELIVERY, 3)));
        assertThat("result[4]", result.get(4), is(new SinkIdStatusCountResult(1, DependencyTracking.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY, 1)));

        assertThat("result[5]", result.get(5), is(new SinkIdStatusCountResult(1551, DependencyTracking.ChunkSchedulingStatus.READY_FOR_PROCESSING, 1)));
        assertThat("result[6]", result.get(6), is(new SinkIdStatusCountResult(1551, DependencyTracking.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING, 2)));
        assertThat("result[7]", result.get(7), is(new SinkIdStatusCountResult(1551, DependencyTracking.ChunkSchedulingStatus.BLOCKED, 3)));
        assertThat("result[8]", result.get(8), is(new SinkIdStatusCountResult(1551, DependencyTracking.ChunkSchedulingStatus.READY_FOR_DELIVERY, 4)));
        assertThat("result[9]", result.get(9), is(new SinkIdStatusCountResult(1551, DependencyTracking.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY, 5)));
    }

    @Test
    public void queryJobCountChunkCount() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "dependencyTracking_sinkStatusLoadTest.sql");

        final Object[] result = persistenceContext.run(() -> (Object[]) entityManager
                .createNamedQuery(DependencyTracking.JOB_COUNT_CHUNK_COUNT_QUERY).setParameter(1, 1)
                .getSingleResult());

        assertThat("number of jobs", result[0], is(1L));
        assertThat("number of chunks", result[1], is(15L));
    }

    @Test
    public void queryBySinkIdAndState() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "dependencyTracking_initialTestData.sql");

        final List<DependencyTracking> entities = persistenceContext.run(() -> entityManager
                .createNamedQuery(DependencyTracking.BY_SINKID_AND_STATE_QUERY, DependencyTracking.class)
                .setParameter("sinkId", 4242)
                .setParameter("state", DependencyTracking.ChunkSchedulingStatus.READY_FOR_PROCESSING)
                .getResultList());

        assertThat("number of entities", entities.size(), is(4));
        assertThat("1st entity", entities.get(0).getKey(), is(new DependencyTracking.Key(6, 21)));
        assertThat("2nd entity", entities.get(1).getKey(), is(new DependencyTracking.Key(1, 20)));
        assertThat("3rd entity", entities.get(2).getKey(), is(new DependencyTracking.Key(1, 21)));
        assertThat("4th entity", entities.get(3).getKey(), is(new DependencyTracking.Key(6, 20)));
    }
}
