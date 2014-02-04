package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ConsumedMessage;
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
     * Handles consumed message by storing contained processor result payload in the underlying data store
     *
     * @param consumedMessage message to be handled
     *
     * @throws InvalidMessageException if message payload can not be unmarshalled to ChunkResult instance
     * @throws JobStoreException on internal error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobStoreException, InvalidMessageException {
        try {
            final ChunkResult processorResult = JsonUtil.fromJson(consumedMessage.getMessagePayload(), ChunkResult.class, MixIns.getMixIns());
            LOGGER.info("Received processor result {} for job {}", processorResult.getChunkId(), processorResult.getJobId());

            jobStore.addProcessorResult(processorResult);
        } catch (JsonException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid processor result type", consumedMessage.getMessageId()), e);
        }
    }
}
