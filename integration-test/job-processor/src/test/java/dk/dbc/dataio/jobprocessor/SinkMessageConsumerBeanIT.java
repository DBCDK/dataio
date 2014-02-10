package dk.dbc.dataio.jobprocessor;

import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.JobStoreMessageProducerBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class SinkMessageConsumerBeanIT {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;
    private JMSContext jmsContext;

    @Before
    public void setupMocks() {
        jmsContext = mock(JMSContext.class);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.SINKS_QUEUE_NAME);
    }

    @Test
    public void sinkMessageConsumerBean_invalidSinkResultOnSinksQueue_eventuallyRemovedFromSinksQueue() throws JMSException, InterruptedException, JsonException {
        final MockedJmsTextMessage sinkMessage = newSinkMessageForJobProcessor(new SinkChunkResultBuilder().build());
        sinkMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, sinkMessage);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
    }

    @Test
    public void sinkMessageConsumerBean_validSinkResultOnSinksQueue_eventuallyRemovedFromSinksQueueAndPutOnProcessorQueue() throws JMSException, InterruptedException, JsonException {
        final long jobId = 42;
        final SinkChunkResult sinkResultIn = new SinkChunkResultBuilder()
                .setJobId(jobId)
                .build();
        final MockedJmsTextMessage sinkMessage = newSinkMessageForJobProcessor(sinkResultIn);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, sinkMessage);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        final List<MockedJmsTextMessage> processorQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        assertSinkMessageForJobStore(processorQueue.get(0), jobId);
    }

    private static void assertSinkMessageForJobStore(MockedJmsTextMessage message, long jobId) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        assertThat(message.getStringProperty(JobStoreMessageProducerBean.SOURCE_PROPERTY_NAME), is(JobStoreMessageProducerBean.SOURCE_PROPERTY_VALUE));
        assertThat(message.getStringProperty(JobStoreMessageProducerBean.PAYLOAD_PROPERTY_NAME), is(JobStoreMessageProducerBean.SINK_RESULT_PAYLOAD_PROPERTY_VALUE));
        final SinkChunkResult sinkResultOut = JsonUtil.fromJson(message.getText(), SinkChunkResult.class, MixIns.getMixIns());
        assertThat(sinkResultOut.getJobId(), is(jobId));
    }

    private MockedJmsTextMessage newSinkMessageForJobProcessor(SinkChunkResult sinkResult) throws JMSException, JsonException {
        final MockedJmsTextMessage message = new MockedJmsTextMessage();
        message.setStringProperty("payload", "SinkChunkResult");
        message.setStringProperty("chunkResultSource", "sink");
        message.setText(JsonUtil.toJson(sinkResult));
        return message;
    }
}
