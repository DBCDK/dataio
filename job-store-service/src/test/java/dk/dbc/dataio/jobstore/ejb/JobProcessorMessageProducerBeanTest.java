package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.NewJobBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JsonUtil.class,})
public class JobProcessorMessageProducerBeanTest {

    private ConnectionFactory jmsConnectionFactory;
    private JMSContext jmsContext;
    private JMSProducer jmsProducer;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        jmsConnectionFactory = mock(ConnectionFactory.class);
        jmsContext = mock(JMSContext.class);
        jmsProducer = mock(JMSProducer.class);

        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @Test(expected = NullPointerException.class)
    public void send_newJobArgIsNull_throws() throws JobStoreException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        jobProcessorMessageProducerBean.send(null);
    }

    @Test
    public void send_createMessageThrowsJsonException_throws() throws JsonException, JobStoreException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        mockStatic(JsonUtil.class);
        when(JsonUtil.toJson(any(NewJob.class))).thenThrow(new JsonException("JsonException"));
        NewJob newJob = new NewJobBuilder().build();

        exception.expect(JobStoreException.class);
        // todo: experiment with com.googlecode.catchexception.CatchException
        // see: http://code.google.com/p/catch-exception/
        jobProcessorMessageProducerBean.send(newJob);
    }

    @Test
    public void createMessage_NewJobArgIsValid_returnsMessageWithHeaderProperties() throws JsonException, JMSException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        TextMessage message = jobProcessorMessageProducerBean.createMessage(jmsContext, new NewJobBuilder().build());
        assertThat(message.getStringProperty("source"), is("jobstore"));
        assertThat(message.getStringProperty("payload"), is("NewJob"));
    }

    private JobProcessorMessageProducerBean getInitializedBean() {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = new JobProcessorMessageProducerBean();
        jobProcessorMessageProducerBean.processorQueueConnectionFactory = jmsConnectionFactory;
        return jobProcessorMessageProducerBean;
    }
}
