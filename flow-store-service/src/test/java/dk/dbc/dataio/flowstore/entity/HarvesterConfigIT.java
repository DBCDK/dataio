package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.flowstore.ejb.StartupDBMigrator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ja7 on 17-05-16.
 * Integration tests depends on localhost postgresql for development mode.
 * Uses Maven single instance for mvn verify
 */
public class HarvesterConfigIT {
    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigIT.class);
    final static String testDbName="testdb";


    @Before
    public void setUp() throws Exception {
        // Execute flyway upgrade
        em = JPATestUtils.getIntegrationTestEntityManager("flowStoreIT");
        StartupDBMigrator startupDBMigrator=new StartupDBMigrator().withDataSource( JPATestUtils.getIntegrationTestDataSource(testDbName) );
        startupDBMigrator.onStartup();

        drop();
    }

    @After
    public void drop() {
        if( em.getTransaction().isActive() ) em.getTransaction().rollback();
        em.getTransaction().begin();
        em.createNativeQuery("delete from harvester_configs").executeUpdate();
        em.getTransaction().commit();
    }

    @Test
    public void simpeLoadStore() throws Exception {
        HarvesterConfig hc= new HarvesterConfig().withId(1L)
                .withVersion(1L)
                .withType("Type")
                .withContent( "{\"id\": 1}"  );
        em.getTransaction().begin();
        em.persist( hc );
        em.getTransaction().commit();


        JPATestUtils.clearEntityManagerCache( em );

        HarvesterConfig hc2 = em.find( HarvesterConfig.class, 1L);

        assertThat( hc, is( hc2 ));
    }

    @Test
    public void ListAllQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "harvesterConfigIT_testdata.sql");

        Query q=em.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE).setParameter("type", "dk.dbc.dataio.harvester.types.RRHarvesterConfig");
        List<HarvesterConfig> result=q.getResultList();

        assertThat( result.size(), is( 13));
    }

    @Test
    public void ListActiveQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "harvesterConfigIT_testdata.sql");

        Query q=em.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_ENABLED_OF_TYPE).setParameter(1, "dk.dbc.dataio.harvester.types.RRHarvesterConfig");
        List<HarvesterConfig> result=q.getResultList();

        assertThat( result.size(), is( 11));
    }
}
