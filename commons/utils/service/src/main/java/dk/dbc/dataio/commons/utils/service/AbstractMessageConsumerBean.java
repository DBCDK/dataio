/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
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
     * that is not an InvalidMessageException result in a IllegalStateException
     * causing the message to be put back on the queue.
     *
     * @param message message received
     */
    public void onMessage(Message message) throws IllegalStateException {
        String messageId = null;
        try {
            final ConsumedMessage consumedMessage = validateMessage(message);
            messageId = consumedMessage.getMessageId();
            message.getIntProperty(DELIVERY_COUNT_PROPERTY);
            handleConsumedMessage(consumedMessage);
            if (messageDrivenContext.getRollbackOnly()) {
                throw new IllegalStateException("Message processing marked the transaction for rollback");
            }
        } catch (InvalidMessageException e) {
            LOGGER.error("Message rejected", e);
        } catch (Throwable t) {
            LOGGER.error("Transaction rollback", t);
            // Ensure that this container-managed transaction can not commit
            // and therefore that this message subsequently will be re-delivered.
            throw new IllegalStateException(String.format("Exception caught while processing message<%s>", messageId), t);
        }
    }

    /**
     * Message handler stub.
     *
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if type not legal
     * @throws ServiceException service exception
     */
    public abstract void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, ServiceException;

    public void confirmLegalChunkTypeOrThrow(ExternalChunk chunk, ExternalChunk.Type legalChunkType) throws InvalidMessageException {
        if(chunk.getType() != legalChunkType) {
            String errMsg = String.format(
                    "The chunk with id (jobId/chunkId) : [%d/%d] is of illegal type [%s] when [%s] was expected.",
                    chunk.getJobId(),
                    chunk.getChunkId(),
                    chunk.getType(),
                    legalChunkType);

            LOGGER.warn(errMsg);
            throw new InvalidMessageException(errMsg);
        }
    }
}
