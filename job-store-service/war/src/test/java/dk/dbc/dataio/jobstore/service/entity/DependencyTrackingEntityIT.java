package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key;
import static java.lang.Integer.max;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    @Test
    public void loadItemEntity() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "dependencyTracking_initialTestData.sql");
        em.getTransaction().begin();

        int i=0;
        for( DependencyTrackingEntity.ChunkProcessStatus chunkProcessStatus : DependencyTrackingEntity.ChunkProcessStatus.values() ) {
            DependencyTrackingEntity entity = new DependencyTrackingEntity();
            entity.setStatus( chunkProcessStatus );
            entity.setKey(new Key(i, 1));
            entity.setMatchKeys(createSet("5 023 297 2", "2 004 091 2", "4 016 438 3", "0 198 393 8", "2 022 704 4", "2 017 916 3", "5 000 116 4", "5 017 224 4", "2 002 537 9", "5 005 396 2", "4 107 001 3", "2 017 919 8", "0 193 840 1", "0 189 413 7", "2 015 874 3", "5 017 504 9", "0 189 446 3", "2 015 875 1", "5 044 974 2", "5 007 721 7", "f"+i));
            int cid=max(1, i-1);
            entity.setBlocking(createSet(new Key(0, 1), new Key(0, 2), new Key(cid, 2), new Key(5, 2), new Key(7, 2)));
            entity.setWaitingOn(createSet(new Key(1, 1), new Key(0, 3)));

            em.persist(entity);
            ++i;
        }
        em.getTransaction().commit();

        JPATestUtils.clearEntityManagerCache( em );

        DependencyTrackingEntity e=em.find(DependencyTrackingEntity.class,  new DependencyTrackingEntity.Key(0,1));

        assertThat( e.getStatus(), is(DependencyTrackingEntity.ChunkProcessStatus.ReadyToProcess));
        assertThat( e.getMatchKeys(), containsInAnyOrder( "5 023 297 2", "2 004 091 2", "4 016 438 3", "0 198 393 8", "2 022 704 4", "2 017 916 3", "5 000 116 4", "5 017 224 4", "2 002 537 9", "5 005 396 2", "4 107 001 3", "2 017 919 8", "0 193 840 1", "0 189 413 7", "2 015 874 3", "5 017 504 9", "0 189 446 3", "2 015 875 1", "5 044 974 2", "5 007 721 7", "f0"));

        e.setWaitingOn(createSet(new Key(1, 1), new Key(0, 3)));
        assertThat( e.getWaitingOn(), containsInAnyOrder( new Key(1,1), new Key(0,3)));
        assertThat( e.getBlocking(), containsInAnyOrder(new Key(0,1), new Key(0, 2), new Key(1,2), new Key(5,2), new Key(7,2)));
        assertThat(e.getSinkid(), is(0) );

    }


     private <T> Set<T> createSet(T... elements) {
        Set<T> r=new HashSet<>();
        Collections.addAll(r, elements);
        return r;
    }

}