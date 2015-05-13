package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
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
    PgJobStore jobStoreBean;

    JSONBContext jsonbContext = new JSONBContext();

    /**
     * Handles consumed message by storing contained result payload in the underlying data store
     *
     * @param consumedMessage message to be handled
     *
     * @throws dk.dbc.dataio.commons.types.exceptions.InvalidMessageException if message payload can not be unmarshalled, or is unknown type
     * @throws dk.dbc.dataio.jobstore.types.JobStoreException on internal error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, JobStoreException {
        try {
            final ExternalChunk externalChunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), ExternalChunk.class);
            LOGGER.info("Received chunk {} with chunk type {} for job {}", externalChunk.getChunkId(), externalChunk.getType(), externalChunk.getJobId());
            try {
                jobStoreBean.addChunk(externalChunk);
            } catch (DuplicateChunkException e) {
                LOGGER.warn("Caught exception trying to add already existing chunk: {}", e.getMessage());
            }
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid %s result type",
                    consumedMessage.getMessageId(), consumedMessage.getMessagePayload()), e);
        }
    }
}
