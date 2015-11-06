package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by ja7 on 02-11-15.
 * Test load af
 */
public class ItemEntityIT {

    private EntityManager em;
    @Before
    public void setUp() throws Exception {
        em= JPATestUtils.createEntityManagerForIntegrationTest("jobstoreIT");
        JPATestUtils.clearDatabase(em);
    }

    @Test
    public void loadItemEntity() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "create_jobstore_v14.sql");
        JPATestUtils.runSqlFromResource(em, this, "itemEntityIT_upgrade.sql");


        int jobid=39098;
        for( short id = 0; id <= 9; id++) {
            ItemEntity item = em.find(ItemEntity.class, new ItemEntity.Key(jobid, 0, (short) 1));
            assertThat(item.getDeliveringOutcome(), notNullValue());
        }

        jobid=39044;
        ItemEntity item = em.find(ItemEntity.class, new ItemEntity.Key(jobid, 0, (short) 0));
        assertThat(item.getPartitioningOutcome(), nullValue());
        assertThat(item.getDeliveringOutcome(), nullValue());
        assertThat(item.getProcessingOutcome(), nullValue());

    }
}