package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
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
public class NewJobSchedulerBeanIT {
    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewJobSchedulerBeanIT.class);

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
    public void findChunksToWaitFor() throws Exception {

        NewJobSchedulerBean bean= new NewJobSchedulerBean();
        bean.entityManager=em;

        assertThat(bean.findChunksToWaitFor( 0, createSet( )).size(), is(0));

        assertThat(bean.findChunksToWaitFor( 0, createSet("K1")),containsInAnyOrder( new Key(1,1)));
        assertThat(bean.findChunksToWaitFor( 0, createSet("C1")),containsInAnyOrder( new Key(1,1)));


        assertThat(bean.findChunksToWaitFor( 0, createSet("KK2")), containsInAnyOrder( new Key(1,0), new Key(1,1), new Key(1,2), new Key(1,3) ));
        assertThat(bean.findChunksToWaitFor( 1, createSet("K4", "K6", "C4")), containsInAnyOrder( new Key(2,0), new Key(2,2), new Key(2,4)));
    }


    @Test
    public void findChunksWaitingForMe() throws Exception {

        NewJobSchedulerBean bean= new NewJobSchedulerBean();
        bean.entityManager=em;

        assertThat(bean.findChunksWaitingForMe( new Key(1,1)), containsInAnyOrder( new Key(1,1)));
        assertThat(bean.findChunksWaitingForMe( new Key(0,1)), containsInAnyOrder( new Key(1,1), new Key(2,1)));

        List<Key> res=bean.findChunksWaitingForMe( new Key(3,0) );
        assertThat(res, containsInAnyOrder( new Key(1,0), new Key(1,1), new Key(1,2), new Key(1,3), new Key(2,0), new Key(2,1), new Key(2,2), new Key(2,3), new Key(2,4)));
    }

    @Test
    public void MultibleCallesToxxDoneIsIgnored() throws Exception {
        JPATestUtils.runSqlFromResource(em, NewJobSchedulerBeanIT.class, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        em.getTransaction().begin();
        em.createNativeQuery("DELETE FROM dependencytracking").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 1, 1, 1, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 2, 1, 2, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 3, 1, 3, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 4, 1, 4, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.createNativeQuery("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 5, 1, 5, '[{\"jobId\": 3, \"chunkId\": 0}]', NULL, '[\"K8\", \"KK2\", \"C4\"]')").executeUpdate();
        em.getTransaction().commit();


        NewJobSchedulerBean bean = new NewJobSchedulerBean();
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
        for (int chunkid : new int[]{1, 2, 3, 4 , 6}) {
            bean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkid)
                    .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                    .build()
            );
        }
        em.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(em);



    }

    // TODO: move to common code some ware
    private <T> Set<T> createSet(T... elements) {
            Set<T> r=new HashSet<>();
            Collections.addAll(r, elements);
            return r;
    }

}