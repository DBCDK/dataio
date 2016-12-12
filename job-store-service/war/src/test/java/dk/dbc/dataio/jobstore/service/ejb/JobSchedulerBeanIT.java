package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
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
public class JobSchedulerBeanIT {
    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBeanIT.class);

    @Before
    public void setUp() throws Exception {
        em = JPATestUtils.createEntityManagerForIntegrationTest("jobstoreIT");
        // Execute flyway upgrade
        final Flyway flyway = new Flyway();
        flyway.setTable("schema_version");
        flyway.setBaselineOnMigrate(true);
        flyway.setDataSource(JPATestUtils.getTestDataSource("testdb"));
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.debug("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();

        JPATestUtils.runSqlFromResource(em, this, "JobSchedulerBeanIT_findWaitForChunks.sql");
    }


    @Test
    public void findChunksWaitingForMe() throws Exception {

        JobSchedulerBean bean= new JobSchedulerBean();
        bean.entityManager=em;

        assertThat(bean.findChunksWaitingForMe( new Key(1,1)), containsInAnyOrder( new Key(1,1)));
        assertThat(bean.findChunksWaitingForMe( new Key(0,1)), containsInAnyOrder( new Key(1,1), new Key(2,1)));

        List<Key> res=bean.findChunksWaitingForMe( new Key(3,0) );
        assertThat(res, containsInAnyOrder( new Key(1,0), new Key(1,1), new Key(1,2), new Key(1,3), new Key(2,0), new Key(2,1), new Key(2,2), new Key(2,3), new Key(2,4)));
    }

    @Test
    public void multipleCallesToChunkXxxxxxDoneIsIgnored() throws Exception {
        JPATestUtils.runSqlFromResource(em, JobSchedulerBeanIT.class, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        em.getTransaction().begin();
        em.createNativeQuery("DELETE FROM dependencytracking").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 1, 1, 1, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 2, 1, 2, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 3, 1, 3, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 4, 1, 4, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 5, 1, 5, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.getTransaction().commit();


        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = em;

        em.getTransaction().begin();
        for (int chunkid : new int[]{1, 3, 4, 5}) {
            bean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkid)
                    .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                    .build()
            );
        }
        em.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(em);

        // check no statuses is modified
        List<DependencyTrackingEntity> res = em.createNativeQuery("SELECT * FROM dependencytracking WHERE jobid=3 AND chunkId != status ").getResultList();
        assertThat("Test chunkProcessingDone did not change any chunk ", res.size(), is(0));


        em.getTransaction().begin();
        for (int chunkid : new int[]{1, 3, 4, 5}) {
            bean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkid)
                    .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                    .build()
            );
        }
        em.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(em);


        em.getTransaction().begin();
        for (int chunkid : new int[]{1, 2, 3, 4, 6}) {
            bean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkid)
                    .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                    .build()
            );
        }
        em.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(em);
    }

    @Test
    public void TickleFirstChunkDependency() throws Exception {
        JPATestUtils.runSqlFromResource(em, JobSchedulerBeanIT.class, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = em;
        JobSchedulerTransactionsBean jtbean= new JobSchedulerTransactionsBean();
        jtbean.entityManager = bean.entityManager;
        jtbean.sinkMessageProducerBean = mock(SinkMessageProducerBean.class);
        bean.jobSchedulerTransactionsBean = jtbean;



        Sink sink1=new SinkBuilder().setId(1).setContent(
                new SinkContentBuilder().setSinkType( SinkContent.SinkType.TICKLE ).build()
        ).build();

        em.getTransaction().begin();
        for (int chunkId : new int[]{0, 1, 2, 3}) {
            String ck=String.format("CK%d",chunkId);
            bean.scheduleChunk(new ChunkEntity()
                            .withJobId(3)
                            .withChunkId( chunkId ).withNumberOfItems((short) 1)
                            .withSequenceAnalysisData( makeSequenceAnalyceData(ck))
                    , sink1
                    , 1
            );
        }
        em.getTransaction().commit();

        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,0).getMatchKeys(), containsInAnyOrder("CK0", "1"));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,1).getMatchKeys(), containsInAnyOrder("CK1" ));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,2).getMatchKeys(), containsInAnyOrder("CK2" ));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,3).getMatchKeys(), containsInAnyOrder("CK3"));
    }


    @Test
    public void NonTickleFirstChunkDependency() throws Exception {
        JPATestUtils.runSqlFromResource(em, JobSchedulerBeanIT.class, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = em;
        JobSchedulerTransactionsBean jtbean= new JobSchedulerTransactionsBean();
        jtbean.entityManager = bean.entityManager;
        jtbean.sinkMessageProducerBean = mock(SinkMessageProducerBean.class);
        bean.jobSchedulerTransactionsBean = jtbean;



        Sink sink1=new SinkBuilder().setId(1).setContent(
                new SinkContentBuilder().setSinkType( SinkContent.SinkType.DUMMY ).build()
        ).build();

        em.getTransaction().begin();
        for (int chunkId : new int[]{0, 1, 2, 3}) {
            String ck=String.format("CK%d",chunkId);
            bean.scheduleChunk(new ChunkEntity()
                            .withJobId(3)
                            .withChunkId( chunkId ).withNumberOfItems((short) 1)
                            .withSequenceAnalysisData( makeSequenceAnalyceData(ck))
                    , sink1
                    , 1
            );
        }
        em.getTransaction().commit();

        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,0).getMatchKeys(), containsInAnyOrder("CK0" ));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,1).getMatchKeys(), containsInAnyOrder("CK1" ));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,2).getMatchKeys(), containsInAnyOrder("CK2" ));
        assertThat("check Ekstra key for chunk0", getDependencyTrackingEntity(3,3).getMatchKeys(), containsInAnyOrder("CK3"));
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
        JPATestUtils.clearEntityManagerCache(em);
        em.getTransaction().begin();

        LOGGER.info("Test Checker entityManager.find( job={}, chunk={} ) ", jobId, chunkId );
        DependencyTrackingEntity dependencyTrackingEntity = em.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(jobId, chunkId), LockModeType.PESSIMISTIC_READ);
        assertThat(dependencyTrackingEntity, is(notNullValue()));
        em.refresh(dependencyTrackingEntity);
        em.getTransaction().rollback();
        return dependencyTrackingEntity;
    }


}