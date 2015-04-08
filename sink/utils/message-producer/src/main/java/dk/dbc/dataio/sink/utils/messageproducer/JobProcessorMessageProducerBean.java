package dk.dbc.dataio.sink.utils.messageproducer;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.List;

@Stateless
public class JobProcessorMessageProducerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobProcessorMessageProducerBean.class);

    @Resource
    ConnectionFactory processorQueueConnectionFactory;

    @Resource(name = "processorJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue processorQueue;

    /**
     * Sends given delivered Chunk instance as JMS message with JSON payload to processor queue destination
     *
     * @param deliveredChunk resulting chunk instance to be inserted as message payload
     *
     * @throws NullPointerException when given null-valued argument
     * @throws SinkException when unable to send given delivered Chunk to destination
     */
    public void send(ExternalChunk deliveredChunk) throws NullPointerException, SinkException {
        LOGGER.info("Sending delivered Chunk {} for job {}", deliveredChunk.getChunkId(), deliveredChunk.getJobId());
        try (JMSContext context = processorQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, deliveredChunk);
            context.createProducer().send(processorQueue, message);
        } catch (JsonException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending delivered Chunk %s for job %s",
                    deliveredChunk.getChunkId(), deliveredChunk.getJobId());
            throw new SinkException(errorMessage, e);
        }
    }

    /**
     * Sends each delivered Chunk instance contained in given list as JMS message with JSON payload
     * to processor queue destination
     *
     * @param deliveredChunks list of delivered Chunk instances
     *
     * @throws NullPointerException when given null-valued argument or when given list contains null valued entries
     * @throws SinkException when unable to send delivered Chunks to destination
     */
    public void sendAll(List<ExternalChunk> deliveredChunks) throws NullPointerException, SinkException {
        try (JMSContext context = processorQueueConnectionFactory.createContext()) {
            final JMSProducer producer = context.createProducer();
            for (ExternalChunk deliveredChunk : deliveredChunks) {
                try {
                    LOGGER.info("Sending delivered Chunk {} for job {}", deliveredChunk.getChunkId(), deliveredChunk.getJobId());
                    final TextMessage message = createMessage(context, deliveredChunk);
                    producer.send(processorQueue, message);
                }catch (JsonException | JMSException e) {
                    final String errorMessage = String.format("Exception caught while sending delivered Chunk %s for job %s",
                            deliveredChunk.getChunkId(), deliveredChunk.getJobId());
                    throw new SinkException(errorMessage, e);
                }
            }
        }
    }

    /**
     * Creates new TextMessage with given delivered Chunk instance as JSON payload with
     * header properties '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SOURCE_PROPERTY_NAME}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SINK_SOURCE_VALUE}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SINK_RESULT_PAYLOAD_TYPE}' respectively.
     *
     * @param context active JMS context
     * @param deliveredChunk resulting chunk instance to be added as payload
     *
     * @return TextMessage instance
     *
     * @throws JsonException when unable to marshall NewJob instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, ExternalChunk deliveredChunk) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(deliveredChunk));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.SINK_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        return message;
    }
}
