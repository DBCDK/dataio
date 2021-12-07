package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.jms.JmsQueueBean;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.TestFileStoreServiceConnector;
import dk.dbc.dataio.flowstore.service.connector.ejb.TestFlowStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
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
import javax.jms.Queue;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import java.io.File;
import java.util.Date;

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter.XML;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by ja7 on 11-04-16.
 * Arquillian Test for pgJobStore.
 * <p>
 * To Run from Intellij use arquillian Plugin JBoss Arquillian Support
 * - Manual Container Configuration -
 * dk.dbc.arquillian.container : arquillian-glassfish-remote-3.1
 */
@SuppressWarnings("JavaDoc")
//@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PgJobStoreArquillianIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreArquillianIT.class);

    // Todo: Fix arquillian tests. Dies with:
    //  SEVERE: WebModule[/jobstore-pgjobstore-test]StandardWrapper.Throwable
    //  java.lang.NoSuchMethodError: org.glassfish.jersey.server.ServerExecutorProvidersConfigurator.registerExecutors(Lo
    //
    //
    /*
    @EJB PgJobStore pgJobStore;
    @EJB PgJobStoreRepository pgJobStoreRepository;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Resource(lookup = "jdbc/dataio/jobstore")
    DataSource dataSource;
    
    @Inject
    UserTransaction utx;

    @Inject 
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @Inject
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @Inject
    JmsQueueBean jmsQueueBean;

    @Resource(lookup = "jms/dataio/processor")
    Queue processorQueue;
    @Resource(lookup = "jms/dataio/sinks")
    Queue sinksQueue;
    

    @Before
    public void clearTestConsumers() throws Exception {
        LOGGER.info("Before Test cleanTestConsumers and stuff");
        TestJobProcessorMessageConsumerBean.reset();
        TestSinkMessageConsumerBean.reset();
        dbCleanUp();

        JobSchedulerBean.resetAllSinkStatuses();

        TestFileStoreServiceConnector.resetTestData();
        
        TestFileStoreServiceConnector.updateFileContent("datafile", "<x><r>record1</r><r>record2</r><r>record3</r><r>record4</r></x>");

        StringBuffer brokenFile = new StringBuffer();
        brokenFile.append("<x>");
        for( int i=0; i<15 ; ++i) { brokenFile.append("<r>").append(i).append("</r>"); }
        brokenFile.append("</t>");
        TestFileStoreServiceConnector.updateFileContent("broken", brokenFile.toString());

        

        StringBuffer datafile30items = new StringBuffer();
        datafile30items.append("<x>");
        for( int i=0; i<30 ; ++i) { datafile30items.append("<r>").append(i).append("</r>"); }
        datafile30items.append("</x>");
        TestFileStoreServiceConnector.updateFileContent("datafile30items", datafile30items.toString());
        TestJobStoreConnection.initializeConnector("http://localhost:" + System.getProperty("container.http.port") + "/jobstore-pgjobstore-test/");
    }


    public void dbCleanUp() throws Exception {

        utx.begin();
        entityManager.joinTransaction();

        entityManager.createNativeQuery("DELETE FROM job").executeUpdate();

        utx.commit();
        JPATestUtils.clearEntityManagerCache( entityManager );
        
        jmsQueueBean.emptyQueue( processorQueue );
        jmsQueueBean.emptyQueue( sinksQueue );
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
                    .createProperty().name("eclipselink.logging.level").value("FINEST").up()
                    .createProperty().name("eclipselink.logging.logger").value("JavaLogger").up()
                    .up() // update Properties
                    .up(); // update PersistenceUnit

            WebArchive war = ShrinkWrap.create(WebArchive.class, "jobstore-pgjobstore-test.war")
                .addPackages(true, Filters.exclude(".*(Test|IT)(\\$.*)?\\.class"), "dk/dbc/dataio/jobstore")
                // add ejb-jar.xml for DmqMessageConsumerBean
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/ejb-jar.xml"), "ejb-jar.xml")
                .addClasses(TestFileStoreServiceConnector.class)
                .addClasses(TestFlowStoreServiceConnector.class);

            File[] deps = Maven.configureResolver().workOffline().loadPomFromFile("pom.xml")
                .importRuntimeAndTestDependencies()
                .resolve().withTransitivity().asFile();
            war.addAsLibraries(deps);

            // https://developer.jboss.org/wiki/HowDoIAddAllWebResourcesToTheShrinkWrapArchive
            // https://issues.jboss.org/browse/SHRINKWRAP-247?_sscc=t
            war.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class)
                .importDirectory("src/main/resources/db/migration")
                .as(GenericArchive.class), "WEB-INF/classes/db/migration", Filters.includeAll());

            war.addAsResource(new StringAsset(persistence.exportAsString()), "META-INF/persistence.xml");

            war.addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"), "beans.xml");

            war.addAsWebInfResource(new File("src/test/resources/arquillian_logback.xml"), "classes/logback-test.xml");
            war.addAsResource(new File("src/test/resources/", "JobSchedulerBeanArquillianIT_findWaitForChunks.sql"));

            //LOGGER.info("war {}", war.toString(true));

            return war;
        } catch (Exception e) {
            LOGGER.error("War building failed", e);
            throw e;
        }
    }


    @Test
    public void partitionJob() throws Exception {

        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning=createTestJobEntity( SinkContent.SinkType.DUMMY, "urn:dataio-fs:datafile", "UTF8");
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();

        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(), entityManager, XML);
        Partitioning partitioning = pgJobStore.partition(partitioningParam);
        JobInfoSnapshot jobInfo = partitioning.getJobInfoSnapshot();
        utx.commit();

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("one chunk for Processing", 1);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 1);

        JobEntity jobPostPartitioning=getJobEntity(jobInfo.getJobId());
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(1));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(4));
    }

    @Test
    public void previewJob() throws Exception {

        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning = createTestJobEntity(SinkContent.SinkType.DUMMY, "urn:dataio-fs:datafile", "UTF8");
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();

        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(), entityManager, XML);
        Partitioning partitioning = pgJobStore.preview(partitioningParam);
        JobInfoSnapshot jobInfo = partitioning.getJobInfoSnapshot();
        utx.commit();

        JobEntity jobPostPartitioning=getJobEntity(jobInfo.getJobId());
        assertThat("Number of chunks", jobPostPartitioning.getNumberOfChunks(), is(0));
        assertThat("Number of items", jobPostPartitioning.getNumberOfItems(), is(4));
        assertThat("Time of creation", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Time of completion", jobPostPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("Diagnostics.size", jobPostPartitioning.getState().getDiagnostics().size(), is(0));
    }

    @Test
    public void partitionTickleOKJob() throws Exception {

        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning=createTestJobEntity( SinkContent.SinkType.TICKLE, "urn:dataio-fs:datafile", "UTF8");
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();

        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(), entityManager, XML);
        Partitioning partitioning = pgJobStore.partition(partitioningParam);
        JobInfoSnapshot jobInfo = partitioning.getJobInfoSnapshot();
        utx.commit();

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("one chunk for Processing", 1);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 2);

        JobEntity jobPostPartitioning=getJobEntity(jobInfo.getJobId());
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(2));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(5));

        ChunkEntity terminationChunk= getChunkEntity(jobInfo.getJobId(), 1);
        assertThat("Termination ChunkId", terminationChunk.getNumberOfItems(), is((short)1));

        Chunk chunk = pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, jobInfo.getJobId(), 1);

        assertThat("Termination Chunk only contains one item ", chunk.getItems().size(), is(1));
        assertThat("Last chunk is Termination Chunk", chunk.isTerminationChunk(), is(true) );
        ChunkItem terminationItem = chunk.getItems().get(0);
        assertThat("Termination item", terminationItem.getType().get(0),is(ChunkItem.Type.JOB_END) );
        assertThat("Termination item status", terminationItem.getStatus(), is(ChunkItem.Status.SUCCESS ));


    }



    @Test
    public void partitionTickleBrokenDatafileJob() throws Exception {

        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning=createTestJobEntity( SinkContent.SinkType.TICKLE, "urn:dataio-fs:broken", "UTF8");
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();

        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(), entityManager, XML);
        Partitioning partitioning = pgJobStore.partition(partitioningParam);
        JobInfoSnapshot jobInfo = partitioning.getJobInfoSnapshot();
        utx.commit();

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("one chunk for Processing", 1);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 2);

        JobEntity jobPostPartitioning=getJobEntity(jobInfo.getJobId());
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(3));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(16));

        ChunkEntity terminationChunk = getChunkEntity(jobInfo.getJobId(), 2);
        assertThat("Termination ChunkId", terminationChunk.getNumberOfItems(), is((short)1));
        
        Chunk chunk = pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, jobInfo.getJobId(), 2);

        assertThat("Termination Chunk only contains one item ", chunk.getItems().size(), is(1));
        assertThat("Last chunk is Termination Chunk", chunk.isTerminationChunk(), is(true) );
        ChunkItem terminationItem = chunk.getItems().get(0);
        assertThat("Termination item", terminationItem.getType().get(0),is(ChunkItem.Type.JOB_END) );
        assertThat("Termination item status", terminationItem.getStatus(), is(ChunkItem.Status.FAILURE ));
        
    }


    @Test
    public void partitionTickleTotalFileNotRead() throws Exception {

        TestFileStoreServiceConnector.updateFileContentLength("datafile", 9999L);
        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning=createTestJobEntity( SinkContent.SinkType.TICKLE, "urn:dataio-fs:datafile", "UTF8");
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();

        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(), entityManager, XML);
        Partitioning partitioning = pgJobStore.partition(partitioningParam);
        JobInfoSnapshot jobInfo = partitioning.getJobInfoSnapshot();
        utx.commit();

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("one chunk for Processing", 1);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 1);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 1);

        JobEntity jobPostPartitioning=getJobEntity(jobInfo.getJobId());
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(2));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(5));

        ChunkEntity terminationChunk= getChunkEntity(jobInfo.getJobId(), 1);
        assertThat("Termination ChunkId", terminationChunk.getNumberOfItems(), is((short)1));

        Chunk chunk = pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, jobInfo.getJobId(), 1);

        assertThat("Termination Chunk only contains one item ", chunk.getItems().size(), is(1));
        assertThat("Last chunk is Termination Chunk", chunk.isTerminationChunk(), is(true) );
        ChunkItem terminationItem = chunk.getItems().get(0);
        assertThat("Termination item", terminationItem.getType().get(0),is(ChunkItem.Type.JOB_END) );
        assertThat("Termination item status", terminationItem.getStatus(), is(ChunkItem.Status.FAILURE));


    }



    @Test
    public void partitionTickleWrongCharSet() throws Exception {

        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning=createTestJobEntity( SinkContent.SinkType.TICKLE, "urn:dataio-fs:datafile", "LATIN-1");
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();
        
        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(), entityManager, XML);
        Partitioning partitioning = pgJobStore.partition(partitioningParam);
        JobInfoSnapshot jobInfo = partitioning.getJobInfoSnapshot();
        utx.commit();


        JobEntity jobPostPartitioning=getJobEntity(jobInfo.getJobId());
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(1));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(1));
        assertThat("Job is failed", jobPostPartitioning.hasFatalDiagnostics(), is(true));

    }




    public JobEntity getJobEntity(int jobId) throws Exception{
        utx.begin();
        entityManager.joinTransaction();
        JobEntity job = entityManager.find(JobEntity.class, jobId);
        entityManager.refresh(job);
        utx.commit();
        return job;
    }

    public ChunkEntity getChunkEntity(int jobId, int chunkId) throws Exception{
        utx.begin();
        entityManager.joinTransaction();
        ChunkEntity chunk = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(chunkId, jobId));
        entityManager.refresh(chunk);
        utx.commit();
        return chunk;
    }


    @Test
    public void resumePartitioningSkipFirstChunkOKJob() throws Exception {
        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning = createTestJobEntity(SinkContent.SinkType.DUMMY, "urn:dataio-fs:datafile30items", "UTF8");
        jobPrePartitioning.setNumberOfChunks(1); // Mark first chunk Already Done.. Ignore Chunk from test
        jobPrePartitioning.setNumberOfItems(10);
        jobPrePartitioning.getState().getPhase(State.Phase.PARTITIONING).withSucceeded(10).withBeginDate(new Date());
        jobPrePartitioning.getState().getPhase(State.Phase.PROCESSING).withSucceeded(10).withBeginDate(new Date());
        jobPrePartitioning.getState().getPhase(State.Phase.DELIVERING).withSucceeded(10).withBeginDate(new Date());

        JobQueueEntity jobQueueEntity=new JobQueueEntity().withJob(jobPrePartitioning).withSinkId(1).withState(JobQueueEntity.State.WAITING).withTypeOfDataPartitioner(XML);
        entityManager.persist(jobQueueEntity);
        utx.commit();

        int jobId = jobPrePartitioning.getId();
        
        pgJobStore.partitionNextJobForSinkIfAvailable(new SinkBuilder().setId(1).build() );

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("one chunk for Processing", 2);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 2);

        JobEntity jobPostPartitioning=getJobEntity(jobId);
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(3));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(30));
        assertThat( TestJobProcessorMessageConsumerBean.getChunksReceivedCount(), is(2));
        assertThat( TestSinkMessageConsumerBean.getChunksReceivedCount(), is(2));

        Chunk chunk = pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, jobId, 2);
        assertThat("Last Chunk only contains one item ", chunk.getItems().size(), is(10));
        ChunkItem item = chunk.getItems().get(0);
        assertThat("Last Item is Failure", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Last item content", new String(item.getData()), is("<?xml version='1.0'?><x><r>20</r></x>"));
    }

    @Test
    public void fatalDiagnosticInPartitioningParam() throws Exception {
        utx.begin();
        entityManager.joinTransaction();
        final JobEntity jobEntity = createTestJobEntity(SinkContent.SinkType.DUMMY, "urn:dataio-fs:404", "UTF8");
        final JobQueueEntity jobQueueEntity = new JobQueueEntity()
                .withJob(jobEntity)
                .withSinkId(1)
                .withState(JobQueueEntity.State.WAITING)
                .withTypeOfDataPartitioner(XML);
        entityManager.persist(jobQueueEntity);
        entityManager.flush();
        entityManager.refresh(jobQueueEntity);
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();
        pgJobStore.partitionNextJobForSinkIfAvailable(new SinkBuilder().setId(jobQueueEntity.getSinkId()).build());
        Thread.sleep(500);
        utx.commit();

        final JobEntity jobEntityAfterPartitioning = getJobEntity(jobEntity.getId());
        assertThat("job timeOfCreation", jobEntityAfterPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("job timeOfCompletion", jobEntityAfterPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("job hasFatalError", jobEntityAfterPartitioning.hasFatalError(), is(true));
        assertThat("job is removed from queue", entityManager.find(JobQueueEntity.class, jobQueueEntity.getId()), is(nullValue()));
    }

    @Test
    public void unexpectedExceptionFromPartitioning() throws Exception {
        utx.begin();
        entityManager.joinTransaction();
        final JobEntity jobEntity = createTestJobEntity(SinkContent.SinkType.DUMMY, "urn:dataio-fs:throws-unexpected", "UTF8");
        final JobQueueEntity jobQueueEntity = new JobQueueEntity()
                .withJob(jobEntity)
                .withSinkId(1)
                .withState(JobQueueEntity.State.WAITING)
                .withTypeOfDataPartitioner(XML);
        entityManager.persist(jobQueueEntity);
        entityManager.flush();
        entityManager.refresh(jobQueueEntity);
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();
        pgJobStore.partitionNextJobForSinkIfAvailable(new SinkBuilder().setId(jobQueueEntity.getSinkId()).build());
        Thread.sleep(500);
        utx.commit();

        final JobEntity jobEntityAfterPartitioning = getJobEntity(jobEntity.getId());
        assertThat("job timeOfCreation", jobEntityAfterPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("job timeOfCompletion", jobEntityAfterPartitioning.getTimeOfCompletion(), is(notNullValue()));
        assertThat("job hasFatalError", jobEntityAfterPartitioning.hasFatalError(), is(true));
        assertThat("job is removed from queue", entityManager.find(JobQueueEntity.class, jobQueueEntity.getId()), is(nullValue()));
    }

    private JobEntity createTestJobEntity(SinkContent.SinkType sinkType, String dataFile, String charset) throws JSONBException {

        JobEntity job = new JobEntity();
        job.setSpecification(new JobSpecification()
                .withPackaging("XML")
                .withFormat("XML")
                .withCharset(charset)
                .withDestination("noware")
                .withSubmitterId(77)
                .withMailForNotificationAboutVerification("")
                .withMailForNotificationAboutProcessing("")
                .withResultmailInitials("")
                .withDataFile(dataFile)
                .withType(JobSpecification.Type.TEST));

        job.setEoj(true);
        job.setPartNumber(0);
        job.setState( new State());

        job.setFlowStoreReferences(new FlowStoreReferences()
                .withReference(FlowStoreReferences.Elements.SINK ,new FlowStoreReference(1, 2, "SinkName"))
                .withReference(FlowStoreReferences.Elements.FLOW, new FlowStoreReference(1, 1, "FlowName"))
                .withReference(FlowStoreReferences.Elements.SUBMITTER, new FlowStoreReference(1, 1, "SubmitterName"))
        );

        final Sink sink = new SinkBuilder().setContent(new SinkContentBuilder().setSinkType( sinkType ).build()).build();
        final SinkCacheEntity sinkCacheEntity = pgJobStoreRepository.cacheSink(pgJobStoreRepository.jsonbContext.marshall(sink));
        entityManager.persist(sinkCacheEntity);
        job.setCachedSink( sinkCacheEntity);
        entityManager.persist(job);

        return job;
    }*/
}
