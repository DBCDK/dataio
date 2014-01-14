package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobprocessor.dto.JobProcessorMessage;
import dk.dbc.dataio.jobprocessor.exception.InvalidMessageJobProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

@MessageDriven
public class SinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkMessageConsumerBean.class);
    private static final String DELIVERY_COUNT_PROPERTY = "JMSXDeliveryCount";

    @Resource
    MessageDrivenContext messageDrivenContext;

    /**
     * Reacts to messages received from sinks.
     *
     * A message must pass validation. Any Invalid message will be removed from the
     * message queue. For a message to be deemed valid the following invariants must be
     * upheld:
     *   <ul>
     *     <li>
     *       message must be non-null and of type TextMessage
     *     <li>
     *       message payload must be non-null and non-empty
     *     <li>
     *       message payload must represent JSON able to unmarshall to SinkChunkResult object
     *     <li>
     *       message payload must not represent empty JSON object '{}'
     *     <li>
     *       ChunkResult object must contain results
     *   </ul>
     *
     * Any exception (checked or unchecked) thrown after the validation step causes
     * the message to be put back on the queue.
     *
     * @param message message to be handled
     */
    public void onMessage(Message message) {
        String messageId = null;
        try {
            final JobProcessorMessage jobProcessorMessage = validateMessage(message);
            messageId = jobProcessorMessage.getMessageId();
            handleJobProcessorMessage(jobProcessorMessage);
        } catch (InvalidMessageJobProcessorException e) {
            LOGGER.error("Message rejected", e);
        } catch (Throwable t) {
            // Ensure that this container-managed transaction can never commit
            // and therefore that this message subsequently will be re-delivered.
            messageDrivenContext.setRollbackOnly();
            LOGGER.error("Exception caught while processing message<{}>: {}", messageId, t);
        }
    }

    /* To prevent message poisoning where invalid messages will be re-delivered
       forever, all messages must be validated */
    JobProcessorMessage validateMessage(Message message) throws InvalidMessageJobProcessorException {
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
            return new JobProcessorMessage(messageId, messagePayload);
        } catch (JMSException e) {
            throw new InvalidMessageJobProcessorException("Unexpected exception during message validation");
        }
    }

    void handleJobProcessorMessage(JobProcessorMessage jobProcessorMessage) throws InvalidMessageJobProcessorException {
        try {
            SinkChunkResult sinkChunkResult = JsonUtil.fromJson(jobProcessorMessage.getMessagePayload(), SinkChunkResult.class, MixIns.getMixIns());
            LOGGER.info("Received SinkChunkResult for jobId={}, chunkId={}", sinkChunkResult.getJobId(), sinkChunkResult.getChunkId());
        } catch (JsonException e) {
            throw new InvalidMessageJobProcessorException(String.format("Message<%s> payload was not valid SinkChunkResult type", jobProcessorMessage.getMessageId()), e);
        }
    }

}
