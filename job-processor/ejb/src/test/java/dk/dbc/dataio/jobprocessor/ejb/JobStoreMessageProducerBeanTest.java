package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void send_sinkResultArgIsNull_throws() throws JobProcessorException {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        try {
            jobStoreMessageProducerBean.sendSink(null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void send_createMessageWithSinkResultPayloadThrowsJMSException_throws() throws JobProcessorException, JMSException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(textMessage);
        doThrow(new JMSException("JMSException"))
                .when(textMessage).setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        try {
            jobStoreMessageProducerBean.sendSink(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());
            fail("No Exception thrown");
        } catch (JobProcessorException e) {
        }
    }

    @Test
    public void send_createMessageWithSinkResultPayloadThrowsJsonException_throws() throws JobProcessorException, JSONBException {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        final JSONBContext jsonbContext = mock(JSONBContext.class);
        when(jobStoreMessageProducerBean.jsonBinding.getContext()).thenReturn(jsonbContext);
        when(jsonbContext.marshall(anyObject())).thenThrow(new JSONBException("JsonException"));
        try {
            jobStoreMessageProducerBean.sendSink(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());
            fail("No Exception thrown");
        } catch (JobProcessorException e) {
        }
    }

    @Test
    public void send_processorResultArgIsNull_throws() throws JobProcessorException {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        try {
            jobStoreMessageProducerBean.sendProc(null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void send_createMessageWithProcessorResultPayloadThrowsJMSException_throws() throws JobProcessorException, JMSException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(textMessage);
        doThrow(new JMSException("JMSException"))
                .when(textMessage).setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        try {
            jobStoreMessageProducerBean.sendProc(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
            fail("No Exception thrown");
        } catch (JobProcessorException e) {
        }
    }

    @Test
    public void send_createMessageWithProcessorResultPayloadThrowsJsonException_throws() throws JobProcessorException, JSONBException {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        final JSONBContext jsonbContext = mock(JSONBContext.class);
        when(jobStoreMessageProducerBean.jsonBinding.getContext()).thenReturn(jsonbContext);
        when(jsonbContext.marshall(anyObject())).thenThrow(new JSONBException("JsonException"));
        try {
            jobStoreMessageProducerBean.sendProc(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
            fail("No Exception thrown");
        } catch (JobProcessorException e) {
        }
    }

    @Test
    public void createMessage_sinkResultArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JSONBException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        TextMessage message = jobStoreMessageProducerBean.createMessage(jmsContext, new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());
        assertThat("Message source property", message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat("Message payload property", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
    }

    @Test
    public void createMessage_processorResultArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JSONBException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        TextMessage message = jobStoreMessageProducerBean.createMessage(
                jmsContext, 
                new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
        assertThat("Message source property", message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat("Message payload property", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
    }

    private JobStoreMessageProducerBean getInitializedBean() {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = new JobStoreMessageProducerBean();
        jobStoreMessageProducerBean.jobStoreQueueConnectionFactory = jmsConnectionFactory;
        jobStoreMessageProducerBean.jsonBinding = Mockito.spy(new JSONBBean());
        jobStoreMessageProducerBean.jsonBinding.initialiseContext();
        return jobStoreMessageProducerBean;
    }
}
