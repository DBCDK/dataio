package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.JobStoreMessageProducerBean;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobProcessorMessageConsumerBeanIT extends AbstractJobStoreTest {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;

    private JMSContext jmsContext;

    @Before
    public void setupMocks() {
        jmsContext = mock(JMSContext.class);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Test
    public void onMessage_resultReceived_resultSavedAndJobStateUpdated() throws URISyntaxException, JsonException, JMSException, JobStoreServiceConnectorException {
        final JobInfo jobInfo = createJob(restClient, JobsBeanIT.setupJobPrerequisites(restClient));

        // Swallow 1st Chunk message
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);

        JobState jobState = getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.PENDING));

        // Put 1st sink result on queue
        ExternalChunk deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED)
                .setJobId(jobInfo.getJobId())
                .setChunkId(1L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newSinkResultMessageForJobStoreSink(deliveredChunk));

        // Put 1st processor result on queue
        ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setJobId(jobInfo.getJobId())
                .setChunkId(1L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newProcessorResultMessageForJobStore(processedChunk));

        // Swallow 2nd Chunk message
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);

        getProcessorResult(restClient, jobInfo.getJobId(), 1L);
        getSinkResult(restClient, jobInfo.getJobId(), 1L);
        jobState = getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.ACTIVE));
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.DELIVERING), is(JobState.LifeCycleState.ACTIVE));

        // Put 2nd processor result on queue
        processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setJobId(jobInfo.getJobId())
                .setChunkId(2L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newProcessorResultMessageForJobStore(processedChunk));

        // Put 2nd sink result on queue
        deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED)
                .setJobId(jobInfo.getJobId())
                .setChunkId(2L)
                .build();
        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, newSinkResultMessageForJobStoreSink(deliveredChunk));

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        getProcessorResult(restClient, jobInfo.getJobId(), 2L);
        getSinkResult(restClient, jobInfo.getJobId(), 2L);
        jobState = getState(restClient, jobInfo.getJobId());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.DONE));
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.DELIVERING), is(JobState.LifeCycleState.DONE));
    }

    private MockedJmsTextMessage newProcessorResultMessageForJobStore(ExternalChunk processedChunk) throws JMSException, JsonException {
        final MockedJmsTextMessage message = (MockedJmsTextMessage) new JobStoreMessageProducerBean()
                .createMessage(jmsContext, processedChunk);
        message.setText(JsonUtil.toJson(processedChunk));
        return message;
    }

    private MockedJmsTextMessage newSinkResultMessageForJobStoreSink(ExternalChunk deliveredChunk) throws JMSException, JsonException {
        final MockedJmsTextMessage message = (MockedJmsTextMessage) new JobStoreMessageProducerBean()
                .createMessageSink(jmsContext, deliveredChunk);
        message.setText(JsonUtil.toJson(deliveredChunk));
        return message;
    }
}
