package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.jms.NotJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.SinkChunkResultJsonBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobStoreMessageConsumerBeanTest {
    @Test
    public void onMessage_messageArgIsNull_noTransactionRollback() {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        jobStoreMessageConsumerBean.onMessage(null);
        assertThat(jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageException {
        getInitializedBean().validateMessage(null);
    }

    @Test
    public void onMessage_messageArgIsNotOfTypeTextMessage_noTransactionRollback() {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        jobStoreMessageConsumerBean.onMessage(new NotJmsTextMessage());
        assertThat(jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgIsNotOfTypeTextMessage_throws() throws InvalidMessageException {
        getInitializedBean().validateMessage(new NotJmsTextMessage());
    }

    @Test
    public void onMessage_messageArgPayloadIsNull_noTransactionRollback() throws JMSException {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        jobStoreMessageConsumerBean.onMessage(textMessage);
        assertThat(jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgPayloadIsNull_throws() throws InvalidMessageException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsEmpty_noTransactionRollback() throws JMSException {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        jobStoreMessageConsumerBean.onMessage(textMessage);
        assertThat(jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
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
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("{'invalid': 'instance'}");
        jobStoreMessageConsumerBean.onMessage(textMessage);
        assertThat(jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalidNewJob_throws() throws JobProcessorException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", "{'invalid': 'instance'}");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsValidNewJob_handled() throws JMSException {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(new SinkChunkResultJsonBuilder().build());
        jobStoreMessageConsumerBean.onMessage(textMessage);
        assertThat(jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messagePayloadCanNotBeUnmarshalledToJson_throws() throws JobProcessorException, InvalidMessageException {
        final JobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final ConsumedMessage message = new ConsumedMessage("id", "invalid");
        jobStoreMessageConsumerBean.handleConsumedMessage(message);
    }

    private TestableJobStoreMessageConsumerBean getInitializedBean() {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = new TestableJobStoreMessageConsumerBean();
        jobStoreMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        return jobStoreMessageConsumerBean;
    }

    private static class TestableJobStoreMessageConsumerBean extends JobStoreMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
    }
}


