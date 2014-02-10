package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

/**
 * Handles messages received from the job-processor
 */
@MessageDriven
public class JobProcessorMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobProcessorMessageConsumerBean.class);

    @EJB
    JobStoreBean jobStore;

    /**
     * Handles consumed message by storing contained result payload in the underlying data store
     *
     * @param consumedMessage message to be handled
     *
     * @throws InvalidMessageException if message payload can not be unmarshalled, or is unknown type
     * @throws JobStoreException on internal error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobStoreException, InvalidMessageException {
        try {
            switch (consumedMessage.getMessagePayload()) {
                case "ChunkResult": handleProcessorResult(consumedMessage);
                    break;
                case "SinkChunkResult": handleSinkResult(consumedMessage);
                    break;
                default:
                    throw new InvalidMessageException(String.format("Message<%s> payload was unknown type %s",
                            consumedMessage.getMessageId(), consumedMessage.getMessagePayload()));
            }
        } catch (JsonException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid %s result type",
                    consumedMessage.getMessageId(), consumedMessage.getMessagePayload()), e);
        }
    }

   private void handleProcessorResult(ConsumedMessage consumedMessage) throws JsonException, JobStoreException {
        final ChunkResult processorResult = JsonUtil.fromJson(consumedMessage.getMessagePayload(), ChunkResult.class, MixIns.getMixIns());
        LOGGER.info("Received processor result {} for job {}", processorResult.getChunkId(), processorResult.getJobId());
        jobStore.addProcessorResult(processorResult);
    }

    private void handleSinkResult(ConsumedMessage consumedMessage) throws JsonException, JobStoreException {
        final SinkChunkResult sinkResult = JsonUtil.fromJson(consumedMessage.getMessagePayload(), SinkChunkResult.class, MixIns.getMixIns());
        LOGGER.info("Received sink result {} for job {}", sinkResult.getChunkId(), sinkResult.getJobId());
        jobStore.addSinkResult(sinkResult);
    }
 }
