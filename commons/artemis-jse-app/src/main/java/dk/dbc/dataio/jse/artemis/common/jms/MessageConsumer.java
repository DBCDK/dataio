package dk.dbc.dataio.jse.artemis.common.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.jse.artemis.common.Metric;
import dk.dbc.dataio.jse.artemis.common.service.ZombieWatch;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.rejected;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.rollback;

public interface MessageConsumer extends MessageListener {
    Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);
    String DELIVERY_COUNT_PROPERTY = "JMSXDeliveryCount";
    AtomicInteger RUNNING_TRANSACTIONS = new AtomicInteger(0);
    AtomicLong LAST_MESSAGE_TS = new AtomicLong(System.currentTimeMillis());
    ObjectMapper MAPPER = new ObjectMapper();

    default void initMetrics(MetricRegistry metricRegistry) {
        metricRegistry.gauge("dataio_running_transactions", RUNNING_TRANSACTIONS::get);
        metricRegistry.gauge("dataio_time_since_last_message_ms",this::getTimeSinceLastMessage);
    }

    default ConsumedMessage validateMessage(Message message) throws InvalidMessageException {
        if (message == null) {
            throw new InvalidMessageException("Message can not be null");
        }
        try {
            String messageId = message.getJMSMessageID();
            LOGGER.debug("Validating message<{}> with deliveryCount={}", messageId, message.getIntProperty(DELIVERY_COUNT_PROPERTY));
            if (!(message instanceof TextMessage)) {
                throw new InvalidMessageException(String.format("Message<%s> was not of type TextMessage", messageId));
            }
            String messagePayload = ((TextMessage) message).getText();
            if (messagePayload == null) {
                throw new InvalidMessageException(String.format("Message<%s> payload was null", messageId));
            }
            if (messagePayload.isEmpty()) {
                throw new InvalidMessageException(String.format("Message<%s> payload is empty string", messageId));
            }
            String payloadType = JMSHeader.payload.getHeader(message);
            if (payloadType == null || payloadType.trim().isEmpty()) {
                throw new InvalidMessageException(String.format("Message <%s> has no %s property", messageId, payloadType));
            }
            return new ConsumedMessage(messageId, getHeaders(message), messagePayload, Priority.of(message.getJMSPriority()));
        } catch (JMSException e) {
            throw new InvalidMessageException("Unexpected exception during message validation");
        }
    }

    default void onMessage(Message message) {
        RUNNING_TRANSACTIONS.incrementAndGet();
        Instant startTime = Instant.now();
        LAST_MESSAGE_TS.set(startTime.toEpochMilli());
        getZombieWatch().update(getAddress(), getQueue(), getFilter());
        String messageId = null;
        List<Tag> tags = new ArrayList<>();
        try {
            messageId = message.getJMSMessageID();
            tags.add(new Tag("destination", getAddress() + "::" + getQueue()));
            tags.add(new Tag("redelivery", Boolean.toString(message.getJMSRedelivered())));
            ConsumedMessage consumedMessage = validateMessage(message);
            handleConsumedMessage(consumedMessage);
        } catch (InvalidMessageException e) {
            tags.add(rejected.is("true"));
            LOGGER.warn("Message {} discarded", messageId, e);
        } catch (RuntimeException | JMSException re) {
            tags.add(rollback.is("true"));
            throw new IllegalStateException("Caught exception while handling message " + messageId + " rolling back", re);
        } finally {
            Tag[] tagArray = tags.toArray(Tag[]::new);
            Metric.dataio_message_count.counter(tagArray).inc();
            Metric.dataio_message_time.simpleTimer(tagArray).update(Duration.between(startTime, Instant.now()));
            RUNNING_TRANSACTIONS.decrementAndGet();
        }
    }

    default void confirmLegalChunkTypeOrThrow(Chunk chunk, Chunk.Type legalChunkType) throws InvalidMessageException {
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

    default Chunk unmarshallPayload(ConsumedMessage consumedMessage) throws NullPointerException, InvalidMessageException {
        String payloadType = JMSHeader.payload.getHeader(consumedMessage, String.class);
        if (!JMSHeader.CHUNK_PAYLOAD_TYPE.equals(payloadType)) {
            throw new InvalidMessageException(String.format("Message.headers<%s> payload type %s != %s", consumedMessage.getMessageId(), payloadType, JMSHeader.CHUNK_PAYLOAD_TYPE));
        }
        Chunk processedChunk;
        try {
            processedChunk = MAPPER.readValue(consumedMessage.getMessagePayload(), Chunk.class);
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type", consumedMessage.getMessageId()), e);
        }
        if (processedChunk.isEmpty()) {
            throw new InvalidMessageException(String.format("Message<%s> processed chunk payload contains no results", consumedMessage.getMessageId()));
        }
        confirmLegalChunkTypeOrThrow(processedChunk, Chunk.Type.PROCESSED);
        return processedChunk;
    }

    void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException;

    private Map<String, Object> getHeaders(Message message) throws JMSException {
        Map<String, Object> headers = new HashMap<>();
        Enumeration<?> messagePropertyNames = message.getPropertyNames();
        while (messagePropertyNames.hasMoreElements()) {
            String propertyName = (String) messagePropertyNames.nextElement();
            headers.put(propertyName, message.getObjectProperty(propertyName));
        }
        return headers;
    }

    default long getTimeSinceLastMessage() {
        return System.currentTimeMillis() - LAST_MESSAGE_TS.get();
    }

    String getQueue();

    String getAddress();

    default String getFilter() {
        return null;
    }

    ZombieWatch getZombieWatch();
}
