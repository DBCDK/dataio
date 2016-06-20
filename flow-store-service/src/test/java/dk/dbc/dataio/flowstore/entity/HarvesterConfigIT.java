package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
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

    @Before
    public void setUp() throws Exception {
        // Execute flyway upgrade
        final Flyway flyway = new Flyway();
        flyway.setTable("schema_version");
        flyway.setBaselineOnMigrate(true);
        flyway.setDataSource(JPATestUtils.getTestDataSource("testdb"));
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.debug("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();

        em = JPATestUtils.createEntityManagerForIntegrationTest("flowStoreIT");
        em.getTransaction().begin();
        em.createNativeQuery("delete from harvester_configs").executeUpdate();
        em.getTransaction().commit();
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

        result=em.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE).setParameter("type", "dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig").getResultList();

        assertThat( result.size(), is(1));
        assertThat( result.get(0), is( new HarvesterConfig().withId(14L).withVersion(1L).withType("dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig")
                .withContent("{\"content\": {\"includeRelations\": true, \"resource\": \"jdbc/dataio/rawrepo-cisterne\", \"format\": \"katalog\", \"batchSize\": 10000, \"destination\": \"broend-aqua\", \"consumerId\": \"broend30-sync\", \"isEnabled\": true, \"openAgencyTarget\": {\"url\": \"http://openagency.addi.dk/2.25/\"}, \"formatOverrides\": {\"870970\": \"basis\"}, \"type\": \"TRANSIENT\", \"id\": \"broend30-sync-cisterne\"}, \"version\": 1, \"type\": \"dk.dbc.dataio.harvester.types.RRHarvesterConfig\", \"id\": 13}")
        ));
    }

    @Test
    public void ListActiveQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "harvesterConfigIT_testdata.sql");

        Query q=em.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_ENABLED_OF_TYPE).setParameter(1, "dk.dbc.dataio.harvester.types.RRHarvesterConfig");
        List<HarvesterConfig> result=q.getResultList();

        assertThat( result.size(), is( 11));

        result=em.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_ENABLED_OF_TYPE).setParameter(1, "dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig").getResultList();

        assertThat( result.size(), is(1));

        assertThat( result.get(0), is( new HarvesterConfig().withId(14L).withVersion(1L).withType("dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig")
                .withContent("{\"content\": {\"includeRelations\": true, \"resource\": \"jdbc/dataio/rawrepo-cisterne\", \"format\": \"katalog\", \"batchSize\": 10000, \"destination\": \"broend-aqua\", \"consumerId\": \"broend30-sync\", \"isEnabled\": true, \"openAgencyTarget\": {\"url\": \"http://openagency.addi.dk/2.25/\"}, \"formatOverrides\": {\"870970\": \"basis\"}, \"type\": \"TRANSIENT\", \"id\": \"broend30-sync-cisterne\"}, \"version\": 1, \"type\": \"dk.dbc.dataio.harvester.types.RRHarvesterConfig\", \"id\": 13}")
        ));
    }

    @Test
    public void getByUshHarvesterJobIdQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "harvesterConfigIT_testdata.sql");

        Query q = em.createNamedQuery(HarvesterConfig.QUERY_FIND_TYPE_WITH_CONTENT);
        q.setParameter(1, UshSolrHarvesterConfig.class.getName());
        q.setParameter(2, "{\"ushHarvesterJobId\": 10002}");

        List<HarvesterConfig> result= q.getResultList();

        assertThat(result.size(), is(1));

        assertThat(result.get(0), is(new HarvesterConfig().withId(15L).withVersion(1L).withType(UshSolrHarvesterConfig.class.getName())
                .withContent("{\"content\": {\"name\": \"testName\", \"description\": \"testDescription\", \"format\": \"katalog\", \"destination\": \"broend-aqua\", \"submitterNumber\": \"1234566\", \"ushHarvesterJobId\": 10002, \"ushHarvesterProperties\": null, \"timeOfLastHarvest\": \"2016-05-23T13:13:32.515+02:00\", \"enabled\": false")
        ));
    }
}