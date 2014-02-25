package dk.dbc.dataio.jobprocessor;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.NewJobBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobstore.ejb.JobProcessorMessageProducerBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.util.List;

import static dk.dbc.dataio.jobprocessor.util.Base64Util.base64decode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class JobStoreMessageConsumerBeanIT {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;

    private final String sinkResourceName = "sinkResourceName";

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
    public void jobStoreMessageConsumerBean_invalidNewJobOnProcessorQueue_eventuallyRemovedFromProcessorQueue() throws JMSException, JsonException {
        final MockedJmsTextMessage jobStoreMessage = newJobStoreMessageForJobProcessor(new NewJobBuilder().build());
        jobStoreMessage.setText("invalid");

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.SINKS_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);
    }

    @Test
    public void jobStoreMessageConsumerBean_validNewJobOnProcessorQueue_eventuallyRemovedForProcessingWithResultsOnProcessorAndSinkQueues() throws Exception {
        final long jobId = 42;

        final NewJob newJob = new NewJobBuilder()
                .setJobId(jobId)
                .setChunkCount(2)
                .setSink(getSink())
                .build();

        final MockedJmsTextMessage jobStoreMessage = newJobStoreMessageForJobProcessor(newJob);

        JmsQueueConnector.putOnQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME, jobStoreMessage);

        final List<MockedJmsTextMessage> sinksQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.SINKS_QUEUE_NAME, 2, MAX_QUEUE_WAIT_IN_MS);

        ChunkResult processorResult;

        processorResult = assertProcessorMessageForSink(sinksQueue.get(0));
        assertThat(processorResult.getJobId(), is(jobId));
        assertThat(processorResult.getChunkId(), is(1L));
        assertThat(processorResult.getItems().size(), is(1));
        assertThat(base64decode(processorResult.getItems().get(0).getData()), is("ONE"));

        processorResult = assertProcessorMessageForSink(sinksQueue.get(1));
        assertThat(processorResult.getJobId(), is(jobId));
        assertThat(processorResult.getChunkId(), is(2L));
        assertThat(processorResult.getItems().size(), is(1));
        assertThat(base64decode(processorResult.getItems().get(0).getData()), is("TWO"));

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 2, MAX_QUEUE_WAIT_IN_MS);
    }

    private ChunkResult assertProcessorMessageForSink(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        assertThat(message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE));
        assertThat(message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(sinkResourceName));
        return JsonUtil.fromJson(message.getText(), ChunkResult.class, MixIns.getMixIns());
    }

    private MockedJmsTextMessage newJobStoreMessageForJobProcessor(NewJob newJob) throws JMSException, JsonException {
        final MockedJmsTextMessage message = (MockedJmsTextMessage) new JobProcessorMessageProducerBean()
                .createMessage(jmsContext, newJob);
        message.setText(JsonUtil.toJson(newJob));
        return message;
    }

    private Sink getSink() throws Exception {
        return new SinkBuilder()
                .setContent(getSinkContent())
                .build();
    }

    private SinkContent getSinkContent() throws Exception {
        return new SinkContentBuilder()
                .setResource(sinkResourceName)
                .build();
    }
}
