package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

/**
 * Handles messages received from sinks.
 */
@MessageDriven
public class SinkMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkMessageConsumerBean.class);

    @EJB
    JobStoreMessageProducerBean jobStoreMessageProducer;

    @EJB
    JSONBBean jsonBinding;

    /**
     * Handles consumed message by forwarding sink result payload from
     * received message to the job-store.
     *
     * @param consumedMessage message to be handled
     *
     * @throws InvalidMessageException if message payload can not be marshalled to delivered Chunk instance
     * @throws JobProcessorException on general handling error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobProcessorException, InvalidMessageException {
        try {
            final ExternalChunk deliveredChunk = jsonBinding.getContext().unmarshall(consumedMessage.getMessagePayload(), ExternalChunk.class);
            confirmLegalChunkTypeOrThrow(deliveredChunk, ExternalChunk.Type.DELIVERED);
            LOGGER.info("Received sink result for jobId={}, chunkId={}", deliveredChunk.getJobId(), deliveredChunk.getChunkId());
            jobStoreMessageProducer.sendSink(deliveredChunk);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid sink result type %s",
                    consumedMessage.getMessageId(), consumedMessage.getPayloadType()), e);
        }
    }
}
