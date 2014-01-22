package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.jobprocessor.dto.ConsumedMessage;
import dk.dbc.dataio.jobprocessor.exception.InvalidMessageJobProcessorException;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public abstract class AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageConsumerBean.class);
    private static final String DELIVERY_COUNT_PROPERTY = "JMSXDeliveryCount";

    @Resource
    MessageDrivenContext messageDrivenContext;

    /**
     * Message validation.
     *
     * For a message to be deemed valid the following invariants must be
     * upheld:
     *   <ul>
     *     <li>
     *       message must be non-null and of type TextMessage
     *     <li>
     *       message payload must be non-null and non-empty
     *   </ul>
     *
     * @param message message to be validated
     *
     * @return message as ConsumedMessage instance
     *
     * @throws InvalidMessageJobProcessorException when massage fails to validate
     */
    protected ConsumedMessage validateMessage(Message message) throws InvalidMessageJobProcessorException {
        if (message == null) {
            throw new InvalidMessageJobProcessorException("Message can not be null");
        }
        try {
            final String messageId = message.getJMSMessageID();
            LOGGER.info("Validating message<{}> with deliveryCount={}", messageId, message.getIntProperty(DELIVERY_COUNT_PROPERTY));
            if (!(message instanceof TextMessage)) {
                throw new InvalidMessageJobProcessorException(String.format("Message<%s> was not of type TextMessage", messageId));
            }
            final String messagePayload = ((TextMessage) message).getText();
            if (messagePayload == null) {
                throw new InvalidMessageJobProcessorException(String.format("Message<%s> payload was null", messageId));
            }
            if (messagePayload.isEmpty()) {
                throw new InvalidMessageJobProcessorException(String.format("Message<%s> payload is empty string", messageId));
            }
            return new ConsumedMessage(messageId, messagePayload);
        } catch (JMSException e) {
            throw new InvalidMessageJobProcessorException("Unexpected exception during message validation");
        }
    }

    /**
     * Callback for all messages received.
     *
     * To prevent message poisoning where invalid messages will be re-delivered
     * forever, a message will first be validated with the validateMessage() method.
     * Any Invalid message will be removed from the message queue.
     *
     * Subsequently the message is handled by calling the handleConsumedMessage()
     * method. Any exception (checked or unchecked) thrown after the validation step
     * that is not an InvalidMessageJobProcessorException causes the message to be put
     * back on the queue.
     *
     * @param message message received
     */
    public void onMessage(Message message) {
        String messageId = null;
        try {
            final ConsumedMessage consumedMessage = validateMessage(message);
            messageId = consumedMessage.getMessageId();
            handleConsumedMessage(consumedMessage);
        } catch (InvalidMessageJobProcessorException e) {
            LOGGER.error("Message rejected", e);
        } catch (Throwable t) {
            // Ensure that this container-managed transaction can never commit
            // and therefore that this message subsequently will be re-delivered.
            messageDrivenContext.setRollbackOnly();
            LOGGER.error("Exception caught while processing message<{}>: {}", messageId, t);
        }
    }

    /**
     * Message handler stub.
     *
     * @param consumedMessage message to be handled
     *
     * @throws JobProcessorException
     */
    protected void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobProcessorException {
        throw new UnsupportedOperationException();
    }
}
