package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
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
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JsonUtil.class,
})
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

    @Test(expected = NullPointerException.class)
    public void send_chunkResultArgIsNull_throws() throws JobProcessorException {
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        sinkMessageProducerBean.send(null, sink);
    }

    @Test(expected = NullPointerException.class)
    public void send_destinationArgIsNull_throws() throws JobProcessorException {
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        sinkMessageProducerBean.send(processedChunk, null);
    }

    @Test(expected = JobProcessorException.class)
    public void send_createMessageThrowsJsonException_throws() throws JobProcessorException, JsonException {
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(ExternalChunk.class))).thenThrow(new JsonException("JsonException"));
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        sinkMessageProducerBean.send(processedChunk, sink);
    }

    @Test
    public void createMessage_chunkResultArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JsonException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        final TextMessage message = sinkMessageProducerBean.createMessage(jmsContext, processedChunk, sink);
        assertThat(message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE));
        assertThat(message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(sink.getContent().getResource()));
    }

    private SinkMessageProducerBean getInitializedBean() {
        final SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.sinksQueueConnectionFactory = jmsConnectionFactory;
        return sinkMessageProducerBean;
    }
}
