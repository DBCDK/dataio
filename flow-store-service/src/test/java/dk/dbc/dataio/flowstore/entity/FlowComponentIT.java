package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.types.*;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
import dk.dbc.dataio.flowstore.ejb.StartupDBMigrator;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import static org.hamcrest.core.Is.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by ja7 on 17-05-16.
 * Integration tests depends on localhost postgresql for development mode.
 * Uses Maven single instance for mvn verify
 */
public class FlowComponentIT {
    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowComponentIT.class);
    final static String testDbName="testdb";

    @Before
    public void setUp() throws Exception {
        // Execute flyway upgrade

        StartupDBMigrator startupDBMigrator=new StartupDBMigrator().withDataSource( JPATestUtils.getTestDataSource(testDbName) );
        startupDBMigrator.onStartup();

        em = JPATestUtils.createEntityManagerForIntegrationTest("flowStoreIT");
        drop();
    }

    @After
    public void drop() {
        if( em.getTransaction().isActive() ) em.getTransaction().rollback();
        em.getTransaction().begin();

        em.createNativeQuery("delete from flow_binders_search_index").executeUpdate();
        em.createNativeQuery("delete from flow_binders_submitters").executeUpdate();
        em.createNativeQuery("delete from flow_components").executeUpdate();
        em.createNativeQuery("delete from flow_binders").executeUpdate();
        em.createNativeQuery("delete from flows").executeUpdate();
        em.getTransaction().commit();
    }

    @Test
    public void simpeLoadStore() throws Exception {

        final FlowComponent flowComponent= new FlowComponent()
                .withId( 1l )
                .withVersion( 1l)
                .withContent( new FlowComponentContentJsonBuilder().build() )
                .withNext( new FlowComponentContentJsonBuilder().setDescription("next").build());

        em.getTransaction().begin();
        em.persist( flowComponent );
        em.getTransaction().commit();


        JPATestUtils.clearEntityManagerCache( em );

        FlowComponent hc2 = em.find( FlowComponent.class, 1L);

        assertThat( flowComponent, is( hc2 ));
    }

    @Test
    public void ListAllQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "flowIT_testdata.sql");

        Query q=em.createNamedQuery(FlowComponent.QUERY_FIND_ALL);
        List<HarvesterConfig> result=q.getResultList();

        assertThat( result.size(), is( 42));

        assertThat( result.get(0), is( new FlowComponent().withId(852L).withVersion(7L)
                .withContent("{\"name\": \"AKP test\", \"description\": \"skal slettes\", \"javascripts\": [], \"svnRevision\": 81689, \"requireCache\": \"\", \"invocationMethod\": \"begin\", \"invocationJavascriptName\": \"trunk/js/cql_xml_to_html.js\", \"svnProjectForInvocationJavascript\": \"datawell-convert\"}")
                .withNext("{\"name\": \"AKP test\", \"description\": \"skal slettes\", \"javascripts\": [], \"svnRevision\": 88286, \"requireCache\": \"\", \"invocationMethod\": \"begin\", \"invocationJavascriptName\": \"trunk/js/cql_xml_to_html.js\", \"svnProjectForInvocationJavascript\": \"datawell-convert\"}")
        ));
    }

}