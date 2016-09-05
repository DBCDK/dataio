package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
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
import java.util.Set;

public class JobSchedulerTransactionsBeanIT {

    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerTransactionsBeanIT.class);

    @Before
    public void setUp() throws Exception {
        StartupDBMigrator startupDBMigrator=new StartupDBMigrator().withDataSource( JPATestUtils.getTestDataSource("testdb") );
        startupDBMigrator.onStartup();

        em = JPATestUtils.createEntityManagerForIntegrationTest("jobstoreIT");
        JPATestUtils.runSqlFromResource(em, this, "JobSchedulerBeanIT_findWaitForChunks.sql");
    }

    @Test
    public void findChunksToWaitFor() throws Exception {

        JobSchedulerTransactionsBean bean= new JobSchedulerTransactionsBean();
        bean.entityManager=em;

        assertThat(bean.findChunksToWaitFor( 0, createSet( )).size(), is(0));

        assertThat(bean.findChunksToWaitFor( 0, createSet("K1")),containsInAnyOrder( new DependencyTrackingEntity.Key(1,1)));
        assertThat(bean.findChunksToWaitFor( 0, createSet("C1")),containsInAnyOrder( new DependencyTrackingEntity.Key(1,1)));


        assertThat(bean.findChunksToWaitFor( 0, createSet("KK2")), containsInAnyOrder( new DependencyTrackingEntity.Key(1,0), new DependencyTrackingEntity.Key(1,1), new DependencyTrackingEntity.Key(1,2), new DependencyTrackingEntity.Key(1,3) ));
        assertThat(bean.findChunksToWaitFor( 1, createSet("K4", "K6", "C4")), containsInAnyOrder( new DependencyTrackingEntity.Key(2,0), new DependencyTrackingEntity.Key(2,2), new DependencyTrackingEntity.Key(2,4)));
    }




    // TODO: move to common code some ware
     private <T> Set<T> createSet(T... elements) {
             Set<T> r=new HashSet<>();
             Collections.addAll(r, elements);
             return r;
     }

}

