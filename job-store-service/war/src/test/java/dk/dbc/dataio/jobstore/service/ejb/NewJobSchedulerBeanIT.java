package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.*;
import dk.dbc.dataio.jobstore.service.entity.ItemEntityIT;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
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
    }

    @Test
    public void findChunksToWaitFor() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "JobSchedulerBeanIT_findWaitForChunks.sql");

        NewJobSchedulerBean bean= new NewJobSchedulerBean();
        bean.entityManager=em;

        assertThat(bean.findChunksToWaitFor( 0, createSet( )).size(), is(0));

        assertThat(bean.findChunksToWaitFor( 0, createSet("K1")),containsInAnyOrder( new Key(1,1)));
        assertThat(bean.findChunksToWaitFor( 0, createSet("C1")),containsInAnyOrder( new Key(1,1)));


        assertThat(bean.findChunksToWaitFor( 0, createSet("KK2")), containsInAnyOrder( new Key(1,0), new Key(1,1), new Key(1,2), new Key(1,3) ));
        assertThat(bean.findChunksToWaitFor( 1, createSet("K4", "K6", "C4")), containsInAnyOrder( new Key(2,0), new Key(2,2), new Key(2,4)));
    }

    // TODO: move to common code some ware
    private <T> Set<T> createSet(T... elements) {
            Set<T> r=new HashSet<>();
            Collections.addAll(r, elements);
            return r;
        }

}