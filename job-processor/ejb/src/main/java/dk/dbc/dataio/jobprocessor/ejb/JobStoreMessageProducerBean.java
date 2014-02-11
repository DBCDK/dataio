package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
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

    /**
     * Sends given sink result instance as JMS message with JSON payload to job-store queue destination
     *
     * @param sinkChunkResult sink result instance to be inserted as JSON string message payload
     *
     * @throws NullPointerException when given null-valued sink result argument
     * @throws JobProcessorException when unable to send given sink result to destination
     */
    public void send(SinkChunkResult sinkChunkResult) throws NullPointerException, JobProcessorException {
        LOGGER.info("Sending sink result for chunk {} in job {}", sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
        try (JMSContext context = jobStoreQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, sinkChunkResult);
            context.createProducer().send(jobStoreQueue, message);
        } catch (JsonException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending sink result for chunk %d in job %s",
                    sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
            throw new JobProcessorException(errorMessage, e);
        }
    }

    /**
     * Sends given processor result instance as JMS message with JSON payload to job-store queue destination
     *
     * @param processorResult processor result instance to be inserted as JSON string message payload
     *
     * @throws NullPointerException when given null-valued processor result argument
     * @throws JobProcessorException when unable to send given processor result to destination
     */
    public void send(ChunkResult processorResult) throws NullPointerException, JobProcessorException {
        LOGGER.info("Sending processor result for chunk {} in job {}", processorResult.getChunkId(), processorResult.getJobId());
        try (JMSContext context = jobStoreQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, processorResult);
            context.createProducer().send(jobStoreQueue, message);
        } catch (JsonException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending processor result for chunk %d in job %s",
                    processorResult.getChunkId(), processorResult.getJobId());
            throw new JobProcessorException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given sink result instance as JSON payload with
     * header properties '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SOURCE_PROPERTY_NAME}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PROCESSOR_SOURCE_VALUE}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SINK_RESULT_PAYLOAD_TYPE}' respectively
     *
     * @param context active JMS context
     * @param sinkResult sink result instance to be added as payload
     *
     * @return TextMessage instance
     *
     * @throws JsonException when unable to marshall sink result instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, SinkChunkResult sinkResult) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(sinkResult));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE);
        return message;
    }

    /**
     * Creates new TextMessage with given processor result instance as JSON payload with
     * header properties '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SOURCE_PROPERTY_NAME}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PROCESSOR_SOURCE_VALUE}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PROCESSOR_RESULT_PAYLOAD_TYPE}' respectively
     *
     * @param context active JMS context
     * @param processorResult processor result instance to be inserted as JSON string message payload
     *
     * @return TextMessage instance
     *
     * @throws JsonException when unable to marshall processor result instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, ChunkResult processorResult) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(processorResult));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE);
        return message;
    }
}
