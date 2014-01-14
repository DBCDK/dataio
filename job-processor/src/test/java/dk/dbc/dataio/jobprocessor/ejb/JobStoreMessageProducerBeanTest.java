package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import org.junit.Before;
import org.junit.Test;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class JobStoreMessageProducerBeanTest {
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

    @Test(expected = NullPointerException.class)
    public void relay_sinkChunkResultArgIsNull_throws() {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        jobStoreMessageProducerBean.relay(null);
    }

    @Test
    public void createMessage_sinkChunkResultArgIsValid_returnsMessageWithChunkResultSourceProperty() throws JMSException, JsonException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = getInitializedBean();
        TextMessage message = jobStoreMessageProducerBean.createMessage(jmsContext, new SinkChunkResultBuilder().build());
        assertThat(message.getStringProperty("chunkResultSource"), is(JobStoreMessageProducerBean.CHUNK_RESULT_SOURCE_PROPERTY));
    }

    private JobStoreMessageProducerBean getInitializedBean() {
        final JobStoreMessageProducerBean jobStoreMessageProducerBean = new JobStoreMessageProducerBean();
        jobStoreMessageProducerBean.jobStoreQueueConnectionFactory = jmsConnectionFactory;
        return jobStoreMessageProducerBean;
    }
}
