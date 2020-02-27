package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.types.*;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.flowstore.ejb.StartupDBMigrator;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ja7 on 17-05-16.
 * Integration tests depends on localhost postgresql for development mode.
 * Uses Maven single instance for mvn verify
 */
public class FlowIT {
    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowIT.class);

    @Before
    public void setUp() throws Exception {
        // Execute flyway upgrade

        em = JPATestUtils.getIntegrationTestEntityManager("flowStoreIT");

        // Execute flyway upgrade
        StartupDBMigrator startupDBMigrator=new StartupDBMigrator().withDataSource( JPATestUtils.getIntegrationTestDataSource("testdb") );
        startupDBMigrator.onStartup();

        drop();
    }

    @After
    public void drop() {
        if( em.getTransaction().isActive() ) em.getTransaction().rollback();
        em.getTransaction().begin();
        em.createNativeQuery("delete from flow_binders").executeUpdate();
        em.createNativeQuery("delete from flows").executeUpdate();
        em.createNativeQuery("delete from flow_components").executeUpdate();
        em.getTransaction().commit();
    }

    @Test
    public void SimpleLoadStore() throws Exception {
        Flow flow= new Flow();

        final FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName("1")
                .setInvocationJavascriptName("invocationJavaScript")
                .setJavascripts(Collections.singletonList(new JavaScript("scripe","moduleName")))
                .setInvocationMethod("invocationFunction")
                .build();

        final dk.dbc.dataio.commons.types.FlowComponent flowComponent = new FlowComponent( 1, 1, flowComponentContent, null);
        final FlowContent flowContent  = new FlowContentBuilder()
                .setName("2")
                .setComponents(Collections.singletonList(flowComponent))
                .build();


        flow.setContent( new JSONBContext().marshall(flowContent));
        em.getTransaction().begin();
        em.persist( flow );
        em.getTransaction().commit();


        JPATestUtils.clearEntityManagerCache( em );

        Flow flow2 = em.find( Flow.class, flow.getId());


        assertThat( flow2, is( flow ));
    }

    @Test
    public void ListAllQuery() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "flowIT_testdata.sql");

        Query q=em.createNamedQuery(Flow.QUERY_FIND_ALL);
        List<Flow> result=q.getResultList();

        assertThat( result.size(), is(8));
        assertThat( result.get(0).getId(), is( 2352L ));
        assertThat( result.get(1).getId(), is( 3151L ));
        assertThat( result.get(7).getId(), is( 53L ));
    }

}
