package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTracking;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.persistence.LockModeType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTracking.Key;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Chunk states
 * 1.  READY_FOR_PROCESSING  ( marks chunk as partitioned and analyzed
 * 2.  QUEUED_FOR_PROCESSING ( marks chunk as sent to processing JMS queue )
 * 3a. READY_FOR_DELIVERY    ( marks chunk as ready for sink delivery )
 * 3b. BLOCKED               ( marks chunk as waiting for delivery of one or more other chunks )
 * 4.  QUEUED_FOR_DELIVERY   ( marks chunk as sent to sink JMS queue )
 */
public class JobSchedulerBeanIT extends AbstractJobStoreIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBeanIT.class);

    @Test
    public void findChunksWaitingForMe() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = entityManager;

        List<Key> res = bean.findChunksWaitingForMe(new Key(3, 0), 1);
        assertThat(res, containsInAnyOrder(new Key(2, 0), new Key(2, 1), new Key(2, 2), new Key(2, 3), new Key(2, 4)));
    }

    @Test
    public void multipleCallesToChunkXxxxxxDoneIsIgnored() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        entityManager.getTransaction().begin();
        entityManager.createNativeQuery("DELETE FROM dependencytracking").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (3, 1, 1, 1, '[{\"jobId\": 3, \"chunkId\": 0}]', '[\"K8\", \"KK2\", \"C4\"]', '{}')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (3, 2, 1, 2, '[{\"jobId\": 3, \"chunkId\": 0}]', '[\"K8\", \"KK2\", \"C4\"]', '{}')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (3, 3, 1, 3, '[{\"jobId\": 3, \"chunkId\": 0}]', '[\"K8\", \"KK2\", \"C4\"]', '{}')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (3, 4, 1, 4, '[{\"jobId\": 3, \"chunkId\": 0}]', '[\"K8\", \"KK2\", \"C4\"]', '{}')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (3, 5, 1, 5, '[{\"jobId\": 3, \"chunkId\": 0}]', '[\"K8\", \"KK2\", \"C4\"]', '{}')").executeUpdate();
        entityManager.getTransaction().commit();


        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = entityManager;

        entityManager.getTransaction().begin();
        for (int chunkid : new int[]{1, 3, 4, 5}) {
            bean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkid)
                    .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                    .build()
            );
        }
        entityManager.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(entityManager);

        // check no statuses is modified
        List<DependencyTracking> res = entityManager.createNativeQuery("SELECT * FROM dependencytracking WHERE jobid=3 AND chunkId != status ").getResultList();
        assertThat("Test chunkProcessingDone did not change any chunk ", res.size(), is(0));


        entityManager.getTransaction().begin();
        for (int chunkid : new int[]{1, 3, 4, 5}) {
            bean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkid)
                    .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                    .build()
            );
        }
        entityManager.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(entityManager);


        entityManager.getTransaction().begin();
        for (int chunkid : new int[]{1, 2, 3, 4, 6}) {
            bean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkid)
                    .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                    .build()
            );
        }
        entityManager.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(entityManager);
    }

    @Test
    public void TickleChunkDependency() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this,
                "JobSchedulerBeanIT_findWaitForChunks.sql");

        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = entityManager;
        JobSchedulerTransactionsBean jtbean = new JobSchedulerTransactionsBean();
        bean.pgJobStoreRepository = newPgJobStoreRepository();
        jtbean.entityManager = bean.entityManager;
        jtbean.enableOptimizer = false;
        jtbean.sinkMessageProducerBean = mock(SinkMessageProducerBean.class);
        jtbean.jobStoreRepository = bean.pgJobStoreRepository;
        bean.jobSchedulerTransactionsBean = jtbean;

        final JobEntity jobEntity = new JobEntity(3);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setSpecification(new JobSpecification()
                .withSubmitterId(1));
        jobEntity.setState(new State());
        jobEntity.setCachedSink(SinkCacheEntity.create(new SinkBuilder()
                .setId(1)
                .setContent(new SinkContentBuilder()
                        .setSinkType(SinkContent.SinkType.TICKLE)
                        .build())
                .build()));

        entityManager.getTransaction().begin();
        for (int chunkId : new int[]{0, 1, 2, 3, 4}) {
            final ChunkEntity chunkEntity = new ChunkEntity()
                    .withJobId(3)
                    .withChunkId(chunkId)
                    .withNumberOfItems((short) 1)
                    .withSequenceAnalysisData(makeSequenceAnalyceData(
                            String.format("CK%d", chunkId),
                            String.format("CK%d", chunkId - 1)));

            bean.scheduleChunk(chunkEntity, jobEntity);
        }
        bean.createAndScheduleTerminationChunk(jobEntity, jobEntity.getCachedSink().getSink(),
                5, "1", ChunkItem.Status.SUCCESS);
        entityManager.getTransaction().commit();

        assertThat("check match key for chunk0",
                getDependencyTrackingEntity(3, 0).getMatchKeys(),
                containsInAnyOrder("CK-1", "CK0", "1"));
        assertThat("check barrier match key for chunk1",
                getDependencyTrackingEntity(3, 1).getMatchKeys(),
                containsInAnyOrder("CK0", "CK1"));
        assertThat("check barrier match key for chunk2",
                getDependencyTrackingEntity(3, 2).getMatchKeys(),
                containsInAnyOrder("CK1", "CK2"));
        assertThat("check barrier match key for chunk3",
                getDependencyTrackingEntity(3, 3).getMatchKeys(),
                containsInAnyOrder("CK2", "CK3"));
        assertThat("check barrier match key for chunk5",
                getDependencyTrackingEntity(3, 5).getMatchKeys(),
                containsInAnyOrder("1"));

        assertThat("check waitingOn for chunk1",
                getDependencyTrackingEntity(3, 0).getWaitingOn().size(), is(0));
        assertThat("check waitingOn for chunk2",
                getDependencyTrackingEntity(3, 1).getWaitingOn(), containsInAnyOrder(
                        mk(3, 0)));
        assertThat("check waitingOn for chunk3",
                getDependencyTrackingEntity(3, 2).getWaitingOn(), containsInAnyOrder(
                        mk(3, 0),
                        mk(3, 1)));
        assertThat("check waitingOn for chunk4",
                getDependencyTrackingEntity(3, 3).getWaitingOn(), containsInAnyOrder(
                        mk(3, 0),
                        mk(3, 2)));
        assertThat("check waitingOn for chunk5",
                getDependencyTrackingEntity(3, 5).getWaitingOn(), containsInAnyOrder(
                        mk(3, 0),
                        mk(3, 1),
                        mk(3, 2),
                        mk(3, 3),
                        mk(3, 4)));
    }

    @Test
    public void isScheduled() {
        JPATestUtils.runSqlFromResource(entityManager, this,
                "JobSchedulerBeanIT_findWaitForChunks.sql");

        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.entityManager = entityManager;

        final ChunkEntity notScheduled = new ChunkEntity();
        notScheduled.setKey(new ChunkEntity.Key(42, 42));
        assertThat("not scheduled", jobSchedulerBean.isScheduled(notScheduled), is(false));

        final ChunkEntity scheduled = new ChunkEntity();
        notScheduled.setKey(new ChunkEntity.Key(1, 1));
        assertThat("scheduled", jobSchedulerBean.isScheduled(scheduled), is(false));
    }

    @Test
    public void ensureLastChunkIsScheduled_alreadyScheduled() {
        final JobEntity jobEntity = newPersistedJobEntity();
        jobEntity.setNumberOfChunks(43);
        newPersistedChunkEntity(new ChunkEntity.Key(42, jobEntity.getId()));
        newPersistedDependencyTrackingEntity(new DependencyTracking.Key(jobEntity.getId(), 42));

        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.entityManager = entityManager;

        // No key violation, so the isScheduled call must have returned true...
        jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId());
    }

    @Test
    public void ensureLastChunkIsScheduled_notAlreadyScheduled() {
        final SinkCacheEntity sinkCacheEntity = newPersistedSinkCacheEntity();

        final JobEntity jobEntity = newJobEntity();
        jobEntity.setNumberOfChunks(43);
        jobEntity.setCachedSink(sinkCacheEntity);
        jobEntity.setPriority(Priority.HIGH);
        persist(jobEntity);

        final ChunkEntity chunkEntity =
                newPersistedChunkEntity(new ChunkEntity.Key(42, jobEntity.getId()));

        final JobSchedulerTransactionsBean jobSchedulerTransactionsBean
                = mock(JobSchedulerTransactionsBean.class);

        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.entityManager = entityManager;
        jobSchedulerBean.jobSchedulerTransactionsBean = jobSchedulerTransactionsBean;

        jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId());

        verify(jobSchedulerTransactionsBean).persistDependencyEntity(
                any(DependencyTracking.class), nullable(String.class));

        verify(jobSchedulerTransactionsBean).submitToProcessingIfPossibleAsync(
                chunkEntity, sinkCacheEntity.getSink().getId(), jobEntity.getPriority().getValue());
    }

    private Key mk(int jobId, int chunkId) {
        return new Key(jobId, chunkId);
    }

    private SequenceAnalysisData makeSequenceAnalyceData(String... s) {
        return new SequenceAnalysisData(makeSet(s));
    }

    private Set<String> makeSet(String... s) {
        Set<String> res = new HashSet<>();
        Collections.addAll(res, s);
        return res;
    }

    @SuppressWarnings("SameParameterValue")
    private DependencyTracking getDependencyTrackingEntity(int jobId, int chunkId) {
        JPATestUtils.clearEntityManagerCache(entityManager);
        entityManager.getTransaction().begin();

        LOGGER.info("Test Checker entityManager.find( job={}, chunk={} ) ", jobId, chunkId);
        DependencyTracking dependencyTracking = entityManager.find(DependencyTracking.class, new DependencyTracking.Key(jobId, chunkId), LockModeType.PESSIMISTIC_READ);
        assertThat(dependencyTracking, is(notNullValue()));
        entityManager.refresh(dependencyTracking);
        entityManager.getTransaction().rollback();
        return dependencyTracking;
    }
}
