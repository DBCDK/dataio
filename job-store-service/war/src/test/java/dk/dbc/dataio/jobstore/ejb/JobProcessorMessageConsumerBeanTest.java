package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkChunkResultJsonBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;

public class JobProcessorMessageConsumerBeanTest {
    @Test
    public void onMessage_messageArgPayloadIsInvalidProcessorResult_noTransactionRollback() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidSinkResult_noTransactionRollback() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgPayloadIsUnknown_noTransactionRollback() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, "Unknown");
        textMessage.setText("{'invalid': 'instance'}");
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalidProcessorResult_throws() throws JobStoreException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE, "{'invalid': 'instance'}");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalidSinkResult_throws() throws JobStoreException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", JmsConstants.SINK_RESULT_PAYLOAD_TYPE, "{'invalid': 'instance'}");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsUnknown_throws() throws JobStoreException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", "Unknown", "{'unknown': 'instance'}");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsValidProcessorResult_handled() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(new ChunkResultJsonBuilder().build());
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgPayloadIsValidSinkResult_handled() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(new SinkChunkResultJsonBuilder().build());
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    private TestableJobProcessorMessageConsumerBean getInitializedBean() {
        final TestableJobProcessorMessageConsumerBean jobStoreMessageConsumerBean = new TestableJobProcessorMessageConsumerBean();
        jobStoreMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        jobStoreMessageConsumerBean.jobStoreBean = mock(JobStoreBean.class);
        return jobStoreMessageConsumerBean;
    }

    private static class TestableJobProcessorMessageConsumerBean extends JobProcessorMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
    }
}
