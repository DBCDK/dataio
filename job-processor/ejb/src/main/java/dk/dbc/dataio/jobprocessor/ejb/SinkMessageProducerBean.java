package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;

/**
 * This Enterprise Java Bean (EJB) functions as JMS message producer for
 * communication going to the sinks
 */
@LocalBean
@Stateless
public class SinkMessageProducerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkMessageProducerBean.class);

    @Resource
    ConnectionFactory sinksQueueConnectionFactory;

    @Resource(name="sinksJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue sinksQueue;

    @EJB
    JSONBBean jsonBinding;

    public SinkMessageProducerBean() {}

    public SinkMessageProducerBean(JSONBBean jsonBinding) {
        this.jsonBinding = jsonBinding;
    }

    /**
     * Sends given processor result instance as JMS message with JSON payload to sink queue destination
     *
     * @param processedChunk processor result instance to be inserted as JSON string message payload
     * @param destination Sink instance for sink target
     *
     * @throws NullPointerException when given null-valued argument
     * @throws JobProcessorException when unable to send given processor result to destination
     */
    public void send(ExternalChunk processedChunk, Sink destination) throws NullPointerException, JobProcessorException {
        LOGGER.info("Sending processor for chunk {} in job {} to sink {}",
                processedChunk.getChunkId(), processedChunk.getJobId(), destination.getContent().getName());
        try (JMSContext context = sinksQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, processedChunk, destination);
            context.createProducer().send(sinksQueue, message);
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
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PROCESSOR_RESULT_PAYLOAD_TYPE}' respectively,
     * and header property '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#RESOURCE_PROPERTY_NAME}'
     * to the resource value contained in given Sink instance.
     *
     * @param context active JMS context
     * @param processedChunk processor result instance to be added as JSON string payload
     * @param destination Sink instance for sink target
     *
     * @return TextMessage instance
     *
     * @throws JSONBException when unable to marshall processor result instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, ExternalChunk processedChunk, Sink destination) throws JMSException, JSONBException {
        final TextMessage message = context.createTextMessage(jsonBinding.getContext().marshall(processedChunk));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        message.setStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME, destination.getContent().getResource());
        return message;
    }
}
