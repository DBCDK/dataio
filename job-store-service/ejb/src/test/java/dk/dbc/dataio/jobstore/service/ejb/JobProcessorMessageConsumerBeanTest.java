package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.powermock.api.mockito.PowerMockito.mock;


public class JobProcessorMessageConsumerBeanTest {

    private TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean;

    @Before
    public void setup() throws URISyntaxException {
        initializeJobProcessorMessageConsumerBean();
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalid_noTransactionRollback() throws JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalid_throws() throws JobStoreException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, "{'invalid': 'instance'}");
        jobProcessorMessageConsumerBean.handleConsumedMessage(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsUnknown_throws() throws JobStoreException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", "Unknown", "{'unknown': 'instance'}");
        jobProcessorMessageConsumerBean.handleConsumedMessage(consumedMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsValidProcessorResult_handled() throws JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    private static class TestableJobProcessorMessageConsumerBean extends JobProcessorMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
    }

    private void initializeJobProcessorMessageConsumerBean() {
        jobProcessorMessageConsumerBean = new TestableJobProcessorMessageConsumerBean();
        jobProcessorMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        jobProcessorMessageConsumerBean.jobStoreBean = mock(PgJobStore.class);
    }
}
