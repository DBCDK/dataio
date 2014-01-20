package dk.dbc.dataio.jobprocessor;

import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.SinkChunkResultJsonBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobprocessor.ejb.JobStoreMessageProducerBean;
import org.junit.After;
import org.junit.Test;

import javax.jms.JMSException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SinkMessageConsumerBeanIT {
    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.SINKS_QUEUE_NAME);
    }

    @Test
    public void sinkMessageConsumerBean_invalidSinkResultOnSinksQueue_eventuallyRemovedFromSinksQueue() throws JMSException, InterruptedException {
        final MockedJmsTextMessage sinkMessage = JmsQueueConnector.newSinkMessageForJobProcessor();
        sinkMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, sinkMessage);

        Thread.sleep(500);
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME), is(0));
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME), is(0));
    }

    @Test
    public void sinkMessageConsumerBean_validSinkResultOnSinksQueue_eventuallyRemovedFromSinksQueueAndPutOnProcessorQueue() throws JMSException, InterruptedException, JsonException {
        final long jobId = 42;
        final String sinkResultIn = new SinkChunkResultJsonBuilder()
                .setJobId(jobId)
                .build();
        final MockedJmsTextMessage sinkMessage = JmsQueueConnector.newSinkMessageForJobProcessor();
        sinkMessage.setText(sinkResultIn);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.SINKS_QUEUE_NAME, sinkMessage);

        Thread.sleep(500);
        assertThat(JmsQueueConnector.getQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME), is(0));
        final List<MockedJmsTextMessage> processorQueue = JmsQueueConnector.listQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
        assertThat(processorQueue.size(), is(1));
        assertSinkMessageForJobStore(processorQueue.get(0), jobId);
    }

    public static void assertSinkMessageForJobStore(MockedJmsTextMessage message, long jobId) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        assertThat(message.getStringProperty(JobStoreMessageProducerBean.SOURCE_PROPERTY_NAME), is(JobStoreMessageProducerBean.SOURCE_PROPERTY_VALUE));
        assertThat(message.getStringProperty(JobStoreMessageProducerBean.PAYLOAD_PROPERTY_NAME), is(JobStoreMessageProducerBean.PAYLOAD_PROPERTY_VALUE));
        final SinkChunkResult sinkResultOut = JsonUtil.fromJson(message.getText(), SinkChunkResult.class, MixIns.getMixIns());
        assertThat(sinkResultOut.getJobId(), is(jobId));
    }
}
