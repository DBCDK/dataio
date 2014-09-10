package dk.dbc.dataio.sink.utils.messageproducer;

import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JsonUtil.class,
})
public class JobProcessorMessageProducerBeanTest {
    private ConnectionFactory jmsConnectionFactory;
    private JMSContext jmsContext;
    private JMSProducer jmsProducer;

    @Before
    public void setup() {
        jmsConnectionFactory = mock(ConnectionFactory.class);
        jmsContext = mock(JMSContext.class);
        jmsProducer = mock(JMSProducer.class);

        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @Test
    public void send_sinkChunkResultArgIsNull_throws() throws SinkException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        try {
            jobProcessorMessageProducerBean.send(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void send_createMessageThrowsJsonException_throws() throws JsonException, SinkException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(SinkChunkResult.class))).thenThrow(new JsonException("JsonException"));
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        try {
            jobProcessorMessageProducerBean.send(sinkChunkResult);
            fail("No exception thrown");
        } catch (SinkException e) {
        }
    }

    @Test
    public void send_sinkChunkResultIsValid_Success() throws SinkException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        when(jmsProducer.send(any(Queue.class), any(TextMessage.class))).thenReturn(jmsProducer);
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();

        jobProcessorMessageProducerBean.send(sinkChunkResult);
    }

    @Test
    public void sendAll_sinkChunkResultsArgIsNull_throws() throws SinkException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        try {
            jobProcessorMessageProducerBean.sendAll(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void sendAll_sinkChunkResultsArgContainsNullEntry_throws() throws SinkException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        try {
            jobProcessorMessageProducerBean.sendAll(Arrays.asList((SinkChunkResult)null));
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void sendAll_createMessageThrowsJsonException_throws() throws JsonException, SinkException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(SinkChunkResult.class))).thenThrow(new JsonException("JsonException"));
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        try {
            jobProcessorMessageProducerBean.sendAll(Arrays.asList(sinkChunkResult));
            fail("No exception thrown");
        } catch (SinkException e) {
        }
    }

    @Test
    public void sendAll_sinkChunkResultsEntriesAreValid_allEntriesSent() throws SinkException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        when(jmsProducer.send(any(Queue.class), any(TextMessage.class))).thenReturn(jmsProducer);
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();

        jobProcessorMessageProducerBean.sendAll(Arrays.asList(sinkChunkResult, sinkChunkResult, sinkChunkResult));

        verify(jmsProducer, times(3)).send(any(Queue.class), any(TextMessage.class));
    }

    @Test
    public void createMessage_sinkChunkResultArgIsValid_returnsMessageWithHeaderProperties() throws JsonException, JMSException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        final TextMessage message = jobProcessorMessageProducerBean.createMessage(jmsContext, new SinkChunkResultBuilder().build());
        assertThat(message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.SINK_SOURCE_VALUE));
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.SINK_RESULT_PAYLOAD_TYPE));
    }

    private JobProcessorMessageProducerBean getInitializedBean() {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = new JobProcessorMessageProducerBean();
        jobProcessorMessageProducerBean.processorQueueConnectionFactory = jmsConnectionFactory;
        return jobProcessorMessageProducerBean;
    }

}
