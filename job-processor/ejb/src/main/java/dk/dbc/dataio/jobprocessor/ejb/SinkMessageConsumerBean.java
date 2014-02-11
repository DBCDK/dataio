package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
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

    /**
     * Handles consumed message by forwarding sink result payload from
     * received message to the job-store.
     *
     * @param consumedMessage message to be handled
     *
     * @throws InvalidMessageException if message payload can not be marshalled to SinkChunkResult instance
     * @throws JobProcessorException on general handling error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobProcessorException, InvalidMessageException {
        try {
            final SinkChunkResult sinkChunkResult = JsonUtil.fromJson(consumedMessage.getMessagePayload(), SinkChunkResult.class, MixIns.getMixIns());
            LOGGER.info("Received sink result for jobId={}, chunkId={}", sinkChunkResult.getJobId(), sinkChunkResult.getChunkId());
            jobStoreMessageProducer.send(sinkChunkResult);
        } catch (JsonException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid sink result type %s",
                    consumedMessage.getMessageId(), consumedMessage.getPayloadType()), e);
        }
    }

}
