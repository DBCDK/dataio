package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

/**
 * Handles Chunk messages received from the job-store
 */
@MessageDriven
public class JobStoreMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageConsumerBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnector;

    @EJB
    JobStoreMessageProducerBean jobStoreMessageProducer;

    @EJB
    SinkMessageProducerBean sinkMessageProducer;

    @EJB
    ChunkProcessorBean chunkProcessor;

    /**
     * Processes Chunk received in consumed message
     *
     * @param consumedMessage message to be handled
     *
     * @throws InvalidMessageException if message payload can not be unmarshalled to Chunk instance
     * @throws JobProcessorException on general handling error
     */
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobProcessorException, InvalidMessageException {
        try {
            final Chunk chunk = JsonUtil.fromJson(consumedMessage.getMessagePayload(), Chunk.class, MixIns.getMixIns());
            LOGGER.info("Received chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
            process(chunk);
        } catch (JsonException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), consumedMessage.getPayloadType()), e);
        }
    }

    private void process(Chunk chunk) throws JobProcessorException {
        final Sink sink = getSink(chunk);
        final Flow flow = getFlow(chunk);
        final ChunkResult processorResult = chunkProcessor.process(chunk, flow);
        jobStoreMessageProducer.send(processorResult);
        sinkMessageProducer.send(processorResult, sink);
    }

    private Sink getSink(Chunk chunk) throws JobProcessorException {
        try {
            return jobStoreServiceConnector.getSink(chunk.getJobId());
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching sink for job %s", chunk.getJobId()), e);
        }
    }

    private Flow getFlow(Chunk chunk) throws JobProcessorException {
        try {
            return jobStoreServiceConnector.getFlow(chunk.getJobId());
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching flow for job %s", chunk.getJobId()), e);
        }
    }
}
