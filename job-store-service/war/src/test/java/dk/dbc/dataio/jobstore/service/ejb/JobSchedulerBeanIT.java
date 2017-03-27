package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.LockModeType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ja7 on 11-04-16.
 *
 * Dependency Tracker for chunks
 *
 * En chunk få gennem følgende trin.
     1 ReadyToProcess ( marker chunk er petitioneret og analyseret
            -> Async SubmitIfPosibleForProcessing( sink, chunkDescription )
     2. QueuedToProcess ( markere den er send til Processing JMS kø )
           ->  AddChunkProcessed(... )
                     ASync SubmitIfPosibleForProcessing( sink, null )
                     ASync SubmitIfPoribleForDelevering( sink, shunk )
     3a. ReadyDelevering  ( marker Chunk er klar til Sink )
     3b. Blocked  ( Chunk Venter på Chunk bliver klar fra sink )
     4. QueuedToSink ( marker chunk er send til Sink )
            -> AddChunkDelvering( ... )
                   removes Waits for from All that waits for this chunk.
                   Change state for chunks with no waits for

 *
 *
 */
public class JobSchedulerBeanIT extends AbstractJobStoreIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBeanIT.class);

    @Before
    public void setUp() throws Exception {
        //entityManager = JPATestUtils.createEntityManagerForIntegrationTest("jobstoreIT");


    }


    @Test
    public void findChunksWaitingForMe() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        JobSchedulerBean bean= new JobSchedulerBean();
        bean.entityManager=entityManager;

        assertThat(bean.findChunksWaitingForMe( new Key(1,1)), containsInAnyOrder( new Key(1,1)));
        assertThat(bean.findChunksWaitingForMe( new Key(0,1)), containsInAnyOrder( new Key(1,1), new Key(2,1)));

        List<Key> res=bean.findChunksWaitingForMe( new Key(3,0) );
        assertThat(res, containsInAnyOrder( new Key(1,0), new Key(1,1), new Key(1,2), new Key(1,3), new Key(2,0), new Key(2,1), new Key(2,2), new Key(2,3), new Key(2,4)));
    }

    @Test
    public void multipleCallesToChunkXxxxxxDoneIsIgnored() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, JobSchedulerBeanIT.class, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        entityManager.getTransaction().begin();
        entityManager.createNativeQuery("DELETE FROM dependencytracking").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 1, 1, 1, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 2, 1, 2, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 3, 1, 3, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 4, 1, 4, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 5, 1, 5, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
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
        List<DependencyTrackingEntity> res = entityManager.createNativeQuery("SELECT * FROM dependencytracking WHERE jobid=3 AND chunkId != status ").getResultList();
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
        //JPATestUtils.runSqlFromResource(entityManager, JobSchedulerBeanIT.class, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = entityManager;
        JobSchedulerTransactionsBean jtbean= new JobSchedulerTransactionsBean();
        bean.pgJobStoreRepository = newPgJobStoreRepository();
        jtbean.entityManager = bean.entityManager;
        jtbean.sinkMessageProducerBean = mock(SinkMessageProducerBean.class);
        jtbean.jobStoreRepository = bean.pgJobStoreRepository;
        bean.jobSchedulerTransactionsBean = jtbean;



        Sink sink1=new SinkBuilder().setId(1).setContent(
                new SinkContentBuilder().setSinkType( SinkContent.SinkType.TICKLE ).build()
        ).build();

        entityManager.getTransaction().begin();
        for (int chunkId : new int[]{0, 1, 2, 3, 4}) {
            String ck=String.format("CK%d",chunkId);
            String previousCk=String.format("CK%d", chunkId - 1);

            bean.scheduleChunk(new ChunkEntity()
                            .withJobId(3)
                            .withChunkId( chunkId ).withNumberOfItems((short) 1)
                            .withSequenceAnalysisData(makeSequenceAnalyceData(ck, previousCk))
                    , sink1
                    , 1
            );
        }
        bean.markJobPartitioned(3, sink1, 5, 1, ChunkItem.Status.SUCCESS);
        entityManager.getTransaction().commit();

        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,0).getMatchKeys(), containsInAnyOrder("CK-1", "CK0", "1"));
        assertThat("check Ekstra key for chunk1", getDependencyTrackingEntity(3,1).getMatchKeys(), containsInAnyOrder("CK0", "CK1" ));
        assertThat("check Ekstra key for chunk2", getDependencyTrackingEntity(3,2).getMatchKeys(), containsInAnyOrder("CK1", "CK2" ));
        assertThat("check Ekstra key for chunk3", getDependencyTrackingEntity(3,3).getMatchKeys(), containsInAnyOrder("CK2", "CK3"));
        assertThat("check Ekstra key for chunk5", getDependencyTrackingEntity(3,5).getMatchKeys(), containsInAnyOrder("1"));

        assertThat("check getWaitingOn key for chunk1", getDependencyTrackingEntity(3,0).getWaitingOn().size(), is(0));
        assertThat("check getWaitingOn key for chunk2", getDependencyTrackingEntity(3,1).getWaitingOn(), containsInAnyOrder( mk(3,0) ));
        assertThat("check getWaitingOn key for chunk3", getDependencyTrackingEntity(3,2).getWaitingOn(), containsInAnyOrder( mk(3,0), mk(3,1) ));
        assertThat("check getWaitingOn key for chunk4", getDependencyTrackingEntity(3,3).getWaitingOn(), containsInAnyOrder( mk(3,0), mk(3,2)));
        assertThat("check getWaitingOn key for chunk5", getDependencyTrackingEntity(3,5).getWaitingOn(), containsInAnyOrder( mk(3,0), mk(3,1),mk(3,2), mk(3,3), mk(3,4)));

    }


    @Test
    public void NonTickleChunkDependency() throws Exception {
        JPATestUtils.runSqlFromResource(entityManager, JobSchedulerBeanIT.class, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = entityManager;
        JobSchedulerTransactionsBean jtbean= new JobSchedulerTransactionsBean();
        jtbean.entityManager = bean.entityManager;
        jtbean.sinkMessageProducerBean = mock(SinkMessageProducerBean.class);
        bean.jobSchedulerTransactionsBean = jtbean;



        Sink sink1=new SinkBuilder().setId(1).setContent(
                new SinkContentBuilder().setSinkType( SinkContent.SinkType.DUMMY ).build()
        ).build();

        entityManager.getTransaction().begin();
        for (int chunkId : new int[]{0, 1, 2, 3}) {
            String ck=String.format("CK%d",chunkId);
            String previousCk=String.format("CK%d", chunkId - 1);

            bean.scheduleChunk(new ChunkEntity()
                            .withJobId(3)
                            .withChunkId( chunkId ).withNumberOfItems((short) 1)
                            .withSequenceAnalysisData( makeSequenceAnalyceData(ck, previousCk))
                    , sink1
                    , 1
            );
        }
        bean.markJobPartitioned(3, sink1, 4, 1, ChunkItem.Status.SUCCESS);
        entityManager.getTransaction().commit();


        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,0).getMatchKeys(), containsInAnyOrder("CK-1", "CK0"));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,1).getMatchKeys(), containsInAnyOrder("CK0", "CK1"));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,2).getMatchKeys(), containsInAnyOrder("CK1", "CK2"));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,3).getMatchKeys(), containsInAnyOrder("CK2", "CK3"));

        assertThat("check getWaitingOn key for chunk0", getDependencyTrackingEntity(3,0).getWaitingOn().size(), is(0));
        assertThat("check getWaitingOn key for chunk0", getDependencyTrackingEntity(3,1).getWaitingOn(), containsInAnyOrder( mk(3,0) ));
        assertThat("check getWaitingOn key for chunk0", getDependencyTrackingEntity(3,2).getWaitingOn(), containsInAnyOrder( mk(3,1) ));
        assertThat("check getWaitingOn key for chunk0", getDependencyTrackingEntity(3,3).getWaitingOn(), containsInAnyOrder( mk(3,2)));
        entityManager.getTransaction().begin();
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(3, 4), LockModeType.PESSIMISTIC_READ);
        assertThat(dependencyTrackingEntity, is(nullValue()));
        entityManager.getTransaction().commit();
    }



    private Key mk(int jobId, int chunkId ) {
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
    private DependencyTrackingEntity getDependencyTrackingEntity(int jobId, int chunkId) {
        JPATestUtils.clearEntityManagerCache(entityManager);
        entityManager.getTransaction().begin();

        LOGGER.info("Test Checker entityManager.find( job={}, chunk={} ) ", jobId, chunkId );
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(jobId, chunkId), LockModeType.PESSIMISTIC_READ);
        assertThat(dependencyTrackingEntity, is(notNullValue()));
        entityManager.refresh(dependencyTrackingEntity);
        entityManager.getTransaction().rollback();
        return dependencyTrackingEntity;
    }


}