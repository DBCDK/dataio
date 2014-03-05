package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.JobStoreMessageProducerBean;
import dk.dbc.dataio.jobstore.types.JobState;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.ws.rs.client.Client;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class JobProcessorMessageConsumerBeanIT {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;

    private static Client restClient;

    private JMSContext jmsContext;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException {
        restClient = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
    }

    @AfterClass
    public static void clearJobStore() {
        ITUtil.clearJobStore();
    }

    @Before
    public void setupMocks() {
        jmsContext = mock(JMSContext.class);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
    }

    @After
    public void emptyFlowStoreDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.newDbConnection(ITUtil.FLOW_STORE_DATABASE_NAME)) {
            ITUtil.clearAllDbTables(connection);
        }
    }

    @Test
    public void onMessage_processorResultReceived_processorResultSavedAndJobStateUpdated() throws URISyntaxException, JsonException, JMSException, JobStoreServiceConnectorException {
        final JobInfo jobInfo = JobsBeanIT.createJob(restClient);

        // Swallow NewJob message
        JmsQueueConnector.awaitQueueList(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);

        JobState jobState = JobsBeanIT.getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.PENDING));

        // Put 1st processor result on queue
        ChunkResult processorResult = new ChunkResultBuilder()
                .setJobId(jobInfo.getJobId())
                .setChunkId(1L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newProcessorResultMessageForJobStore(processorResult));
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JobsBeanIT.getProcessorResult(restClient, jobInfo.getJobId(), 1L);
        jobState = JobsBeanIT.getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.ACTIVE));

        // Put 2nd and last processor result on queue
        processorResult = new ChunkResultBuilder()
                .setJobId(jobInfo.getJobId())
                .setChunkId(2L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newProcessorResultMessageForJobStore(processorResult));
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JobsBeanIT.getProcessorResult(restClient, jobInfo.getJobId(), 2L);
        jobState = JobsBeanIT.getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.DONE));
    }

    @Test
    public void onMessage_sinkResultReceived_sinkResultSavedAndJobStateUpdated() throws URISyntaxException, JsonException, JMSException, JobStoreServiceConnectorException {
        final JobInfo jobInfo = JobsBeanIT.createJob(restClient);

        // Swallow NewJob message
        JmsQueueConnector.awaitQueueList(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);

        JobState jobState = JobsBeanIT.getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.DELIVERING), is(JobState.LifeCycleState.PENDING));

        // Put 1st sink result on queue
        SinkChunkResult sinkResult = new SinkChunkResultBuilder()
                .setJobId(jobInfo.getJobId())
                .setChunkId(1L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newSinkResultMessageForJobStore(sinkResult));
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JobsBeanIT.getSinkResult(restClient, jobInfo.getJobId(), 1L);
        jobState = JobsBeanIT.getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.DELIVERING), is(JobState.LifeCycleState.ACTIVE));

        // Put 2nd and last sink result on queue
        sinkResult = new SinkChunkResultBuilder()
                .setJobId(jobInfo.getJobId())
                .setChunkId(2L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newSinkResultMessageForJobStore(sinkResult));
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JobsBeanIT.getSinkResult(restClient, jobInfo.getJobId(), 2L);
        jobState = JobsBeanIT.getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.DELIVERING), is(JobState.LifeCycleState.DONE));
    }

    private MockedJmsTextMessage newProcessorResultMessageForJobStore(ChunkResult processorResult) throws JMSException, JsonException {
        final MockedJmsTextMessage message = (MockedJmsTextMessage) new JobStoreMessageProducerBean()
                .createMessage(jmsContext, processorResult);
        message.setText(JsonUtil.toJson(processorResult));
        return message;
    }

    private MockedJmsTextMessage newSinkResultMessageForJobStore(SinkChunkResult sinkResult) throws JMSException, JsonException {
        final MockedJmsTextMessage message = (MockedJmsTextMessage) new JobStoreMessageProducerBean()
                .createMessage(jmsContext, sinkResult);
        message.setText(JsonUtil.toJson(sinkResult));
        return message;
    }
}
