package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Integer.max;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

/**
 * Created by ja7 on 07-04-16.
 * Initial EntityManager Test
 */
public class DependencyTrackingEntityIT {

    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEntityIT.class);

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
    }

    @After
    public void cleanEntityManagerUp() {
        if (em.getTransaction().isActive() ) {
            em.getTransaction().rollback();
        }
    }

    @Test
    public void loadItemEntity() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "dependencyTracking_initialTestData.sql");
        em.getTransaction().begin();

        int i=0;
        for( DependencyTrackingEntity.ChunkProcessStatus chunkProcessStatus : DependencyTrackingEntity.ChunkProcessStatus.values() ) {
            DependencyTrackingEntity entity = new DependencyTrackingEntity();
            entity.setStatus( chunkProcessStatus );
            entity.setKey(new Key(1,i));
            entity.setMatchKeys(createSet("5 023 297 2", "2 004 091 2", "4 016 438 3", "0 198 393 8", "2 022 704 4", "2 017 916 3", "5 000 116 4", "5 017 224 4", "2 002 537 9", "5 005 396 2", "4 107 001 3", "2 017 919 8", "0 193 840 1", "0 189 413 7", "2 015 874 3", "5 017 504 9", "0 189 446 3", "2 015 875 1", "5 044 974 2", "5 007 721 7", "f"+i));
            int cid=max(1, i-1);
            entity.setBlocking(createSet(new Key(1, 0), new Key(2, 0), new Key(2, cid), new Key(2, 5), new Key(2, 7)));
            entity.setWaitingOn(createSet(new Key(1, 1), new Key(3,0 )));

            em.persist(entity);
            ++i;
        }
        em.getTransaction().commit();

        JPATestUtils.clearEntityManagerCache( em );

        DependencyTrackingEntity e=em.find(DependencyTrackingEntity.class,  new DependencyTrackingEntity.Key(1,0));

        assertThat( e.getStatus(), is(DependencyTrackingEntity.ChunkProcessStatus.READY_TO_PROCESS));
        assertThat( e.getSinkid(), is(0) );

        assertThat( e.getMatchKeys(), containsInAnyOrder( "5 023 297 2", "2 004 091 2", "4 016 438 3", "0 198 393 8", "2 022 704 4", "2 017 916 3", "5 000 116 4", "5 017 224 4", "2 002 537 9", "5 005 396 2", "4 107 001 3", "2 017 919 8", "0 193 840 1", "0 189 413 7", "2 015 874 3", "5 017 504 9", "0 189 446 3", "2 015 875 1", "5 044 974 2", "5 007 721 7", "f0"));
        assertThat( e.getBlocking(), containsInAnyOrder( new Key(1, 0), new Key(2, 0), new Key(2, 1), new Key(2, 5), new Key(2, 7)));
        assertThat( e.getWaitingOn(), containsInAnyOrder( new Key(1,1), new Key(3,0)));


    }

    @Test
    public void SinkIdStatusCountResultQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "dependencyTracking_sinkStatusLoadTest.sql");
        em.getTransaction().begin();

        Query q=em.createNamedQuery("SinkIdStatusCount");
        List<SinkIdStatusCountResult> result=q.getResultList();


        assertThat(result.size(), is(10));

        em.getTransaction().commit();
        assertThat("result[0]", result.get(0) ,is( new SinkIdStatusCountResult(1,DependencyTrackingEntity.ChunkProcessStatus.READY_TO_PROCESS, 5)));
        assertThat("result[1]", result.get(1) ,is( new SinkIdStatusCountResult(1,DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_PROCESS, 4)));
        assertThat("result[2]", result.get(2) ,is( new SinkIdStatusCountResult(1,DependencyTrackingEntity.ChunkProcessStatus.BLOCKED, 2)));
        assertThat("result[3]", result.get(3) ,is( new SinkIdStatusCountResult(1,DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER, 3)));
        assertThat("result[4]", result.get(4) ,is( new SinkIdStatusCountResult(1,DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_DELIVERY, 1)));

        assertThat("result[5]", result.get(5) ,is( new SinkIdStatusCountResult(1551,DependencyTrackingEntity.ChunkProcessStatus.READY_TO_PROCESS, 1)));
        assertThat("result[6]", result.get(6) ,is( new SinkIdStatusCountResult(1551,DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_PROCESS, 2)));
        assertThat("result[7]", result.get(7) ,is( new SinkIdStatusCountResult(1551,DependencyTrackingEntity.ChunkProcessStatus.BLOCKED, 3)));
        assertThat("result[8]", result.get(8) ,is( new SinkIdStatusCountResult(1551,DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER, 4)));
        assertThat("result[9]", result.get(9) ,is( new SinkIdStatusCountResult(1551,DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_DELIVERY, 5)));
    }

    @Test
    public void FindAllForSinkResultQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "dependencyTracking_sinkStatusLoadTest.sql");

        final Query query = em.createNamedQuery(DependencyTrackingEntity.QUERY_JOB_COUNT_CHUNK_COUNT).setParameter(1, 1);
        final Object[] resultList = (Object[]) query.getSingleResult();

        assertThat(resultList[0], is(1L));  // numberOfJobs
        assertThat(resultList[1], is(15L)); // numberOfChunks
    }

    private <T> Set<T> createSet(T... elements) {
        Set<T> r=new HashSet<>();
        Collections.addAll(r, elements);
        return r;
    }

}