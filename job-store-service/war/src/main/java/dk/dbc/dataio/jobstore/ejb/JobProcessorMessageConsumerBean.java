package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.newjobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.ChunkResult;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SinkChunkResult;
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
    JobStoreBean jobStoreBean;

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @EJB
    JobStoreServiceConnectorBean newJobStoreServiceConnectorBean;

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
            switch (consumedMessage.getPayloadType()) {
                case JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE:
                    handleProcessorResult(consumedMessage);
                    break;
                case JmsConstants.SINK_RESULT_PAYLOAD_TYPE:
                    handleSinkResult(consumedMessage);
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
        final ExternalChunk processedChunk = JsonUtil.fromJson(consumedMessage.getMessagePayload(), ExternalChunk.class, MixIns.getMixIns());
        LOGGER.debug(JsonUtil.toJson(processedChunk));
        final ChunkResult processorResult = ChunkResult.convertFromExternalChunk(processedChunk);
        LOGGER.info("Received processor result {} for job {}", processorResult.getChunkId(), processorResult.getJobId());
        jobStoreBean.getJobStore().addProcessorResult(processorResult);
        addChunkToNewJobStore(makeChunkCompatibleWithNewJobStore(processedChunk));
    }

    private void handleSinkResult(ConsumedMessage consumedMessage) throws JsonException, JobStoreException {
        final ExternalChunk deliveredChunk = JsonUtil.fromJson(consumedMessage.getMessagePayload(), ExternalChunk.class);
        final SinkChunkResult sinkResult = SinkChunkResult.convertFromExternalChunk(deliveredChunk);
        LOGGER.info("Received sink result {} for job {}", sinkResult.getChunkId(), sinkResult.getJobId());
        jobStoreBean.getJobStore().addSinkResult(sinkResult);
        jobSchedulerBean.releaseChunk(sinkResult.getJobId(), sinkResult.getChunkId());
        addChunkToNewJobStore(makeChunkCompatibleWithNewJobStore(deliveredChunk));
    }

    private void addChunkToNewJobStore(ExternalChunk chunk) throws JobStoreException {
        try {
            newJobStoreServiceConnectorBean.getConnector().addChunk(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("New job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new JobStoreException("Error in communication with new job-store", e);
        }
    }

    private ExternalChunk makeChunkCompatibleWithNewJobStore(ExternalChunk chunk) {
        final ExternalChunk compatibleChunk = new ExternalChunk(chunk.getJobId(), chunk.getChunkId() - 1, chunk.getType());
        for (ChunkItem item : chunk) {
            compatibleChunk.insertItem(item);
        }
        return compatibleChunk;
    }
 }
