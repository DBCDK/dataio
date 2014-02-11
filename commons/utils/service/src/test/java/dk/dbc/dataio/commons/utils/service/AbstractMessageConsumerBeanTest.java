package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.jms.NotJmsTextMessage;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AbstractMessageConsumerBeanTest {
    @Test
    public void onMessage_messageArgIsNull_noTransactionRollback() {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.onMessage(null);
        assertThat(messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgIsNotOfTypeTextMessage_noTransactionRollback() {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.onMessage(new NotJmsTextMessage());
        assertThat(messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgPayloadIsNull_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        messageConsumerBean.onMessage(textMessage);
        assertThat(messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgPayloadIsEmpty_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        messageConsumerBean.onMessage(textMessage);
        assertThat(messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgHasNoPayloadTypeProperty_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("{'key': 'value'}");
        messageConsumerBean.onMessage(textMessage);
        assertThat(messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgHasEmptyPayloadTypeProperty_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, "   ");
        textMessage.setText("{'key': 'value'}");
        messageConsumerBean.onMessage(textMessage);
        assertThat(messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageException {
        getInitializedBean().validateMessage(null);
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgIsNotOfTypeTextMessage_throws() throws InvalidMessageException {
        getInitializedBean().validateMessage(new NotJmsTextMessage());
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgPayloadIsNull_throws() throws InvalidMessageException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(null);
        getInitializedBean().validateMessage(textMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgPayloadIsEmpty_throws() throws InvalidMessageException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgHasNoPayloadTypeProperty_throws() throws InvalidMessageException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText("{'key': 'value'}");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void validateMessage_messageArgHasEmptyPayloadTypeProperty_throws() throws InvalidMessageException, JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, "  ");
        textMessage.setText("{'key': 'value'}");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void validateMessage_onValidMessage_returnsConsumedMessage() throws JMSException, InvalidMessageException {
        final String payload = "payload";
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, "Payload");
        textMessage.setText(payload);
        final ConsumedMessage consumedMessage = getInitializedBean().validateMessage(textMessage);
        assertThat(consumedMessage.getMessageId(), is(MockedJmsTextMessage.DEFAULT_MESSAGE_ID));
        assertThat(consumedMessage.getMessagePayload(), is(payload));
    }

    private TestableMessageConsumerBean getInitializedBean() {
        final TestableMessageConsumerBean messageConsumerBean = new TestableMessageConsumerBean();
        messageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        return messageConsumerBean;
    }

    private static class TestableMessageConsumerBean extends AbstractMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }

        @Override
        public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        }
    }
}
