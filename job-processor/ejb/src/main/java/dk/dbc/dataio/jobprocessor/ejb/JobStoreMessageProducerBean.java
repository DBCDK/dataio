package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
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

    public static final String SOURCE_PROPERTY_NAME = "source";
    public static final String SOURCE_PROPERTY_VALUE = "processor";
    public static final String PAYLOAD_PROPERTY_NAME = "payload";
    public static final String SINK_RESULT_PAYLOAD_PROPERTY_VALUE = "SinkChunkResult";
    public static final String PROCESSOR_RESULT_PAYLOAD_PROPERTY_VALUE = "ChunkResult";

    @Resource
    ConnectionFactory jobStoreQueueConnectionFactory;

    @Resource(name="jobStoreJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue jobStoreQueue;

    /**
     * Sends given SinkChunkResult instance as JMS message with JSON payload to job-store queue destination
     *
     * @param sinkChunkResult SinkChunkResult instance to be inserted as message payload
     *
     * @throws NullPointerException when given null-valued sinkChunkResult argument
     * @throws JobProcessorException when unable to send given SinkChunkResult to destination
     */
    public void send(SinkChunkResult sinkChunkResult) throws NullPointerException, JobProcessorException {
        LOGGER.info("Sending SinkChunkResult for chunk {} in job {}", sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
        try (JMSContext context = jobStoreQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, sinkChunkResult);
            context.createProducer().send(jobStoreQueue, message);
        } catch (JsonException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending SinkChunkResult for chunk %d in job %s",
                    sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
            throw new JobProcessorException(errorMessage, e);
        }
    }

    /**
     * Sends given ChunkResult instance as JMS message with JSON payload to job-store queue destination
     *
     * @param processorResult ChunkResult instance to be inserted as message payload
     *
     * @throws NullPointerException when given null-valued sinkChunkResult argument
     * @throws JobProcessorException when unable to send given ChunkResult to destination
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
     * Creates new TextMessage with given SinkChunkResult instance as JSON payload with
     * header properties '{@value #SOURCE_PROPERTY_NAME}' and '{@value #PAYLOAD_PROPERTY_NAME}'
     * set to '{@value #SOURCE_PROPERTY_VALUE}' and '{@value #SINK_RESULT_PAYLOAD_PROPERTY_VALUE}' respectively
     *
     * @param context active JMS context
     * @param sinkResult SinkChunkResult instance to be added as payload
     *
     * @return TextMessage instance
     *
     * @throws JsonException when unable to marshall SinkChunkResult instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, SinkChunkResult sinkResult) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(sinkResult));
        message.setStringProperty(SOURCE_PROPERTY_NAME, SOURCE_PROPERTY_VALUE);
        message.setStringProperty(PAYLOAD_PROPERTY_NAME, SINK_RESULT_PAYLOAD_PROPERTY_VALUE);
        return message;
    }

    /**
     * Creates new TextMessage with given ChunkResult instance as JSON payload with
     * header properties '{@value #SOURCE_PROPERTY_NAME}' and '{@value #PAYLOAD_PROPERTY_NAME}'
     * set to '{@value #SOURCE_PROPERTY_VALUE}' and '{@value #PROCESSOR_RESULT_PAYLOAD_PROPERTY_VALUE}' respectively
     *
     * @param context active JMS context
     * @param processorResult ChunkResult instance to be added as payload
     *
     * @return TextMessage instance
     *
     * @throws JsonException when unable to marshall ChunkResult instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, ChunkResult processorResult) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(processorResult));
        message.setStringProperty(SOURCE_PROPERTY_NAME, SOURCE_PROPERTY_VALUE);
        message.setStringProperty(PAYLOAD_PROPERTY_NAME, PROCESSOR_RESULT_PAYLOAD_PROPERTY_VALUE);
        return message;
    }
}
