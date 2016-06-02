package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkProcessStatus;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by ja7 on 11-04-16.
 * Arquillian Test for NewJobSchedulerBeanArquillian.
 * <p>
 * To Run from InteliJ use arguillian Plugin JBoss Arquillian Support
 * - Manual Container Configuation -
 * dk.dbc.arquillian.container : arquillian-glassfish-remote-3.1
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NewJobSchedulerBeanArquillianIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewJobSchedulerBeanArquillianIT.class);

    @EJB
    NewJobSchedulerBean newJobSchedulerBean;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Resource(lookup = "jdbc/dataio/jobstore")
    DataSource dataSource;

    @Inject
    UserTransaction utx;


    @EJB
    SinkMessageProducerBean sinkMessageProducerBean;

    @Before
    public void clearTestConsumers() throws Exception {
        TestJobProcessorMessageConsumerBean.reset();
        TestSinkMessageConsumerBean.reset();
        dbCleanUp();
    }


    @After
    public void dbCleanUp() throws Exception {
        utx.begin();
        entityManager.joinTransaction();

        entityManager.createNativeQuery("DELETE FROM job").executeUpdate();
        utx.commit();
    }

    @Deployment
    public static WebArchive createDeployment() {
        LOGGER.warn("in public static WebArchive createDeployment() ");
        try {

            // Change eclipse Link log level and destination
            PersistenceDescriptor persistence = Descriptors.importAs(PersistenceDescriptor.class)
                    .fromFile("src/main/resources/META-INF/persistence.xml")
                    .getOrCreatePersistenceUnit().name("jobstorePU")
                    .getOrCreateProperties()
                    .createProperty().name("eclipselink.logging.file").value("../logs/eclipselink.log").up()
                    .createProperty().name("eclipselink.logging.level").value("FINEST").up()
                    .createProperty().name("eclipselink.logging.logger").value("JavaLogger").up()
                    .up() // update Properties */
                    .up(); // update PersistenceUnit

            WebArchive war = ShrinkWrap.create(WebArchive.class, "jobstore-jobscheduler-test.war")
                    .addPackages(true, "dk/dbc/dataio/jobstore/service/entity", "dk/dbc/dataio/jobstore/service/digest",
                            "dk/dbc/dataio/jobstore/service/cdi", "dk/dbc/dataio/jobstore/service/param",
                            "dk/dbc/dataio/jobstore/service/partitioner", "dk/dbc/dataio/jobstore/service/util"
                    )

                    .addClasses(PgJobStoreRepository.class, RepositoryBase.class, StartupDBMigrator.class)
                    .addClasses(JobProcessorMessageProducerBean.class)
                    .addClasses(DmqMessageConsumerBean.class)
                    .addClasses(SinkMessageProducerBean.class)
                    .addClasses(NewJobSchedulerBean.class)
                    .addClasses(TestJobProcessorMessageConsumerBean.class)
                    .addClasses(TestSinkMessageConsumerBean.class);

            // Add Maven Dependencyes  // .workOffline fejler med  mvnlocal ( .m2 i projectHome
            File[] files = Maven.configureResolver().workOffline().withMavenCentralRepo(false).loadPomFromFile("pom.xml")
                    //File[] files = Maven.configureResolver().loadPomFromFile("pom.xml")
                    .importRuntimeDependencies().resolve().withTransitivity().asFile();
            war.addAsLibraries(files);

            // Add DB Migrations
            for (File file : new File("src/main/resources/db/migration").listFiles()) {
                if (file.getName().equals(".svn")) {
                    continue;
                }

                String fileNameAdded = "classes/db/migration/" + file.getName();
                war.addAsWebInfResource(file, fileNameAdded);
            }

            war.addAsResource(new StringAsset(persistence.exportAsString()), "META-INF/persistence.xml");

            war.addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"));
            war.addAsWebInfResource(new File("src/main/webapp/WEB-INF/glassfish-ejb-jar.xml"));


            war.addAsWebInfResource(new File("src/test/resources/newjobschedulerbean-ejb-jar.xml"), "ejb-jar.xml");
            war.addAsWebInfResource(new File("src/test/resources/arquillian_logback.xml"), "classes/logback-test.xml");
            war.addAsResource(new File("src/test/resources/", "JobSchedulerBeanArquillianIT_findWaitForChunks.sql"));

            //LOGGER.info("war {}", war.toString(true));

            return war;
        } catch (Exception e) {
            LOGGER.error("War building failed", e);
            throw e;
        }
    }

    /*
      Test 2 Chunks (3,0) Is gowning thru the Stages
             Chunks (3,1) Is blocked by (3,1) and released for Delivering when 3.0 is done.
     */
    @Test
    public void scheduleChunkSimple() throws Exception {
        NewJobSchedulerBean.resetQueuedToDelivering((long) 3, 0);
        NewJobSchedulerBean.resetQueuedToDelivering((long) 3, 0);


        runSqlFromResource("JobSchedulerBeanArquillianIT_findWaitForChunks.sql");
        dbSingleUpdate("INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 1, 3, 4, '[{\"jobId\": 3, \"chunkId\": 0}]', null, '[\"K8\", \"KK2\", \"C4\"]')");
        // Given
        ChunkEntity chunkEntity = new ChunkEntity()
                .withJobId(3)
                .withChunkId(0)
                .withSequenceAnalysisData(new SequenceAnalysisData(makeSet("f1")));

        // when
        newJobSchedulerBean.scheduleChunk(chunkEntity, new SinkBuilder().setId(3).build());

        // Then
        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks(1);

        assertThat(TestJobProcessorMessageConsumerBean.getChunksRetrived().size(), is(1));

        DependencyTrackingEntity dependencyTrackingEntity = getDependencyTrackingEntity(3, 0);

        assertThat(dependencyTrackingEntity, notNullValue());
        assertThat(dependencyTrackingEntity.getStatus(), is(ChunkProcessStatus.QUEUED_TO_PROCESS));


        // Given a chunk Returned from Processing it moves to Sink
        Chunk chunk = new ChunkBuilder(PROCESSED)
                .setJobId(3)
                .setChunkId(0)
                .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                .build();
        // When
        newJobSchedulerBean.chunkProcessingDone(chunk);


        TestSinkMessageConsumerBean.waitForDeliveringOfChunks(1);
        // Then

        dependencyTrackingEntity = getDependencyTrackingEntity(3, 0);

        assertThat(dependencyTrackingEntity, notNullValue());
        assertThat(dependencyTrackingEntity.getStatus(), is(ChunkProcessStatus.QUEUED_TO_DELIVERY));

        // whenn chunk returns from Delivering
        newJobSchedulerBean.chunkDeliveringDone(chunk);

        // Then
        JPATestUtils.clearEntityManagerCache(entityManager);
        //Check no chunks is waiting for chunk(3,0)

        String keyAsJson = ConverterJSONBContext.getInstance().marshall(new DependencyTrackingEntity.Key(3, 0));
        Query query = entityManager.createNativeQuery("select jobid, chunkid from dependencyTracking where waitingon @> '[" + keyAsJson + "]'", "JobIdChunkIdResult");
        assertThat(query.getResultList().size(), is(0));

        TestSinkMessageConsumerBean.waitForDeliveringOfChunks(1);

        DependencyTrackingEntity dep = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(3, 0));
        assertThat(dep, is(nullValue()));

        // Test that chunk 3.1 is ready for sink.
        dep = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(3, 1));
        assertThat(dep.getStatus(), is(ChunkProcessStatus.QUEUED_TO_DELIVERY));

        // When 3.0 is returned from sink
        newJobSchedulerBean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                .setJobId(3)
                .setChunkId(1)
                .appendItem(new ChunkItemBuilder().setData("Processed Chunk").build())
                .build());
    }


    @Test
    public void RateLimits() throws Exception {

        runSqlFromResource("JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        // when threre is only space for one chunk to sink
        Sink sink1 = new SinkBuilder().setId(1).build();

        // Fake queue size is 1.
        NewJobSchedulerBean.resetQueuedToProcessing(sink1.getId(), NewJobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK - 1);
        NewJobSchedulerBean.resetQueuedToDelivering(sink1.getId(), NewJobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK - 1);

        newJobSchedulerBean.scheduleChunk(new ChunkEntity()
                        .withJobId(3).withChunkId(0)
                        .withSequenceAnalysisData(new SequenceAnalysisData(makeSet("f1"))),
                sink1);
        // Chunk 3.0 must take last slot before chunk is delayed.

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks(1);

        newJobSchedulerBean.scheduleChunk(new ChunkEntity()
                        .withJobId(3).withChunkId(1)
                        .withSequenceAnalysisData(new SequenceAnalysisData(makeSet("f2"))),
                sink1);

        // then Second chunk is blocked in ready_to_process
        assertThat(getDependencyTrackingEntity(3, 0).getStatus(), is(ChunkProcessStatus.QUEUED_TO_PROCESS));
        assertThat(getDependencyTrackingEntity(3, 1).getStatus(), is(ChunkProcessStatus.READY_TO_PROCESS));

        // when chunk 3.0 is done..
        newJobSchedulerBean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                .setJobId(3).setChunkId(0)
                .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                .build()
        );

        // then chunk 3,1 is sent to processing
        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks(1);
        assertThat(getDependencyTrackingEntity(3, 1).getStatus(), is(ChunkProcessStatus.QUEUED_TO_PROCESS));

        assertThat(getDependencyTrackingEntity(3, 0).getStatus(), is(ChunkProcessStatus.QUEUED_TO_DELIVERY));

        // Chunk 3.0 sent to delivering

        // When chunk 3.1 is done processing
        newJobSchedulerBean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                .setJobId(3)
                .setChunkId(1)
                .appendItem(new ChunkItemBuilder().setData("ProcessdChunk").build())
                .build()
        );

        JPATestUtils.clearEntityManagerCache(entityManager);
        // Then chunk 3.1 i blocked by 3.0 in sink -- It waits for space in queue to Delivering
        assertThat(getDependencyTrackingEntity(3, 1).getStatus(), is(ChunkProcessStatus.READY_TO_DELIVER));


        // When 3.0 is returned from sink
        newJobSchedulerBean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                .setJobId(3)
                .setChunkId(0)
                .appendItem(new ChunkItemBuilder().setData("Processed Chunk").build())
                .build());

        // Then chunk 3.1 is released to Sink
        assertThat(getDependencyTrackingEntity(3, 1).getStatus(), is(ChunkProcessStatus.QUEUED_TO_DELIVERY));

    }

    Set<String> makeSet(String... s) {
        Set<String> res = new HashSet<>();
        Collections.addAll(res, s);
        return res;
    }

    public void runSqlFromResource(String resourceName) throws IOException, URISyntaxException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        String sql = JPATestUtils.readResource(this, resourceName);
        utx.begin();
        entityManager.joinTransaction();


        Query q = entityManager.createNativeQuery(sql);
        q.executeUpdate();
        utx.commit();

    }

    private DependencyTrackingEntity getDependencyTrackingEntity(int jobId, int chunkId) throws NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        JPATestUtils.clearEntityManagerCache(entityManager);
        utx.begin();
        entityManager.joinTransaction();
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(jobId, chunkId));
        assertThat(dependencyTrackingEntity, is(notNullValue()));
        entityManager.refresh(dependencyTrackingEntity);
        utx.commit();
        return dependencyTrackingEntity;
    }


    private void dbSingleUpdate(String update) throws Exception {
        utx.begin();
        entityManager.joinTransaction();

        entityManager.createNativeQuery(update).executeUpdate();
        utx.commit();
    }

}