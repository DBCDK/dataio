package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jobstore.types.JobError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;

/**
 * Handles Chunk messages received from the job-store
 */
@MessageDriven(name = "jobStoreListener", activationConfig = {
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/processor"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "shard = '${ENV=PROCESSOR_SHARD}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true")
})
public class JobStoreMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageConsumerBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnector;
    @EJB
    ChunkProcessorBean chunkProcessor;
    @EJB
    CapacityBean capacityBean;

    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Processes Chunk received in consumed message
     *
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if message payload can not be unmarshalled to chunk instance
     * @throws JobProcessorException   on general handling error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, JobProcessorException {
        final Chunk chunk = extractChunkFromConsumedMessage(consumedMessage);
        LOGGER.info("Received chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
        final long flowId = consumedMessage.getHeaderValue(JmsConstants.FLOW_ID_PROPERTY_NAME, Long.class);
        final long flowVersion = consumedMessage.getHeaderValue(JmsConstants.FLOW_VERSION_PROPERTY_NAME, Long.class);
        final String additionalArgs = consumedMessage.getHeaderValue(JmsConstants.ADDITIONAL_ARGS, String.class);
        final Flow flow = getFlow(chunk, flowId, flowVersion);
        sendResultToJobStore(processChunk(chunk, flow, additionalArgs));
    }

    private Chunk extractChunkFromConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            final Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            confirmLegalChunkTypeOrThrow(chunk, Chunk.Type.PARTITIONED);
            return chunk;
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), consumedMessage.getHeaderValue(JmsConstants.CHUNK_PAYLOAD_TYPE, String.class)), e);
        }
    }

    private Chunk processChunk(Chunk chunk, Flow flow, String additionalArgs) {
        final StopWatch stopWatch = new StopWatch();
        final Chunk result = chunkProcessor.process(chunk, flow, additionalArgs);
        if (stopWatch.getElapsedTime() > CapacityBean.MAXIMUM_TIME_TO_PROCESS_IN_MILLISECONDS) {
            LOGGER.error("This processor has exceeded its maximum capacity");
            capacityBean.signalCapacityExceeded();
        }
        return result;
    }

    private Flow getFlow(Chunk chunk, long flowId, long flowVersion) throws JobProcessorException {
        return chunkProcessor.getCachedFlow(flowId, flowVersion).orElseGet(() -> flowFromJobStore(chunk));
    }

    private Flow flowFromJobStore(Chunk chunk) throws JobProcessorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStoreServiceConnector.getConnector().getCachedFlow((int) chunk.getJobId());
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching flow for job %s", chunk.getJobId()), e);
        } finally {
            LOGGER.debug("Fetching flow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @SuppressWarnings("Duplicates")
    private void sendResultToJobStore(Chunk chunk) throws JobProcessorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            jobStoreServiceConnector.getConnector().addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new JobProcessorException("Error while sending result to job-store", e);
        } finally {
            LOGGER.debug("Sending result took {} milliseconds", stopWatch.getElapsedTime());
        }
    }
}
