package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.integrationtest.ESTaskPackageIntegrationTestUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.SinkMessageProducerBean;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@Ignore("24-02-2015 (jbn) - Tests fail intermittently, although apparently more often on Java8")
public class EsMessageProcessorBeanIT {
    // todo: investigate why it takes a very long time (30+ secs) to obtain a ES db connection in certain cases (fx. on the Jenkins node)
    private static final long MAX_QUEUE_WAIT_IN_MS = 120000;

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
        try (final Connection connection = ITUtil.getEsConnection()) {
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
        try (final Connection connection = ITUtil.getEsConnection()) {
            ESUtil.deleteTaskpackages(connection, ES_DATABASE_NAME);
            ESUtil.deleteDatabase(connection, ES_DATABASE_NAME);
        }
    }

    @Test
    public void esMessageProcessorBean_invalidProcessorResultOnSinksQueue_eventuallyRemovedFromSinksQueue()
            throws JMSException, JsonException, SQLException, ClassNotFoundException, JSONBException {
        final MockedJmsTextMessage processorMessage = 
                newProcessorMessageForSink(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
        processorMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, processorMessage);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        assertThat(getNumberOfRecordsInFlight(), is(0));
    }

    @Ignore("sma 15-09-2014: This test is currently not running when executed locally. It is working on Jenkins")
    @Test
    public void esMessageProcessorBean_chunkWithAllRecordsFailed_notProcessedAndSinkResultIsAllIgnored()
            throws JMSException, JsonException, SQLException, ClassNotFoundException, JSONBException {
        final int itemsInChunk = 10;
        // Create ChunkResult with 10 failed items:
        List<ChunkItem> items = new ArrayList<>(itemsInChunk);
        for(long i=0; i<itemsInChunk; i++) {
            items.add( new ChunkItemBuilder().setId(i).setStatus(ChunkItem.Status.FAILURE).build() );
        }
        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();

        // Put ChunkResult on Queue as message:
        final MockedJmsTextMessage processorMessage = newProcessorMessageForSink(processedChunk);
        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, processorMessage);

        // Below wait is defunc - since processing happens so fast that
        // the result is put on the queue before we can assert the empty queue
        //
        // Wait for sink-queue to be empty - ie. message has been taken by ProcessorBean:
        //JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);

        // Since all items are failed, there should not be any Records in flight:
        assertThat(getNumberOfRecordsInFlight(), is(0));

        // There should not have been added any taskpackages
        assertThat(getEsTaskPackages().size(), is(0));

        // Get SinkResult from queue
        final List<MockedJmsTextMessage> sinksQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.SINKS_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        final ExternalChunk deliveredChunk = assertSinkMessageForProcessor(sinksQueue.get(0));

        // Assert that SinkResult corresponds to ChunkResult and that all items are ignored:
        assertThat(deliveredChunk.getJobId(), is(processedChunk.getJobId()));
        assertThat(deliveredChunk.getChunkId(), is(processedChunk.getChunkId()));
        assertThat(deliveredChunk.size(), is(itemsInChunk));
        for (final ChunkItem chunkItem : deliveredChunk) {
            assertThat(chunkItem.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }


    @Test
    public void esMessageProcessorBean_validProcessorResultOnSinksQueue_eventuallyProcessed()
            throws JsonException, JMSException, InterruptedException, SQLException, ClassNotFoundException, JSONBException {
        final ChunkItem item = new ChunkItemBuilder()
                .setData(Base64Util.base64encode(ADDI_OK))
                .build();
        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(Arrays.asList(item))
                .build();
        final MockedJmsTextMessage processorMessage = newProcessorMessageForSink(processedChunk);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, processorMessage);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        assertThat(getNumberOfRecordsInFlight(), is(1));
        final List<Integer> esTaskPackages = getEsTaskPackages();
        assertThat(esTaskPackages.size(), is(1));

        successfullyCompleteEsTaskPackages(esTaskPackages.get(0));
        awaitInFlightEmpty();

        assertThat(getNumberOfRecordsInFlight(), is(0));
        assertThat(getEsTaskPackages().size(), is(0));
        final List<MockedJmsTextMessage> sinksQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.SINKS_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        final ExternalChunk deliveredChunk = assertSinkMessageForProcessor(sinksQueue.get(0));
        assertThat(deliveredChunk.getJobId(), is(processedChunk.getJobId()));
        assertThat(deliveredChunk.getChunkId(), is(processedChunk.getChunkId()));
        assertThat(deliveredChunk.size(), is(1));
        for (final ChunkItem chunkItem : deliveredChunk) {
            assertThat(chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
        }
    }

    private MockedJmsTextMessage newProcessorMessageForSink(ExternalChunk processedChunk) throws JMSException, JsonException, JSONBException {
        final SinkContent sinkContent = new SinkContentBuilder()
                .setName(SINK_NAME)
                .setResource(ES_RESOURCE_NAME)
                .build();
        final Sink sink = new SinkBuilder()
                .setContent(sinkContent)
                .build();
        final MockedJmsTextMessage message = (MockedJmsTextMessage) getSinkMessageProducerBean()
                .createMessage(jmsContext, processedChunk, sink);
        message.setText(JsonUtil.toJson(processedChunk));
        return message;
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

    private List<Integer> getEsTaskPackages() throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.getEsConnection()) {
            return ESTaskPackageIntegrationTestUtil.findTaskpackagesForDBName(connection, ES_DATABASE_NAME);
        }
    }

    private void successfullyCompleteEsTaskPackages(int targetReference) throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.getEsConnection()) {
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

    private SinkMessageProducerBean getSinkMessageProducerBean() {
        return new SinkMessageProducerBean();
    }

    public ExternalChunk assertSinkMessageForProcessor(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        ExternalChunk deliveredChunk = JsonUtil.fromJson(message.getText(), ExternalChunk.class);
        assertThat(deliveredChunk.getType(), is(ExternalChunk.Type.DELIVERED));
        return deliveredChunk;
    }
}
