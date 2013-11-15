package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.sink.InvalidMessageSinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

@MessageDriven
public class EsMessageProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    @Resource
    MessageDrivenContext messageDrivenContext;

    @EJB
    EsThrottlerBean esThrottler;

    /**
     * Extracts ChunkResult object from message causing a Task Package to be generated
     * in the ES database.
     *
     * The message must pass validation. Any Invalid message will be removed from the
     * message queue. For a message to be deemed valid the following invariants must be
     * upheld:
     *   <ul>
     *     <li>
     *       message must be non-null and of type TextMessage
     *     <li>
     *       message payload must be non-null and non-empty
     *     <li>
     *       message payload must represent JSON able to unmarshall to ChunkResult object
     *     <li>
     *       message payload must not represent empty JSON object '{}'
     *     <li>
     *       ChunkResult object must contain results
     *   </ul>
     *
     * Any exception (checked or unchecked) thrown after the validation step causes
     * the message to be put back on the queue.
     *
     * @param message message to be processed.

     */
    public void onMessage(Message message) {
        String messageId = null;
        try {
            final ChunkResult chunkResult = validateMessage(message);
            messageId = message.getJMSMessageID();
            processChunkResult(chunkResult);
        } catch (InvalidMessageSinkException e) {
            LOGGER.error("Message rejected", e);
        } catch (InterruptedException | JMSException | RuntimeException e) {
            // Ensure that this container-managed transaction can never commit
            // and therefore that this message subsequently will be re-delivered.
            messageDrivenContext.setRollbackOnly();
            LOGGER.error("Exception caught while processing message<{}>: {}", messageId, e);
        }
    }

    /* To prevent message poisoning where invalid messages will be re-delivered
       forever, all messages must be validated */
    ChunkResult validateMessage(Message message) throws InvalidMessageSinkException {
        if (message == null) {
            throw new InvalidMessageSinkException("Message can not be null");
        }

        ChunkResult chunkResult;
        try {
            final String messageId = message.getJMSMessageID();
            LOGGER.info("Validating message<{}> with deliveryCount={}", messageId, message.getIntProperty("JMSXDeliveryCount"));
            if (!(message instanceof TextMessage)) {
                throw new InvalidMessageSinkException(String.format("Message<%s> was not of type TextMessage", messageId));
            }
            chunkResult = validateMessagePayload(messageId, ((TextMessage) message).getText());

        } catch (JMSException e) {
            throw new InvalidMessageSinkException("Unexpected exception during message validation");
        }
        return chunkResult;
    }

    private ChunkResult validateMessagePayload(String messageId, String messagePayload) throws JMSException, InvalidMessageSinkException {
        ChunkResult chunkResult;
        if (messagePayload == null) {
            throw new InvalidMessageSinkException(String.format("Message<%s> payload was null", messageId));
        }
        if (messagePayload.isEmpty()) {
            throw new InvalidMessageSinkException(String.format("Message<%s> payload is empty string", messageId));
        }
        try {
            chunkResult = JsonUtil.fromJson(messagePayload, ChunkResult.class, MixIns.getMixIns());
        } catch (JsonException e) {
            throw new InvalidMessageSinkException(String.format("Message<%s> payload was not valid ChunkResult type: %s", messageId, e));
        }
        if (chunkResult.getResults().isEmpty()) {
            throw new InvalidMessageSinkException(String.format("Message<%s> ChunkResult payload contains no results: payload='%s'", messageId, messagePayload));
        }
        return chunkResult;
    }

    void processChunkResult(ChunkResult chunkResult) throws InterruptedException, JMSException {
        esThrottler.acquireRecordSlots(chunkResult.getResults().size());
    }
}
