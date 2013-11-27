package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.sink.InvalidMessageSinkException;
import dk.dbc.dataio.sink.SinkException;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.IOException;

@MessageDriven
public class EsMessageProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    private static final String DELIVERY_COUNT_PROPERTY = "JMSXDeliveryCount";

    @Resource
    MessageDrivenContext messageDrivenContext;

    @EJB
    EsThrottlerBean esThrottler;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    ESTaskPackageInserterBean esTaskPackageInserter;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsSinkConfigurationBean configuration;

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
        int messageDeliveryCount = 0;

        try {
            final EsWorkload workload = validateMessage(message);
            messageId = message.getJMSMessageID();
            messageDeliveryCount = message.getIntProperty(DELIVERY_COUNT_PROPERTY);
            processWorkload(workload);
        } catch (InvalidMessageSinkException e) {
            LOGGER.error("Message rejected", e);
        } catch (Throwable t) {
            // Ensure that this container-managed transaction can never commit
            // and therefore that this message subsequently will be re-delivered.
            messageDrivenContext.setRollbackOnly();
            LOGGER.error("Exception caught while processing message<{}>: {}", messageId, t);
            backoffOnFailureRetry(messageDeliveryCount);
        }
    }

    /* To prevent message poisoning where invalid messages will be re-delivered
       forever, all messages must be validated */
    EsWorkload validateMessage(Message message) throws InvalidMessageSinkException {
        if (message == null) {
            throw new InvalidMessageSinkException("Message can not be null");
        }

        final EsWorkload workload;
        try {
            final String messageId = message.getJMSMessageID();
            LOGGER.info("Validating message<{}> with deliveryCount={}", messageId, message.getIntProperty(DELIVERY_COUNT_PROPERTY));
            if (!(message instanceof TextMessage)) {
                throw new InvalidMessageSinkException(String.format("Message<%s> was not of type TextMessage", messageId));
            }
            workload = validateMessagePayload(messageId, ((TextMessage) message).getText());

        } catch (JMSException e) {
            throw new InvalidMessageSinkException("Unexpected exception during message validation");
        }
        return workload;
    }

    private EsWorkload validateMessagePayload(String messageId, String messagePayload) throws JMSException, InvalidMessageSinkException {
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
            throw new InvalidMessageSinkException(String.format("Message<%s> payload was not valid ChunkResult type", messageId), e);
        }
        return validateChunkResult(messageId, chunkResult);
    }

    private EsWorkload validateChunkResult(String messageId, ChunkResult chunkResult) throws InvalidMessageSinkException {
        if (chunkResult.getResults().isEmpty()) {
            throw new InvalidMessageSinkException(String.format("Message<%s> ChunkResult payload contains no results", messageId));
        }
        try {
            return new EsWorkload(chunkResult, esTaskPackageInserter.getAddiRecordsFromChunk(chunkResult));
        } catch (RuntimeException | IOException e) {
            throw new InvalidMessageSinkException(String.format("Message<%s> ChunkResult payload contained invalid addi", messageId), e);
        }
    }

    void processWorkload(EsWorkload workload) throws InterruptedException, SinkException {
        esThrottler.acquireRecordSlots(workload.getAddiRecords().size());
        try {
            final int targetReference = esConnector.insertEsTaskPackage(workload);
            final EsInFlight esInFlight = new EsInFlight();
            esInFlight.setResourceName(configuration.getEsResourceName());
            esInFlight.setJobId(workload.getChunkResult().getJobId());
            esInFlight.setChunkId(workload.getChunkResult().getChunkId());
            esInFlight.setRecordSlots(workload.getAddiRecords().size());
            esInFlight.setTargetReference(targetReference);
            esInFlightAdmin.addEsInFlight(esInFlight);

            LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}",
                    targetReference, workload.getChunkResult().getChunkId(), workload.getChunkResult().getJobId());

        } catch (Throwable t) {
            esThrottler.releaseRecordSlots(workload.getAddiRecords().size());
            throw t;
        }
    }

    void backoffOnFailureRetry(int deliveryCount) {
        try {
            Thread.sleep((deliveryCount / 10) * 1000);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted while backing off for failure retry");
        }
    }
}
