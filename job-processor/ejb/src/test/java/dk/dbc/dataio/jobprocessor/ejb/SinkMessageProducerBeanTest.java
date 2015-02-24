package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SinkMessageProducerBeanTest {
    private ConnectionFactory jmsConnectionFactory;
    private JMSContext jmsContext;
    private JMSProducer jmsProducer;
    private ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();
    private Sink sink = new SinkBuilder().build();

    @Before
    public void setup() {
        jmsConnectionFactory = mock(ConnectionFactory.class);
        jmsContext = mock(JMSContext.class);
        jmsProducer = mock(JMSProducer.class);

        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @Test
    public void send_processedChunkArgIsNull_throws() throws JobProcessorException {
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        try {
            sinkMessageProducerBean.send(null, sink);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void send_destinationArgIsNull_throws() throws JobProcessorException {
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        try {
            sinkMessageProducerBean.send(processedChunk, null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void send_createMessageThrowsJsonException_throws() throws JobProcessorException, JSONBException {
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        final JSONBContext jsonbContext = mock(JSONBContext.class);
        when(sinkMessageProducerBean.jsonBinding.getContext()).thenReturn(jsonbContext);
        when(jsonbContext.marshall(anyObject())).thenThrow(new JSONBException("JsonException"));
        try {
            sinkMessageProducerBean.send(processedChunk, sink);
            fail("No Exception thrown");
        } catch (JobProcessorException e) {
        }
    }

    @Test
    public void createMessage_deliveredChunkArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JSONBException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        final TextMessage message = sinkMessageProducerBean.createMessage(jmsContext, processedChunk, sink);
        assertThat("Message source property", message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat("Message payload property", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE));
        assertThat("Message resource property", message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(sink.getContent().getResource()));
    }

    private SinkMessageProducerBean getInitializedBean() {
        final SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.sinksQueueConnectionFactory = jmsConnectionFactory;
        sinkMessageProducerBean.jsonBinding = Mockito.spy(new JSONBBean());
        sinkMessageProducerBean.jsonBinding.initialiseContext();
        return sinkMessageProducerBean;
    }
}
