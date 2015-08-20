package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobprocessor.ejb.SinkMessageProducerBean;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.postgresql.ds.PGSimpleDataSource;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class SinkIT {
    protected static final PGSimpleDataSource ES_INFLIGHT_DATASOURCE;
    protected static final OracleDataSource ES_DATASOURCE;
    protected static final String ES_INFLIGHT_DATABASE_NAME = "esinflight";
    protected static final String ES_RESOURCE_NAME = "jdbc/dataio/es";
    protected static final String ES_DATABASE_NAME;

    protected EntityManager esInFlightEntityManager;
    protected JMSContext jmsContext = mock(JMSContext.class);
    protected JSONBContext jsonbContext = new JSONBContext();
    protected JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    protected MockedJobStoreServiceConnector jobStoreServiceConnector;
    protected SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();

    static {
        ES_INFLIGHT_DATASOURCE = new PGSimpleDataSource();
        ES_INFLIGHT_DATASOURCE.setDatabaseName(ES_INFLIGHT_DATABASE_NAME);
        ES_INFLIGHT_DATASOURCE.setServerName("localhost");
        ES_INFLIGHT_DATASOURCE.setPortNumber(Integer.parseInt(System.getProperty("postgresql.port")));
        ES_INFLIGHT_DATASOURCE.setUser(System.getProperty("user.name"));
        ES_INFLIGHT_DATASOURCE.setPassword(System.getProperty("user.name"));

        ES_DATABASE_NAME = System.getProperty("es.dbname");

        try {
            ES_DATASOURCE = new OracleDataSource();
            ES_DATASOURCE.setURL("jdbc:oracle:thin:@tora1.dbc.dk:1521/tora1.dbc.dk");
            ES_DATASOURCE.setUser("jbn");
            ES_DATASOURCE.setPassword("jbn");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeClass
    public static void setInitialContextFactory() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @BeforeClass
    public static void createEsInFlightDb() {
        final StartupDBMigrator dbMigrator = new StartupDBMigrator();
        dbMigrator.dataSource = ES_INFLIGHT_DATASOURCE;
        dbMigrator.onStartup();
    }

    @Before
    public void setInitialContext() {
        InMemoryInitialContextFactory.bind(ES_RESOURCE_NAME, ES_DATASOURCE);
    }

    @Before
    public void mockJmsContext() {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Before
    public void mockJobStoreServiceConnector() {
        jobStoreServiceConnector = new MockedJobStoreServiceConnector();
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Before
    public void initialiseEsInFlightDbEntityManager() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, System.getProperty("user.name"));
        properties.put(JDBC_PASSWORD, System.getProperty("user.name"));
        properties.put(JDBC_URL, String.format("jdbc:postgresql://localhost:%s/%s",
                System.getProperty("postgresql.port"), ES_INFLIGHT_DATABASE_NAME));
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");

        final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("esInFlightIT", properties);
        esInFlightEntityManager = entityManagerFactory.createEntityManager(properties);
    }

    @Before
    public void createEsDatabase() throws SQLException {
        try (final Connection connection = ES_DATASOURCE.getConnection()) {
            ESUtil.createDatabaseIfNotExisting(connection, ES_DATABASE_NAME);
        }
    }

    @After
    public void clearEntityManagerCache() {
        esInFlightEntityManager.clear();
        esInFlightEntityManager.getEntityManagerFactory().getCache().evictAll();
    }

    @After
    public void clearEsInFlight() {
        final EntityTransaction transaction = esInFlightEntityManager.getTransaction();
        transaction.begin();
        final Query nativeQuery = esInFlightEntityManager.createNativeQuery("DELETE FROM esinflight");
        nativeQuery.executeUpdate();
        transaction.commit();
    }

    @After
    public void removeEsDatabase() throws SQLException {
        try (final Connection connection = ES_DATASOURCE.getConnection()) {
            ESUtil.deleteTaskpackages(connection, ES_DATABASE_NAME);
            ESUtil.deleteDatabase(connection, ES_DATABASE_NAME);
        }
    }

    @After
    public void clearInitialContext() {
        InMemoryInitialContextFactory.clear();
    }

    protected EsMessageProcessorBean getEsMessageProcessorBean() {
        final TestableEsMessageProcessorBean esMessageProcessorBean = new TestableEsMessageProcessorBean();
        esMessageProcessorBean.configuration = getEsSinkConfigurationBean();
        esMessageProcessorBean.esConnector = getEsConnectorBean();
        esMessageProcessorBean.esInFlightAdmin = getEsInFlightBean();
        esMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        esMessageProcessorBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        esMessageProcessorBean.setup();
        return esMessageProcessorBean;
    }

    protected EsSinkConfigurationBean getEsSinkConfigurationBean() {
        final EsSinkConfigurationBean esSinkConfigurationBean = new EsSinkConfigurationBean();
        esSinkConfigurationBean.esDatabaseName = ES_DATABASE_NAME;
        esSinkConfigurationBean.esResourceName = ES_RESOURCE_NAME;
        return esSinkConfigurationBean;
    }

    protected EsConnectorBean getEsConnectorBean() {
        final EsConnectorBean esConnectorBean = new EsConnectorBean();
        esConnectorBean.configuration = getEsSinkConfigurationBean();
        return esConnectorBean;
    }

    protected EsInFlightBean getEsInFlightBean() {
        final EsInFlightBean esInFlightBean = new EsInFlightBean();
        esInFlightBean.configuration = getEsSinkConfigurationBean();
        esInFlightBean.entityManager = esInFlightEntityManager;
        return esInFlightBean;
    }

    protected MockedJmsTextMessage getSinkMessage(ExternalChunk chunk) throws JMSException, JSONBException {
        final Sink sink = new SinkBuilder().build();
        final MockedJmsTextMessage message = (MockedJmsTextMessage) sinkMessageProducerBean
                .createMessage(jmsContext, chunk, sink);
        message.setText(jsonbContext.marshall(chunk));
        return message;
    }

    protected List<EsInFlight> listEsInFlight() {
        final TypedQuery<EsInFlight> query = esInFlightEntityManager.createQuery(
                "SELECT esInFlight FROM EsInFlight esInFlight", EsInFlight.class);
        return query.getResultList();
    }

    protected List<Integer> findTaskPackages() {
        try (final Connection connection = ES_DATASOURCE.getConnection()) {
            return ESTaskPackageIntegrationTestUtil.findTaskpackagesForDBName(connection, ES_DATABASE_NAME);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected byte[] getValidAddi() {
        return ("131\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "</es:referencedata>\n1\nb\n").getBytes(StandardCharsets.UTF_8);
    }

    protected byte[] getValidAddiWithProcessingTrue() {
        return ("235\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>" +
                "\n506\n" +
                "<marcx:record xmlns:marcx='info:lc/xmlns/marcxchange-v1'>" +
                "<marcx:leader>00000n    2200000   4500</marcx:leader>" +
                "<marcx:datafield tag='100' ind1='0' ind2='0'>" +
                "<marcx:subfield code='a'>field1</marcx:subfield>" +
                "<marcx:subfield code='b'/>" +
                "<marcx:subfield code='d'>Field2</marcx:subfield>" +
                "</marcx:datafield><marcx:datafield tag='101' ind1='1' ind2='2'>" +
                "<marcx:subfield code='h'>est</marcx:subfield>" +
                "<marcx:subfield code='k'>o</marcx:subfield>" +
                "<marcx:subfield code='G'>ris</marcx:subfield>" +
                "</marcx:datafield></marcx:record>\n").getBytes(StandardCharsets.UTF_8);
    }

    protected static class TestableEsMessageProcessorBean extends EsMessageProcessorBean {
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
        public MessageDrivenContext getMessageDrivenContext() {
            return this.messageDrivenContext;
        }
    }
}
