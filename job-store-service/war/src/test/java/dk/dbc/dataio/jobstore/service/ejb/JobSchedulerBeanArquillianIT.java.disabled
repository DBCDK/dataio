package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.logstore.service.connector.ejb.LogStoreServiceConnectorBean;
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
import javax.persistence.LockModeType;
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
import java.util.concurrent.TimeUnit;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by ja7 on 11-04-16.
 * Arquillian Test for NewJobSchedulerBeanArquillian.
 * <p>
 * To Run from Intellij use arquillian Plugin JBoss Arquillian Support
 * - Manual Container Configuration -
 * dk.dbc.arquillian.container : arquillian-glassfish-remote-3.1
 */
@SuppressWarnings("JavaDoc")
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JobSchedulerBeanArquillianIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBeanArquillianIT.class);

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @EJB
    JobSchedulerBulkSubmitterBean bulkSubmitterBean;

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
        LOGGER.info("Before Test cleanTestConsumers and stuff");
        TestJobProcessorMessageConsumerBean.reset();
        TestSinkMessageConsumerBean.reset();
        dbAndQueuesCleanup();

        JobSchedulerBean.resetAllSinkStatuses();
        TestJobStoreConnection.resetConnector(
        );
    }


    @After
    public void dbAndQueuesCleanup() throws Exception {
        utx.begin();
        entityManager.joinTransaction();

        entityManager.createNativeQuery("DELETE FROM job").executeUpdate();
        utx.commit();
    }

    @SuppressWarnings("ConstantConditions")
    @Deployment
    public static WebArchive createDeployment() {
        LOGGER.warn("in public static WebArchive createDeployment() ");
        try {

            // Change eclipse Link log level and destination
            PersistenceDescriptor persistence = Descriptors.importAs(PersistenceDescriptor.class)
                    .fromFile("src/main/resources/META-INF/persistence.xml")
                    .getOrCreatePersistenceUnit().name("jobstorePU")
                    .getOrCreateProperties()
                    .createProperty().name("eclipselink.allow-zero-id").value("true").up()
                    .createProperty().name("eclipselink.logging.level").value("FINEST").up()
                    .createProperty().name("eclipselink.logging.logger").value("JavaLogger").up()
                    .up() // update Properties */
                    .up(); // update PersistenceUnit

            WebArchive war = ShrinkWrap.create(WebArchive.class, "jobstore-jobScheduler-test.war")
                    .addPackages(true, "dk/dbc/dataio/jobstore/service/entity",
                            "dk/dbc/dataio/jobstore/service/digest", "dk/dbc/dataio/jobstore/service/cdi",
                            "dk/dbc/dataio/jobstore/service/param", "dk/dbc/dataio/jobstore/service/partitioner",
                            "dk/dbc/dataio/jobstore/service/util", "dk/dbc/dataio/jobstore/service/dependencytracking",
                            "dk/dbc/dataio/sink/types")

                    .addClasses(PgJobStoreRepository.class, RepositoryBase.class, DatabaseMigrator.class)
                    .addClasses(JobProcessorMessageProducerBean.class)

                    .addClasses(SinkMessageProducerBean.class)
                    .addClasses(JobSchedulerBean.class, JobSchedulerTransactionsBean.class,
                            JobSchedulerBulkSubmitterBean.class, JobSchedulerSinkStatus.class )

                    .addClasses( JobsBean.class, JobNotificationRepository.class, PgJobStore.class)
                    .addClasses(LogStoreServiceConnectorBean.class)
                    .addClasses(TestJobProcessorMessageConsumerBean.class)
                    .addClasses(TestSinkMessageConsumerBean.class)
                    .addClass(TestJobSchedulerConfigOverWrite.class)
                    .addClass( TestJobStoreConnection.class)

                    ;

            // Add Maven Dependencies  // .workOffline fails med  mvnLocal ( .m2 i projectHome
            File[] files = Maven.configureResolver().workOffline().withMavenCentralRepo(false).loadPomFromFile("pom.xml")
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
            war.addAsWebInfResource(new File("src/test/resources/arquillian_logback.xml"), "classes/logback-test.xml");
            war.addAsResource(new File("src/test/resources/", "JobSchedulerBeanArquillianIT_findWaitForChunks.sql"));

            return war;
        } catch (Exception e) {
            LOGGER.error("War building failed", e);
            throw e;
        }
    }

    /*
      Test 2 Chunks (3,0) Is gowning through the Stages
             Chunks (3,1) Is blocked by (3,0) and released for Delivering when 3.0 is done.
     */
    @Test
    public void scheduleChunkSimple() throws Exception {
        runSqlFromResource("JobSchedulerBeanArquillianIT_findWaitForChunks.sql");
        // Given

        final JobEntity job = new JobEntity();
        job.setSpecification(new JobSpecification().withSubmitterId(123456));
        job.setPriority(Priority.NORMAL);
        job.setCachedSink(SinkCacheEntity.create(new SinkBuilder().setId(1).build()));

        // when
        jobSchedulerBean.scheduleChunk(new ChunkEntity()
                        .withJobId(3)
                        .withChunkId(0)
                        .withSequenceAnalysisData(new SequenceAnalysisData(makeSet("f1"))), job);

        // Then
        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("", 1);

        assertThat(TestJobProcessorMessageConsumerBean.getChunksReceivedCount(), is(1));

        DependencyTrackingEntity dependencyTrackingEntity = getDependencyTrackingEntity(3, 0);

        assertThat(dependencyTrackingEntity, notNullValue());
        assertThat(dependencyTrackingEntity.getStatus(), is(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING));


        // Given a chunk Returned from Processing it moves to Sink
        Chunk chunk1 = new ChunkBuilder(PROCESSED)
                .setJobId(3)
                .setChunkId(0)
                .appendItem(new ChunkItemBuilder().setData("ProcessedChunk").build())
                .build();
        // When
        jobSchedulerBean.chunkProcessingDone(chunk1);


        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("",1);
        // Then

        dependencyTrackingEntity = getDependencyTrackingEntity(3, 0);

        assertThat(dependencyTrackingEntity, notNullValue());
        assertThat(dependencyTrackingEntity.getStatus(), is(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY));

        jobSchedulerBean.scheduleChunk(new ChunkEntity()
                        .withJobId(3)
                        .withChunkId(1)
                        .withSequenceAnalysisData(new SequenceAnalysisData(makeSet("f1"))), job);
        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("", 1);

        jobSchedulerBean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                .setJobId(3).setChunkId(1)
                .appendItem(new ChunkItemBuilder().setData("Processed Chunk").build())
                .build()
        );


        assertThat(getDependencyTrackingEntity(3, 1).getStatus(), is( ChunkSchedulingStatus.BLOCKED));


        // when chunk returns from Delivering
        jobSchedulerBean.chunkDeliveringDone(chunk1);

        // Then
        JPATestUtils.clearEntityManagerCache(entityManager);
        //Check no chunks is waiting for chunk(3,0)

        String keyAsJson = ConverterJSONBContext.getInstance().marshall(new DependencyTrackingEntity.Key(3, 0));
        Query query = entityManager.createNativeQuery("select jobid, chunkid from dependencyTracking where waitingon @> '[" + keyAsJson + "]'", DependencyTrackingEntity.KEY_RESULT);
        assertThat(query.getResultList().size(), is(0));

        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("",1);

        DependencyTrackingEntity dep = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(3, 0));
        assertThat(dep, is(nullValue()));

        // Test that chunk 3.1 is ready for sink.
        dep = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(3, 1));
        assertThat(dep.getStatus(), is(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY));

        // When 3.0 is returned from sink
        jobSchedulerBean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                .setJobId(3)
                .setChunkId(1)
                .appendItem(new ChunkItemBuilder().setData("Processed Chunk").build())
                .build());
    }


    /*
      The Tests use a queue limit on 10 chunks. And tests by submitting 16 chunks.
      and forces bulk handle 3 chunks one at a time.
     */
    @Test
    public void RateLimits() throws Exception {

        runSqlFromResource("JobSchedulerBeanArquillianIT_findWaitForChunks.sql");

        // when there is only space for 10 chunks to sink 1
        final JobEntity job = new JobEntity();
        job.setSpecification(new JobSpecification().withSubmitterId(123456));
        job.setPriority(Priority.NORMAL);
        job.setCachedSink(SinkCacheEntity.create(new SinkBuilder().setId(1).build()));

        JobSchedulerSinkStatus sinkStatus = JobSchedulerBean.getSinkStatus(job.getCachedSink().getSink().getId());

        assertThat("Processing is back to directMode", sinkStatus.processingStatus.isDirectSubmitMode(),is(true));
        assertThat("Processing is back to directMode", sinkStatus.deliveringStatus.isDirectSubmitMode(),is(true));

        // submit 3.[0-9]
        for (int i=0; i<=9; ++i) {
            jobSchedulerBean.scheduleChunk(new ChunkEntity()
                            .withJobId(3)
                            .withChunkId(i)
                            .withSequenceAnalysisData(new SequenceAnalysisData(makeSet("f"+i))), job);
        }
        // Chunk 3.[0-9] must take last slot before chunk is delayed.
        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("", 10);



        for (int i=10; i <= 15; ++i ) {
            jobSchedulerBean.scheduleChunk(new ChunkEntity()
                            .withJobId(3)
                            .withChunkId(i)
                            .withSequenceAnalysisData(new SequenceAnalysisData(makeSet("f"+i))), job);
        }

        assertThat("Processing is back to directMode", sinkStatus.processingStatus.isDirectSubmitMode(),is(false));
        assertThat("Processing is back to directMode", sinkStatus.deliveringStatus.isDirectSubmitMode(),is(true));


        // then Second chunk is blocked in ready_to_process
        for( int i=0 ; i<=9 ; ++i) {
            assertThat("Check QUEUED to PROCESS for chunk "+i, getDependencyTrackingEntity(3, i).getStatus(), is(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING));
        }
        for( int i=10; i <= 15 ; ++i ) {
            assertThat("Check READY_FOR_PROCESSING for chunk "+i, getDependencyTrackingEntity(3, i).getStatus(), is(ChunkSchedulingStatus.READY_FOR_PROCESSING));
        }

        // when chunk 3.[0-2]s is done.. only 10-12 shut be submitted to processing once.
        for( int i=0; i<=2; ++i) {
            jobSchedulerBean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(i)
                    .appendItem(new ChunkItemBuilder().setData("Processed Chunk").build())
                    .build()
            );
        }

        for( int i=10; i<=12; ++i) {
            TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("chunk "+i,1);
            assertThat("Check QUEUED to PROCESS for chunk " + i, getDependencyTrackingEntity(3, i).getStatus(), is(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING));
            assertThat("Processing status after chunk "+i, sinkStatus.processingStatus.isDirectSubmitMode(),is(false));
        }

        for( int i=13; i <= 15 ; ++i ) {
            assertThat("Check READY_FOR_PROCESSING for chunk "+i, getDependencyTrackingEntity(3, i).getStatus(), is(ChunkSchedulingStatus.READY_FOR_PROCESSING));
        }


        assertThat( TestJobProcessorMessageConsumerBean.getChunksReceivedCount(), is(13));
        for(int i=0 ; i<=2 ; ++i ) {
            bulkSubmitterBean.bulkScheduleChunksForProcessing();
            LOGGER.info("UnitTest Waiting one Second");
            TimeUnit.SECONDS.sleep(1);
            assertThat( TestJobProcessorMessageConsumerBean.getChunksReceivedCount(), is(13));
        }

        for( int i=3; i<=9; ++i) {
            jobSchedulerBean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(i)
                    .appendItem(new ChunkItemBuilder().setData("Processed Chunk").build())
                    .build()
            );
        }

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("chunks 13-15 ",3);

        // Mark last chunks done for chunks 10-12
        for( int i=10; i<=15; ++i) {
            jobSchedulerBean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(i)
                    .appendItem(new ChunkItemBuilder().setData("Processed Chunk").build())
                    .build()
            );
        }

        assertThat( TestJobProcessorMessageConsumerBean.getChunksReceivedCount(), is(16));



        assertThat("DeliveringStatus after 16 chunks went tru processing ", sinkStatus.deliveringStatus.getMode(),is(JobSchedulerBean.QueueSubmitMode.BULK));
        waitForDirectSubmitModeIs( sinkStatus.processingStatus, JobSchedulerBean.QueueSubmitMode.DIRECT);
        assertThat("Processing is back to directMode", sinkStatus.processingStatus.getMode(),is(JobSchedulerBean.QueueSubmitMode.DIRECT));

        LOGGER.info("Processing Testing DONE ");

        //
        // Done Testing processing Queue.. 16 chunks passed on for Delivery
        //
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("0-9", 10);
        for( int i=0; i<=9; ++i) {
            assertThat("Check QUEUED_FOR_DELIVERY for chunk "+i, getDependencyTrackingEntity(3, i).getStatus(), is(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY));
        }
        for( int i=10; i<=15; ++i ) {
            assertThat("Check READY_FOR_DELIVERY for chunk "+i, getDependencyTrackingEntity(3, i).getStatus(), is(ChunkSchedulingStatus.READY_FOR_DELIVERY));
        }

        assertThat( TestSinkMessageConsumerBean.getChunksReceivedCount(),is( 10));
        // Mark Chunks Done from delivering one by one
        // when chunk 3.[0-9] is done..
        for( int i=0; i<=2; ++i) {
            jobSchedulerBean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(i)
                    .appendItem(new ChunkItemBuilder().setData("Delivered Chunk").build())
                    .build()
            );
            int chunkExpected=10+i;
            if( chunkExpected <= 12 ) {
                TestSinkMessageConsumerBean.waitForDeliveringOfChunks(" chunk "+chunkExpected, 1);
                assertThat("Check QUEUED_FOR_DELIVERY for chunk " + chunkExpected, getDependencyTrackingEntity(3, chunkExpected).getStatus(), is(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY));
            }
        }

        assertThat( TestSinkMessageConsumerBean.getChunksReceivedCount(),is( 13 ));
        for(int i=0 ; i<=2 ; ++i ) {
            bulkSubmitterBean.bulkScheduleChunksForDelivering();
            LOGGER.info("UnitTest Waiting one Second");
            TimeUnit.SECONDS.sleep(1);
            assertThat( TestSinkMessageConsumerBean.getChunksReceivedCount(), is(13));
        }

        for( int i=3; i<=9; ++i) {
            jobSchedulerBean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(i)
                    .appendItem(new ChunkItemBuilder().setData("Delivered Chunk").build())
                    .build()
            );
        };

        TestSinkMessageConsumerBean.waitForDeliveringOfChunks(" chunk 13-15", 3);



        // Mark last chunks done for chunks 11-12
        for( int i=10; i<=12; ++i) {
            jobSchedulerBean.chunkDeliveringDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(i)
                    .appendItem(new ChunkItemBuilder().setData("Delivered Chunk").build())
                    .build()
            );
        }


        waitForDirectSubmitModeIs( sinkStatus.processingStatus, JobSchedulerBean.QueueSubmitMode.DIRECT);
        waitForDirectSubmitModeIs( sinkStatus.deliveringStatus, JobSchedulerBean.QueueSubmitMode.DIRECT);


        assertThat("Processing is back to directMode", sinkStatus.processingStatus.getMode(),is(JobSchedulerBean.QueueSubmitMode.DIRECT));
        assertThat("Delivering is back to directMode", sinkStatus.deliveringStatus.getMode(),is(JobSchedulerBean.QueueSubmitMode.DIRECT));

    }

    public void waitForDirectSubmitModeIs(JobSchedulerSinkStatus.QueueStatus queueStatus, JobSchedulerBean.QueueSubmitMode expected ) {
        for(int i=0; i<40 ; ++i) {
            if( queueStatus.getMode() == expected) return;
            try {
                TimeUnit.MILLISECONDS.sleep( 250 ); // 1/4 second
            } catch (InterruptedException e) {
            }
        }

    }

    private Set<String> makeSet(String... s) {
        Set<String> res = new HashSet<>();
        Collections.addAll(res, s);
        return res;
    }

    public void runSqlFromResource(String resourceName) throws IOException, URISyntaxException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        String sql = ResourceReader.getResourceAsString(this.getClass(), resourceName);
        utx.begin();
        entityManager.joinTransaction();


        Query q = entityManager.createNativeQuery(sql);
        q.executeUpdate();
        utx.commit();

    }

    @SuppressWarnings("SameParameterValue")
    private DependencyTrackingEntity getDependencyTrackingEntity(int jobId, int chunkId) throws NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        JPATestUtils.clearEntityManagerCache(entityManager);
        utx.begin();
        entityManager.joinTransaction();
        LOGGER.info("Test Checker entityManager.find( job={}, chunk={} ) ", jobId, chunkId );
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, new DependencyTrackingEntity.Key(jobId, chunkId), LockModeType.PESSIMISTIC_READ);
        assertThat(dependencyTrackingEntity, is(notNullValue()));
        entityManager.refresh(dependencyTrackingEntity);
        utx.commit();
        return dependencyTrackingEntity;
    }


}