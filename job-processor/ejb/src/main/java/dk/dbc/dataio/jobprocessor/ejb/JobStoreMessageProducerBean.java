package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;

/**
 * This Enterprise Java Bean (EJB) functions as JMS message producer for
 * communication going to the job-store
 */
@LocalBean
@Stateless
public class JobStoreMessageProducerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageProducerBean.class);

    @Resource
    ConnectionFactory jobStoreQueueConnectionFactory;

    @Resource(name="jobStoreJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue jobStoreQueue;

    JSONBContext jsonbContext = new JSONBContext();

    public JobStoreMessageProducerBean() {}

    /**
     * Sends given sink result instance as JMS message with JSON payload to job-store queue destination
     *
     * @param deliveredChunk sink result instance to be inserted as JSON string message payload
     *
     * @throws NullPointerException when given null-valued sink result argument
     * @throws JobProcessorException when unable to send given sink result to destination
     */
    public void sendSink(ExternalChunk deliveredChunk) throws NullPointerException, JobProcessorException {
        LOGGER.info("Sending sink result for chunk {} in job {}", deliveredChunk.getChunkId(), deliveredChunk.getJobId());
        try (JMSContext context = jobStoreQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, deliveredChunk);
            context.createProducer().send(jobStoreQueue, message);
        } catch (JSONBException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending sink result for chunk %d in job %s",
                    deliveredChunk.getChunkId(), deliveredChunk.getJobId());
            throw new JobProcessorException(errorMessage, e);
        }
    }

    /**
     * Sends given processor result instance as JMS message with JSON payload to job-store queue destination
     *
     * @param processedChunk processor result instance to be inserted as JSON string message payload
     *
     * @throws NullPointerException when given null-valued processor result argument
     * @throws JobProcessorException when unable to send given processor result to destination
     */
    public void sendProc(ExternalChunk processedChunk) throws NullPointerException, JobProcessorException {
        LOGGER.info("Sending processor result for chunk {} in job {}", processedChunk.getChunkId(), processedChunk.getJobId());
        try (JMSContext context = jobStoreQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, processedChunk);
            context.createProducer().send(jobStoreQueue, message);
        } catch (JSONBException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending processor result for chunk %d in job %s",
                    processedChunk.getChunkId(), processedChunk.getJobId());
            throw new JobProcessorException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given processor result instance as JSON payload with
     * header properties '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SOURCE_PROPERTY_NAME}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PROCESSOR_SOURCE_VALUE}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#CHUNK_PAYLOAD_TYPE}' respectively
     *
     * @param context active JMS context
     * @param processedChunk processor result instance to be inserted as JSON string message payload
     *
     * @return TextMessage instance
     *
     * @throws JSONBException when unable to marshall processor result instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, ExternalChunk processedChunk) throws JMSException, JSONBException {
        final TextMessage message = context.createTextMessage(jsonbContext.marshall(processedChunk));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        return message;
    }
}
