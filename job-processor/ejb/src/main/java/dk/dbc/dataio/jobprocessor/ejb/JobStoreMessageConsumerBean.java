package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
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
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB
    SinkMessageProducerBean sinkMessageProducer;

    @EJB
    ChunkProcessorBean chunkProcessor;

    JSONBContext jsonbContext = new JSONBContext();

    /**
     * Processes Chunk received in consumed message
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if message payload can not be unmarshalled to chunk instance
     * @throws JobProcessorException on general handling error
     */
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobProcessorException, InvalidMessageException {
        try {
            final ExternalChunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), ExternalChunk.class);
            LOGGER.info("Received chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
            confirmLegalChunkTypeOrThrow(chunk, ExternalChunk.Type.PARTITIONED);
            process(chunk);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), consumedMessage.getPayloadType()), e);
        }
    }

    private void process(ExternalChunk chunk) throws JobProcessorException {
        final ResourceBundle resourceBundle = getResourceBundle(chunk);
        final ExternalChunk processedChunk = chunkProcessor.process(chunk, resourceBundle.getFlow(), resourceBundle.getSupplementaryProcessData());
        try {
            jobStoreServiceConnector.getConnector().addChunkIgnoreDuplicates(processedChunk, processedChunk.getJobId(), processedChunk.getChunkId());
        } catch(JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new EJBException(e);
        }
        sinkMessageProducer.send(processedChunk, resourceBundle.getSink());
    }

    private ResourceBundle getResourceBundle(ExternalChunk chunk) throws JobProcessorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStoreServiceConnector.getConnector().getResourceBundle((int) chunk.getJobId());
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching resources for job %s", chunk.getJobId()), e);
        } finally {
            LOGGER.debug("Fetching resource bundle took {} milliseconds", stopWatch.getElapsedTime());
        }
    }
}
