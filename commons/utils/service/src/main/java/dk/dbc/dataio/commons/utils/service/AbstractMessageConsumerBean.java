package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.MessageDrivenContext;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static dk.dbc.dataio.commons.types.jms.JMSHeader.chunkId;
import static dk.dbc.dataio.commons.types.jms.JMSHeader.jobId;
import static dk.dbc.dataio.commons.types.jms.JMSHeader.trackingId;

@LocalBean
public abstract class AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageConsumerBean.class);
    private static final String DELIVERY_COUNT_PROPERTY = "JMSXDeliveryCount";
    @Resource
    protected MessageDrivenContext messageDrivenContext;
    @Inject
    private MetricRegistry metricRegistry;
    protected static final AtomicInteger RUNNING_TRANSACTIONS = new AtomicInteger(0);
    protected static final AtomicLong LAST_MESSAGE_TS = new AtomicLong(System.currentTimeMillis());

    @PostConstruct
    @SuppressWarnings("PMD")
    private void initMetrics() {
        if(metricRegistry != null) metricRegistry.gauge("dataio_running_transactions", RUNNING_TRANSACTIONS::get);
        if(metricRegistry != null) metricRegistry.gauge("dataio_time_since_last_message_ms",this::getTimeSinceLastMessage);
    }

    /**
     * Message validation.
     * <p>
     * For a message to be deemed valid the following invariants must be
     * upheld:
     * <ul>
     *   <li>
     *     message must be non-null and of type TextMessage
     *   <li>
     *     message payload must be non-null and non-empty
     *   <li>
     *     message must must have a non-null and non-empty JmsConstants.PAYLOAD_PROPERTY_NAME header property
     * </ul>
     *
     * @param message message to be validated
     * @return message as ConsumedMessage instance
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
            return new ConsumedMessage(messageId, getHeaders(message), messagePayload,
                    Priority.of(message.getJMSPriority()));
        } catch (JMSException e) {
            throw new InvalidMessageException("Unexpected exception during message validation");
        }
    }

    /**
     * Callback for all messages received.
     * <p>
     * To prevent message poisoning where invalid messages will be re-delivered
     * forever, a message will first be validated with the validateMessage() method.
     * Any Invalid message will be removed from the message queue.
     * <p>
     * Subsequently the message is handled by calling the handleConsumedMessage()
     * method. Any exception (checked or unchecked) thrown after the validation step
     * that is not an InvalidMessageException result in a IllegalStateException
     * causing the message to be put back on the queue.
     *
     * @param message message received
     */
    public void onMessage(Message message) throws IllegalStateException {
        RUNNING_TRANSACTIONS.incrementAndGet();
        Instant startTime = Instant.now();
        LAST_MESSAGE_TS.set(startTime.toEpochMilli());
        String messageId = null;
        List<Tag> tags = new ArrayList<>();
        try {
            tags.add(new Tag("destination", Optional.ofNullable(message.getJMSDestination()).map(Object::toString).orElse("none")));
            tags.add(new Tag("redelivery", Boolean.toString(message.getJMSRedelivered())));
            final ConsumedMessage consumedMessage = validateMessage(message);
            messageId = consumedMessage.getMessageId();
            LOGGER.info("Received chunk {}/{} with uid: {}", jobId.getHeader(message), chunkId.getHeader(message), trackingId.getHeader(message));
            message.getIntProperty(DELIVERY_COUNT_PROPERTY);
            handleConsumedMessage(consumedMessage);
            if (messageDrivenContext.getRollbackOnly()) {
                throw new IllegalStateException("Message processing marked the transaction for rollback");
            }
        } catch (InvalidMessageException e) {
            LOGGER.error("Message rejected", e);
            tags.add(new Tag("rejected", "true"));

        } catch (Throwable t) {
            tags.add(new Tag("rollback", "true"));
            LOGGER.error("Transaction rollback", t);
            // Ensure that this container-managed transaction can not commit
            // and therefore that this message subsequently will be re-delivered.
            throw new IllegalStateException(String.format("Exception caught while processing message<%s>", messageId), t);
        } finally {
            Tag[] tagArray = tags.toArray(Tag[]::new);
            if(metricRegistry != null) {
                metricRegistry.counter("dataio_message_count", tagArray).inc();
                metricRegistry.timer("dataio_message_time", tagArray).update(Duration.between(startTime, Instant.now()));
            }
            RUNNING_TRANSACTIONS.decrementAndGet();
        }
    }

    public long getTimeSinceLastMessage() {
        return System.currentTimeMillis() - LAST_MESSAGE_TS.get();
    }

    /**
     * Message handler stub.
     *
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if type not legal
     * @throws ServiceException        service exception
     */
    public abstract void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, ServiceException;

    public void confirmLegalChunkTypeOrThrow(Chunk chunk, Chunk.Type legalChunkType) throws InvalidMessageException {
        if (chunk.getType() != legalChunkType) {
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

    /*
     * Private methods
     */

    /**
     * Extracts all headers from the message given as input
     *
     * @param message input message
     * @return map containing extracted headers
     * @throws JMSException on failure to extract property names and values
     */
    private Map<String, Object> getHeaders(Message message) throws JMSException {
        final Map<String, Object> headers = new HashMap<>();
        final Enumeration messagePropertyNames = message.getPropertyNames();
        while (messagePropertyNames.hasMoreElements()) {
            String propertyName = (String) messagePropertyNames.nextElement();
            headers.put(propertyName, message.getObjectProperty(propertyName));
        }
        return headers;
    }
}
