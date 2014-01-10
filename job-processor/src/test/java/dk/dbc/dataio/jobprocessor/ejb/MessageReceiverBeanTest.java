package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.jms.NotJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.SinkChunkResultJsonBuilder;
import dk.dbc.dataio.jobprocessor.exception.InvalidMessageJobProcessorException;
import org.junit.Test;

import javax.jms.JMSException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MessageReceiverBeanTest {
    @Test
    public void onMessage_messageArgIsNull_noTransactionRollback() {
        final MessageReceiverBean  messageReceiverBean = getInitializedBean();
        messageReceiverBean.onMessage(null);
        assertThat(messageReceiverBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageJobProcessorException {
        getInitializedBean().validateMessage(null);
    }

    @Test
    public void onMessage_messageArgIsNotOfTypeTextMessage_noTransactionRollback() {
        final MessageReceiverBean messageReceiverBean = getInitializedBean();
        messageReceiverBean.onMessage(new NotJmsTextMessage());
        assertThat(messageReceiverBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgIsNotOfTypeTextMessage_throws() throws InvalidMessageJobProcessorException {
        getInitializedBean().validateMessage(new NotJmsTextMessage());
    }

    @Test
    public void onMessage_messageArgPayloadIsNull_noTransactionRollback() throws JMSException {
        final MessageReceiverBean messageReceiverBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        messageReceiverBean.onMessage(textMessage);
        assertThat(messageReceiverBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgPayloadIsNull_throws() throws InvalidMessageJobProcessorException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsEmpty_noTransactionRollback() throws JMSException {
        final MessageReceiverBean messageReceiverBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        messageReceiverBean.onMessage(textMessage);
        assertThat(messageReceiverBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void validateMessage_messageArgPayloadIsEmpty_throws() throws InvalidMessageJobProcessorException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void validateMessage_onValidMessage_returnsJobProcessorMessage() throws JMSException, InvalidMessageJobProcessorException {
        final String payload = "payload";
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(payload);
        MessageReceiverBean.JobProcessorMessage jobProcessorMessage = getInitializedBean().validateMessage(textMessage);
        assertThat(jobProcessorMessage.getMessageId(), is(MockedJmsTextMessage.DEFAULT_MESSAGE_ID));
        assertThat(jobProcessorMessage.getMessagePayload(), is(payload));
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidSinkChunkResult_noTransactionRollback() throws JMSException {
        final MessageReceiverBean messageReceiverBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("{'invalid': 'instance'}");
        messageReceiverBean.onMessage(textMessage);
        assertThat(messageReceiverBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageJobProcessorException.class)
    public void handleJobProcessorMessage_messageArgPayloadIsInvalidSinkChunkResult_throws() throws InvalidMessageJobProcessorException, JMSException {
        final MessageReceiverBean.JobProcessorMessage jobProcessorMessage = new MessageReceiverBean.JobProcessorMessage("id", "{'invalid': 'instance'}");
        getInitializedBean().handleJobProcessorMessage(jobProcessorMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsValidSinkChunkResult_handled() throws JMSException {
        final MessageReceiverBean messageReceiverBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(new SinkChunkResultJsonBuilder().build());
        messageReceiverBean.onMessage(textMessage);
        assertThat(messageReceiverBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    private MessageReceiverBean getInitializedBean() {
        final MessageReceiverBean messageReceiverBean = new MessageReceiverBean();
        messageReceiverBean.messageDrivenContext = new MockedJmsMessageDrivenContext();
        return messageReceiverBean;
    }
}


