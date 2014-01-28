package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.integrationtest.ESTaskPackageIntegrationTestUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.SinkMessageProducerBean;
import dk.dbc.dataio.jobprocessor.util.Base64Util;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@Ignore
public class EsMessageProcessorBeanIT {
    private static final String SINK_NAME = "esSinkIT";
    private static final String ES_RESOURCE_NAME = "jdbc/dataio/es";
    private static final String ES_INFLIGHT_DATABASE_NAME = "es_inflight";
    private static final String ADDI_OK = "1\na\n1\nb\n";
    private static String ES_DATABASE_NAME;

    private JMSContext jmsContext;

    @BeforeClass
    public static void setUpClass() {
        ES_DATABASE_NAME = System.getProperty("es.dbname");
    }

    @Before
    public void setupMocks() {
        jmsContext = mock(JMSContext.class);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Before
    public void createEsDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = getEsConnection()) {
            ESUtil.createDatabaseIfNotExisting(connection, ES_DATABASE_NAME);
        }
    }

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.SINKS_QUEUE_NAME);
    }

    @After
    public void emptyInFlightDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.newDbConnection(ES_INFLIGHT_DATABASE_NAME)) {
            ITUtil.clearDbTables(connection, "esinflight");
        }
    }

    @After
    public void removeEsDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = getEsConnection()) {
            ESUtil.deleteTaskpackages(connection, ES_DATABASE_NAME);
            ESUtil.deleteDatabase(connection, ES_DATABASE_NAME);
        }
    }

    @Test
    public void esMessageProcessorBean_invalidProcessorResultOnSinksQueue_eventuallyRemovedFromSinksQueue()
            throws JMSException, InterruptedException, JsonException, SQLException, ClassNotFoundException {
        final MockedJmsTextMessage processorMessage = newProcessorMessageForSink(new ChunkResultBuilder().build());
        processorMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, processorMessage);

        Thread.sleep(500);
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME), is(0));
        assertThat(getNumberOfRecordsInFlight(), is(0));
    }

    @Test
    public void esMessageProcessorBean_validProcessorResultOnSinksQueue_eventuallyProcessed()
            throws JsonException, JMSException, InterruptedException, SQLException, ClassNotFoundException {
        final ChunkResult processorResult = new ChunkResultBuilder()
                .setResults(Arrays.asList(Base64Util.base64encode(ADDI_OK)))
                .build();
        final MockedJmsTextMessage processorMessage = newProcessorMessageForSink(processorResult);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, processorMessage);

        Thread.sleep(1000);
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME), is(0));
        assertThat(getNumberOfRecordsInFlight(), is(1));
        final List<Integer> esTaskPackages = getEsTaskPackages();
        assertThat(esTaskPackages.size(), is(1));

        successfullyCompleteEsTaskPackages(esTaskPackages.get(0));
        awaitInFlightEmpty();

        assertThat(getNumberOfRecordsInFlight(), is(0));
        assertThat(getEsTaskPackages().size(), is(0));
        final List<MockedJmsTextMessage> sinksQueue = JmsQueueConnector.listQueue(JmsQueueConnector.SINKS_QUEUE_NAME);
        assertThat(sinksQueue.size(), is(1));
        final SinkChunkResult sinkResult = assertSinkMessageForProcessor(sinksQueue.get(0));
        assertThat(sinkResult.getJobId(), is(processorResult.getJobId()));
        assertThat(sinkResult.getChunkId(), is(processorResult.getChunkId()));
    }

    private MockedJmsTextMessage newProcessorMessageForSink(ChunkResult processorResult) throws JMSException, JsonException {
        final SinkContent sinkContent = new SinkContentBuilder()
                .setName(SINK_NAME)
                .setResource(ES_RESOURCE_NAME)
                .build();
        final Sink sink = new SinkBuilder()
                .setContent(sinkContent)
                .build();
        return (MockedJmsTextMessage) new SinkMessageProducerBean()
                .createMessage(jmsContext, processorResult, sink);
    }

    private int getNumberOfRecordsInFlight() throws SQLException, ClassNotFoundException {
        int numberOfRecordsInFlight = 0;
        final String getNumberOfRecordsInFlightStmt = "SELECT SUM(recordslots) FROM esinflight WHERE resourcename = ?";
        try (
            final Connection connection = ITUtil.newDbConnection(ES_INFLIGHT_DATABASE_NAME);
            final PreparedStatement pstmt = JDBCUtil.query(connection, getNumberOfRecordsInFlightStmt, ES_RESOURCE_NAME);
            final ResultSet rs = pstmt.getResultSet()) {
            while (rs.next()) {
                numberOfRecordsInFlight = rs.getInt(1);
            }
        }
        return numberOfRecordsInFlight;
    }

    private Connection getEsConnection() throws ClassNotFoundException, SQLException {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        return DriverManager.getConnection(
                "jdbc:oracle:thin:@tora1.dbc.dk:1521/tora1.dbc.dk", "jbn", "jbn");
    }

    private List<Integer> getEsTaskPackages() throws SQLException, ClassNotFoundException {
        try (final Connection connection = getEsConnection()) {
            return ESTaskPackageIntegrationTestUtil.findTaskpackagesForDBName(connection, ES_DATABASE_NAME);
        }
    }

    private void successfullyCompleteEsTaskPackages(int targetReference) throws SQLException, ClassNotFoundException {
        try (final Connection connection = getEsConnection()) {
            ESTaskPackageIntegrationTestUtil.successfullyCompleteTaskpackage(connection, targetReference);
        }
    }

    private void awaitInFlightEmpty() throws InterruptedException, SQLException, ClassNotFoundException {
        int numberOfTries = 0;
        while (numberOfTries < 30) {
            Thread.sleep(1000);
            numberOfTries++;
            if (getNumberOfRecordsInFlight() == 0) {
                break;
            }
        }
    }

    public SinkChunkResult assertSinkMessageForProcessor(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        return JsonUtil.fromJson(message.getText(), SinkChunkResult.class, MixIns.getMixIns());
    }
}
