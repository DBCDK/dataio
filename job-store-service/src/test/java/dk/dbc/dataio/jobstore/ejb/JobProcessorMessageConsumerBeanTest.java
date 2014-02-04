package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.jms.NotJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;

public class JobProcessorMessageConsumerBeanTest {
    @Test
    public void onMessage_messageArgIsNull_noTransactionRollback() {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        jobProcessorMessageConsumerBean.onMessage(null);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageException {
        getInitializedBean().validateMessage(null);
    }

    @Test
    public void onMessage_messageArgIsNotOfTypeTextMessage_noTransactionRollback() {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        jobProcessorMessageConsumerBean.onMessage(new NotJmsTextMessage());
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgIsNotOfTypeTextMessage_throws() throws InvalidMessageException {
        getInitializedBean().validateMessage(new NotJmsTextMessage());
    }

    @Test
    public void onMessage_messageArgPayloadIsNull_noTransactionRollback() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgPayloadIsNull_throws() throws InvalidMessageException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsEmpty_noTransactionRollback() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgPayloadIsEmpty_throws() throws InvalidMessageException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void validateMessage_onValidMessage_returnsConsumedMessage() throws JMSException, InvalidMessageException {
        final String payload = "payload";
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(payload);
        final ConsumedMessage consumedMessage = getInitializedBean().validateMessage(textMessage);
        assertThat(consumedMessage.getMessageId(), is(MockedJmsTextMessage.DEFAULT_MESSAGE_ID));
        assertThat(consumedMessage.getMessagePayload(), is(payload));
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidNewJob_noTransactionRollback() throws JMSException {
        final TestableJobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("{'invalid': 'instance'}");
        jobProcessorMessageConsumerBean.onMessage(textMessage);
        assertThat(jobProcessorMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalidNewJob_throws() throws JobStoreException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", "{'invalid': 'instance'}");
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

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messagePayloadCanNotBeUnmarshalledToJson_throws() throws JobStoreException, InvalidMessageException {
        final JobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = getInitializedBean();
        final ConsumedMessage message = new ConsumedMessage("id", "invalid");
        jobProcessorMessageConsumerBean.handleConsumedMessage(message);
    }

    private TestableJobProcessorMessageConsumerBean getInitializedBean() {
        final TestableJobProcessorMessageConsumerBean jobStoreMessageConsumerBean = new TestableJobProcessorMessageConsumerBean();
        jobStoreMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        jobStoreMessageConsumerBean.jobStore = mock(JobStoreBean.class);
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
