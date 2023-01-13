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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class AbstractMessageConsumerBeanTest {
    protected enum HandleConsumedMessageReaction {ACCEPT, INVALID, THROW}

    private static final String PAYLOAD = "{'key': 'value'}";
    private static final String PAYLOAD_TYPE = HandleConsumedMessageReaction.ACCEPT.toString();

    @Test
    public void onMessage_messageArgIsNotOfTypeTextMessage_noTransactionRollback() {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.onMessage(new NotJmsTextMessage());
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(false));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgPayloadIsNull_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.onMessage(getMockedJmsTextMessage(PAYLOAD_TYPE, null));
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(false));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgPayloadIsEmpty_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.onMessage(getMockedJmsTextMessage(PAYLOAD_TYPE, ""));
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(false));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgHasNoPayloadTypeProperty_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setText(PAYLOAD);
        messageConsumerBean.onMessage(textMessage);
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(false));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_messageArgHasEmptyPayloadTypeProperty_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.onMessage(getMockedJmsTextMessage("", PAYLOAD));
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(false));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_consumedMessageHandlingThrowsInvalidMessageException_noTransactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.onMessage(getMockedJmsTextMessage(HandleConsumedMessageReaction.INVALID.toString(), PAYLOAD));
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(true));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_consumedMessageHandlingThrowsServiceException_transactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        try {
            messageConsumerBean.onMessage(getMockedJmsTextMessage(HandleConsumedMessageReaction.THROW.toString(), PAYLOAD));
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(true));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_consumedMessageHandlingSetsRollbackOnly_transactionRollback() throws JMSException {
        final TestableMessageConsumerBean messageConsumerBean = getInitializedBean();
        messageConsumerBean.messageDrivenContext.setRollbackOnly();
        try {
            messageConsumerBean.onMessage(getMockedJmsTextMessage(HandleConsumedMessageReaction.THROW.toString(), PAYLOAD));
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
        assertThat("handleConsumedMessage() called", messageConsumerBean.handleConsumedMessageCalled, is(true));
        assertThat("rollback", messageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(true));
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

    private MockedJmsTextMessage getMockedJmsTextMessage(String payloadType, String payload) throws JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, payloadType);
        textMessage.setText(payload);
        return textMessage;
    }

    private static class TestableMessageConsumerBean extends AbstractMessageConsumerBean {
        public boolean handleConsumedMessageCalled = false;

        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }

        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }

        @Override
        public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
            handleConsumedMessageCalled = true;
            switch (HandleConsumedMessageReaction.valueOf(consumedMessage.getHeaderValue(JmsConstants.PAYLOAD_PROPERTY_NAME, String.class))) {
                case INVALID:
                    throw new InvalidMessageException("INVALID");
                case THROW:
                    throw new TestableMessageConsumerBeanException("THROWS");
            }
        }

        public static class TestableMessageConsumerBeanException extends ServiceException {
            private static final long serialVersionUID = -1908673274650168855L;

            public TestableMessageConsumerBeanException(String message) {
                super(message);
            }
        }
    }
}
