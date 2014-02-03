package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.NewJob;
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
 * Handles messages received from the job-store
 */
@MessageDriven
public class JobStoreMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageConsumerBean.class);

    @EJB
    JobProcessorBean jobProcessor;

    /**
     * Handles consumed message by notifying the job processor that a new job is available
     *
     * @param consumedMessage message to be handled
     *
     * @throws InvalidMessageException if message payload can not be unmarshalled to NewJob instance
     * @throws JobProcessorException on general handling error
     */
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobProcessorException, InvalidMessageException {
        try {
            final NewJob newJob = JsonUtil.fromJson(consumedMessage.getMessagePayload(), NewJob.class, MixIns.getMixIns());
            LOGGER.info("Received NewJob for jobId={}", newJob.getJobId());
            jobProcessor.process(newJob);
        } catch (JsonException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid NewJob type", consumedMessage.getMessageId()), e);
        }
    }

}
