package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.JobSpecification;
import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter.XML;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.jms.JmsQueueBean;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.TestFileStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.JSONBException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans10.BeansDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.NamingException;
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

/**
 * Created by ja7 on 11-04-16.
 * Arquillian Test for pgJobStore.
 * <p>
 * To Run from Intellij use arquillian Plugin JBoss Arquillian Support
 * - Manual Container Configuration -
 * dk.dbc.arquillian.container : arquillian-glassfish-remote-3.1
 */
@SuppressWarnings("JavaDoc")
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PgJobStoreArquillianIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreArquillianIT.class);

    @EJB PgJobStore pgJobStore;
    @EJB PgJobStoreRepository pgJobStoreRepository;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Resource(lookup = "jdbc/dataio/jobstore")
    DataSource dataSource;

    @EJB
    SinkMessageProducerBean sinkMessageProducerBean;


    @Inject
    UserTransaction utx;

    @Inject 
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @Inject
    JmsQueueBean jmsQueueBean;

    @Resource(name = "processorJmsQueue")
    Queue processorQueue;
    @Resource(name="sinksJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue sinksQueue;

    @Resource
    private ConnectionFactory messageQueueConnectionFactory;



    @Before
    public void clearTestConsumers() throws Exception {
        LOGGER.info("Before Test cleanTestConsumers and stuff");
        TestJobProcessorMessageConsumerBean.reset();
        TestSinkMessageConsumerBean.reset();
        dbCleanUp();

        JobSchedulerBean.ForTesting_ResetPrSinkStatuses();

        TestFileStoreServiceConnector i=new TestFileStoreServiceConnector();
    }

    @Before
    public void setupTestConnector() {
        TestJobStoreConnection.initializeConnector("http://localhost:8080/jobstore-pgjobstore-test/");
    }
    

    @Before
    public void dbCleanUp() throws Exception {

        utx.begin();
        entityManager.joinTransaction();

        entityManager.createNativeQuery("DELETE FROM job").executeUpdate();
        utx.commit();

        //jmsQueueBean.emptyQueue( "jms/dataio/processor");
        jmsQueueBean.emptyQueue( processorQueue );
        jmsQueueBean.emptyQueue( sinksQueue );
    }

    public int purgeJmsQueue(Queue queue) {
        int numDeleted=0;
        try (JMSContext context = messageQueueConnectionFactory.createContext()) {
            try (final JMSConsumer consumer = context.createConsumer(this.processorQueue)) {
                Message message;
                do {
                    message = consumer.receiveNoWait(); // todo: we should probably add an option to receive with timeout as well
                    if (message != null) {
                        message.acknowledge();
                        numDeleted++;
                    }
                } while (message != null);
            }
        } catch (JMSException e) {
            throw new EJBException(e);
        }
        return numDeleted;
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
                    .createProperty().name("eclipselink.logging.file").value("../logs/eclipselink.log").up()
                    .createProperty().name("eclipselink.logging.level").value("FINEST").up()
                    .createProperty().name("eclipselink.logging.logger").value("JavaLogger").up()
                    .up() // update Properties */
                    .up(); // update PersistenceUnit

            WebArchive war = ShrinkWrap.create(WebArchive.class, "jobstore-pgjobstore-test.war")
                    .addPackages(true, "dk/dbc/dataio/jobstore/service/entity", "dk/dbc/dataio/jobstore/service/digest",
                            "dk/dbc/dataio/jobstore/service/cdi", "dk/dbc/dataio/jobstore/service/param",
                            "dk/dbc/dataio/jobstore/service/partitioner", "dk/dbc/dataio/jobstore/service/util"
                            ,"dk/dbc/dataio/jobstore/service/rs"
                            //,"dk/dbc/dataio/jobstore/service/ejb"

                    )

                    
                    .addClasses(PgJobStoreRepository.class, RepositoryBase.class, StartupDBMigrator.class)
                    .addClasses(JobProcessorMessageProducerBean.class)


                    .addClasses(SinkMessageProducerBean.class)
                    .addClasses(JobSchedulerBean.class, JobSchedulerTransactionsBean.class,
                            JobSchedulerBulkSubmitterBean.class, JobSchedulerPrSinkQueueStatuses.class )

                    .addClasses(JobsBean.class)
                    .addClasses(PgJobStore.class , JobQueueRepository.class , JobNotificationRepository.class)
                    // Added to be able to reuse /rs classes"
                    .addClasses(JobSchedulerRestBean.class, NotificationsBean.class )
                    
                    .addClasses(FileStoreServiceConnectorBean.class, FlowStoreServiceConnectorBean.class )

                    .addClasses(TestJobProcessorMessageConsumerBean.class)
                    .addClasses(TestSinkMessageConsumerBean.class)
                    .addClass(TestJobSchedulerConfigOverWrite.class)

                    .addClasses( TestFileStoreServiceConnector.class, TestFileStoreServiceConnectorBean.class)
                    .addClass( TestJobStoreConnection.class)
                    ;

            // Add Maven Dependencies  // .workOffline fails med  mvnLocal ( .m2 i projectHome
            //File[] files = Maven.configureResolver().workOffline().withMavenCentralRepo(true).loadPomFromFile("pom.xml")
            File[] files = Maven.configureResolver().workOffline().withMavenCentralRepo(true).loadPomFromFile("pom.xml")
            //File[] files = Maven.configureResolver().withMavenCentralRepo(true).loadPomFromFile("pom.xml")
                    .importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
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

            // Add alternative to beans.xml
            war.addAsWebInfResource(new StringAsset(
                    Descriptors.importAs(BeansDescriptor.class)
                                        .fromFile("src/main/webapp/WEB-INF/beans.xml")
                                            .getOrCreateAlternatives()
                                                .clazz(TestFileStoreServiceConnectorBean.class.getCanonicalName())
                                            .up()
                            . exportAsString()),"beans.xml");

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


    @Test
    public void partitionJob() throws Exception {

        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning=createTestJobEntity( SinkContent.SinkType.DUMMY );
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();

        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), entityManager, XML);
        JobInfoSnapshot JobInfo=pgJobStore.partition( partitioningParam );
        utx.commit();

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("one chunk for Processing", 1);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 1);

        JobEntity jobPostPartitioning=getJobEntity(JobInfo.getJobId());
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(1));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(4));
    }

    @Test
    public void partitionTickleOKJob() throws Exception {

        utx.begin();
        entityManager.joinTransaction();
        JobEntity jobPrePartitioning=createTestJobEntity( SinkContent.SinkType.TICKLE);
        utx.commit();

        utx.begin();
        entityManager.joinTransaction();

        PartitioningParam partitioningParam = new PartitioningParam(jobPrePartitioning, fileStoreServiceConnectorBean.getConnector(), entityManager, XML);
        JobInfoSnapshot JobInfo=pgJobStore.partition( partitioningParam );
        utx.commit();

        TestJobProcessorMessageConsumerBean.waitForProcessingOfChunks("one chunk for Processing", 1);
        TestSinkMessageConsumerBean.waitForDeliveringOfChunks("one chunk delivering ", 2);

        JobEntity jobPostPartitioning=getJobEntity(JobInfo.getJobId());
        assertThat("Job is done", jobPostPartitioning.getNumberOfChunks(), is(2));
        assertThat("Job is done", jobPostPartitioning.getTimeOfCreation(), is(notNullValue()));
        assertThat("Job is done", jobPostPartitioning.getNumberOfItems(), is(5));

        ChunkEntity terminationChunk= getChunkEntity(JobInfo.getJobId(), 1);
        assertThat("Termination ChunkId", terminationChunk.getNumberOfItems(), is((short)1));
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

    
    private JobEntity createTestJobEntity(SinkContent.SinkType sinkType) throws JSONBException {

        JobEntity job=new JobEntity();
        job.setSpecification( new JobSpecification("XML","XML",
                "UTF8","noware",
                77,
                "","","",
                "urn:dataio-fs:datafile", JobSpecification.Type.TEST));
        job.setEoj(true);
        job.setPartNumber(0);
        job.setState( new State());

        job.setFlowStoreReferences(new FlowStoreReferences()
                .withReference(FlowStoreReferences.Elements.SINK ,new FlowStoreReference(1, 2, "SinkName")  )
        );

        final Sink sink = new SinkBuilder().setContent(new SinkContentBuilder().setSinkType( sinkType ).build()).build();
    
        final SinkCacheEntity sinkCacheEntity = pgJobStoreRepository.cacheSink(pgJobStoreRepository.jsonbContext.marshall(sink));

        entityManager.persist(sinkCacheEntity);
        job.setCachedSink( sinkCacheEntity);
        entityManager.persist(job);

        return job;
    }
    private Set<String> makeSet(String... s) {
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
