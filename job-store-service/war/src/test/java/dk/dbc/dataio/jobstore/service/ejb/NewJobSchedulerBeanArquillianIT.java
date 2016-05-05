package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ja7 on 11-04-16.
 * Arquillian Test for NewJobSchedulerBeanArquillian.
 *
 * To Run from InteliJ use arguillian Plugin JBoss Arquillian Support
 *    - Manual Container Configuation -
 *    dk.dbc.arquillian.container : arquillian-glassfish-remote-3.1
 *
 */
@RunWith(Arquillian.class)
public class NewJobSchedulerBeanArquillianIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewJobSchedulerBeanArquillianIT.class);

    @EJB
    NewJobSchedulerBean newJobSchedulerBean;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Resource( lookup = "jdbc/dataio/jobstore" )
    DataSource dataSource;

    @Inject
    UserTransaction utx;

    // Init Done By
    @Before
    @Transactional
    public void setUp() throws Exception {
        // Execute flyway upgrade
        final Flyway flyway = new Flyway();
        flyway.setTable("schema_version");
        flyway.setBaselineOnMigrate(true);
        flyway.setDataSource(this.dataSource);
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.debug("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();
    }


    @After
    public void dbCleanUp() throws Exception {
        utx.begin();
        entityManager.joinTransaction();

        entityManager.createNativeQuery("delete from job").executeUpdate();
        utx.commit();
    }

    @Deployment
    public static WebArchive createDeployment() {
        LOGGER.warn( "in public static WebArchive createDeployment() ");
        try {

            // Change eclipse Link log level and destination
            PersistenceDescriptor persistence = Descriptors.importAs(PersistenceDescriptor.class)
                    .fromFile("src/main/resources/META-INF/persistence.xml")
                    .getOrCreatePersistenceUnit().name("jobstorePU")
                    .getOrCreateProperties()
                    .createProperty().name("eclipselink.logging.file").value("../logs/eclipselink.log").up()
                    .createProperty().name("eclipselink.logging.level").value("FINE").up()
                    .createProperty().name("eclipselink.logging.logger").value("JavaLogger").up()
                    .up() // update Properties */
                    .up(); // update PersistenceUnit

            LOGGER.info("persistence.xml : {}", persistence.exportAsString());
            WebArchive war = ShrinkWrap.create(WebArchive.class, "jobstore-jobscheduler-test.war")
                    .addPackages(true, "dk/dbc/dataio/jobstore/service/entity", "dk/dbc/dataio/jobstore/service/digest",
                            "dk/dbc/dataio/jobstore/service/cdi", "dk/dbc/dataio/jobstore/service/param",
                            "dk/dbc/dataio/jobstore/service/partitioner", "dk/dbc/dataio/jobstore/service/util"
                            )

                    .addClasses(PgJobStoreRepository.class, RepositoryBase.class)
                    .addClasses(JobProcessorMessageProducerBean.class)
                    .addClasses(DmqMessageConsumerBean.class)
                    .addClasses(SinkMessageProducerBean.class)
                    .addClasses(NewJobSchedulerBean.class)
                    .addClasses(TestJobProcessorMessageConsumerBean.class)
                    ;

            // Add Maven Dependencyes  // .workOffline fejler med  mvnlocal ( .m2 i projectHome
            //File[] files = Maven.configureResolver().workOffline().withMavenCentralRepo(false).loadPomFromFile("pom.xml")
            File[] files = Maven.configureResolver().loadPomFromFile("pom.xml")
                    .importRuntimeDependencies().resolve().withTransitivity().asFile();
            war.addAsLibraries(files);

            // Add DB Migrations
            for (File file : new File("src/main/resources/db/migration").listFiles()) {
                if( file.getName().equals(".svn") ) {
                    continue;
                }

                String fileNameAdded="classes/db/migration/"+file.getName();
                war.addAsWebInfResource(file, fileNameAdded);
            }

            war.addAsResource( new StringAsset(persistence.exportAsString()), "META-INF/persistence.xml");

            war.addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"));
            war.addAsWebInfResource(new File("src/main/webapp/WEB-INF/glassfish-ejb-jar.xml"));


            war.addAsWebInfResource(new File("src/test/resources/newjobschedulerbean-ejb-jar.xml"), "ejb-jar.xml");
            war.addAsWebInfResource( new File("src/test/resources/arquillian_logback.xml"), "classes/logback-test.xml");
            war.addAsResource(new File("src/test/resources/","JobSchedulerBeanIT_findWaitForChunks.sql"));

            //LOGGER.info("war {}", war.toString(true));

            return war;
        } catch(Exception e) {
            LOGGER.error("War building failed",e);
            throw e;
        }
    }

    @Test
    public void scheduleChunkSimple() throws IOException, URISyntaxException, HeuristicRollbackException, HeuristicMixedException, NotSupportedException, RollbackException, SystemException, JobStoreException, InterruptedException {
        runSqlFromResource( "JobSchedulerBeanIT_findWaitForChunks.sql");
        // Given
        ChunkEntity chunkEntity= new ChunkEntity();
        chunkEntity.setKey( new ChunkEntity.Key(0,3));
        chunkEntity.setSequenceAnalysisData( new SequenceAnalysisData( makeSet("f1")));

        newItemEntity( 3,0,0);

        // when
        newJobSchedulerBean.scheduleChunk( chunkEntity, new SinkBuilder().setId(3).build());

        // Then
        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks(1);
        assertThat( TestJobProcessorMessageConsumerBean.getChunksRetrived().size(),is(1));

        JPATestUtils.clearEntityManagerCache( entityManager );
        utx.begin();
        entityManager.joinTransaction();
        DependencyTrackingEntity dependencyTrackingEntity=entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(3,0));
        utx.commit();

        assertThat( dependencyTrackingEntity, notNullValue());
        assertThat( dependencyTrackingEntity.getStatus(), is(DependencyTrackingEntity.ChunkProcessStatus.QUEUED_TO_PROCESS));


        // Given a chunk Returned from Processing it moves to Sink
        Chunk chunk=new ChunkBuilder(PROCESSED)
                .setJobId(3)
                .setChunkId(0).
                appendItem( new ChunkItemBuilder().setData("ProcessdChunk").build() )
                .build();

        newJobSchedulerBean.chunkProcessingDoneScheduleForSink( chunk );



    }

    protected void newItemEntity(int jobId, int chunkId, int itemId) throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey( new ItemEntity.Key(jobId, chunkId, (short) itemId) );
        itemEntity.setState(new State());
        itemEntity.setPartitioningOutcome( new ChunkItemBuilder().setData("chunkItem").build());

        utx.begin();
        entityManager.joinTransaction();
        entityManager.persist( itemEntity );
        utx.commit();
    }



    Set<String> makeSet(String... s) {
        Set<String> res= new HashSet<>();
        Collections.addAll(res,s);
        return res;
    }

    public void runSqlFromResource( String resouceName ) throws IOException, URISyntaxException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        String sql= JPATestUtils.readResource(this, resouceName);
        utx.begin();
        entityManager.joinTransaction();


        Query q = entityManager.createNativeQuery(sql);
        q.executeUpdate();
        utx.commit();

    }


}