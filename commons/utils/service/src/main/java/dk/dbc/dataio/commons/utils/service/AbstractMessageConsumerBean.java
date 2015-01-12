package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
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
    private static final long EXPONENTIAL_BACKOFF_CUTOFF_POINT_IN_MS = 3600000; // 1 hour

    @Resource
    protected MessageDrivenContext messageDrivenContext;

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
     *     <li>
     *       message must must have a non-null and non-empty JmsConstants.PAYLOAD_PROPERTY_NAME header property
     *   </ul>
     *
     * @param message message to be validated
     *
     * @return message as ConsumedMessage instance
     *
     * @throws InvalidMessageException when message fails to validate
     */
    public ConsumedMessage validateMessage(Message message) throws InvalidMessageException {
        if (message == null) {
            throw new InvalidMessageException("Message can not be null");
        }
        try {
            final String messageId = message.getJMSMessageID();
            LOGGER.info("Validating message<{}> with deliveryCount={}", messageId, message.getIntProperty(DELIVERY_COUNT_PROPERTY));
            if (!(message instanceof TextMessage)) {
                throw new InvalidMessageException(String.format("Message<%s> was not of type TextMessage", messageId));
            }
            final String messagePayload = ((TextMessage) message).getText();
            if (messagePayload == null) {
                throw new InvalidMessageException(String.format("Message<%s> payload was null", messageId));
            }
            if (messagePayload.isEmpty()) {
                throw new InvalidMessageException(String.format("Message<%s> payload is empty string", messageId));
            }
            final String payloadType = message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME);
            if (payloadType == null || payloadType.trim().isEmpty()) {
                throw new InvalidMessageException(String.format("Message<%s> has no %s property", messageId, JmsConstants.PAYLOAD_PROPERTY_NAME));
            }
            return new ConsumedMessage(messageId, payloadType, messagePayload);
        } catch (JMSException e) {
            throw new InvalidMessageException("Unexpected exception during message validation");
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
     * back on the queue after a suitable wait period handled by the backoffBeforeRetry
     * method.
     *
     * @param message message received
     */
    public void onMessage(Message message) {
        String messageId = null;
        int messageDeliveryCount = 0;
        try {
            final ConsumedMessage consumedMessage = validateMessage(message);
            messageId = consumedMessage.getMessageId();
            messageDeliveryCount = message.getIntProperty(DELIVERY_COUNT_PROPERTY);
            handleConsumedMessage(consumedMessage);
        } catch (InvalidMessageException e) {
            LOGGER.error("Message rejected", e);
        } catch (Throwable t) {
            // Ensure that this container-managed transaction can never commit
            // and therefore that this message subsequently will be re-delivered.
            messageDrivenContext.setRollbackOnly();
            LOGGER.error("Exception caught while processing message<{}>", messageId, t);
            backoffBeforeRetry(messageDeliveryCount);
        }
    }

    /**
     * Message handler stub.
     *
     * @param consumedMessage message to be handled
     *
     * @throws InvalidMessageException
     * @throws ServiceException
     */
    public abstract void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException;

    /**
     * Performs default exponential backoff wait up to a certain cutoff point
     * (currently one hour) in which case the wait period is made constant
     * @param deliveryCount number of times message has been re-delivered
     */
    public void backoffBeforeRetry(int deliveryCount) {
        // Calculate the next wait interval, in milliseconds, using an exponential
        // backoff algorithm.
        long waitTime = (long) Math.pow(2, deliveryCount) * 100L;
        if (waitTime > EXPONENTIAL_BACKOFF_CUTOFF_POINT_IN_MS) {
            waitTime = EXPONENTIAL_BACKOFF_CUTOFF_POINT_IN_MS;
        }
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted while backing off for failure retry");
        }
    }
}
