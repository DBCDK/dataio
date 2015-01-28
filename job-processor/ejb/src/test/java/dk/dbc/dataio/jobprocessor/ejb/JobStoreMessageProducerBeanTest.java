package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JsonUtil.class,
})
public class JobStoreMessageProducerBeanTest {
    private ConnectionFactory jmsConnectionFactory;
    private JMSContext jmsContext;
    private JMSProducer jmsProducer;
    private TextMessage textMessage;

    @Before
    public void setup() {
        jmsConnectionFactory = mock(ConnectionFactory.class);
        jmsContext = mock(JMSContext.class);
        jmsProducer = mock(JMSProducer.class);
        textMessage = mock(TextMessage.class);

        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @Test(expected = NullPointerException.class)
    public void send_sinkResultArgIsNull_throws() throws JobProcessorException {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        jobStoreMessageProducerBean.sendSink((ExternalChunk) null);
    }

    @Test(expected = JobProcessorException.class)
    public void send_createMessageWithSinkResultPayloadThrowsJMSException_throws() throws JobProcessorException, JMSException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(textMessage);
        doThrow(new JMSException("JMSException"))
                .when(textMessage).setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        jobStoreMessageProducerBean.sendSink(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());
    }

    @Test(expected = JobProcessorException.class)
    public void send_createMessageWithSinkResultPayloadThrowsJsonException_throws() throws JobProcessorException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(ExternalChunk.class))).thenThrow(new JsonException("JsonException"));
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        jobStoreMessageProducerBean.sendSink(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());
    }

    @Test(expected = NullPointerException.class)
    public void send_processorResultArgIsNull_throws() throws JobProcessorException {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        jobStoreMessageProducerBean.sendProc((ExternalChunk) null);
    }

    @Test(expected = JobProcessorException.class)
    public void send_createMessageWithProcessorResultPayloadThrowsJMSException_throws() throws JobProcessorException, JMSException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(textMessage);
        doThrow(new JMSException("JMSException"))
                .when(textMessage).setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        jobStoreMessageProducerBean.sendProc(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
    }

    @Test(expected = JobProcessorException.class)
    public void send_createMessageWithProcessorResultPayloadThrowsJsonException_throws() throws JobProcessorException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(ExternalChunk.class))).thenThrow(new JsonException("JsonException"));
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        jobStoreMessageProducerBean.sendProc(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
    }

    @Test
    public void createMessage_sinkResultArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JsonException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        TextMessage message = jobStoreMessageProducerBean.createMessageSink(jmsContext, new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());
        assertThat(message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.SINK_RESULT_PAYLOAD_TYPE));
    }

    @Test
    public void createMessage_processorResultArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JsonException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        TextMessage message = jobStoreMessageProducerBean.createMessage(
                jmsContext, 
                new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
        assertThat(message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE));
    }

    private JobStoreMessageProducerBean getInitializedBean() {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = new JobStoreMessageProducerBean();
        jobStoreMessageProducerBean.jobStoreQueueConnectionFactory = jmsConnectionFactory;
        return jobStoreMessageProducerBean;
    }
}
