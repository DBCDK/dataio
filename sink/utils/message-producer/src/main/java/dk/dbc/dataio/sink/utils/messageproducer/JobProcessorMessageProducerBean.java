package dk.dbc.dataio.sink.utils.messageproducer;

import dk.dbc.dataio.commons.types.SinkChunkResult;
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
     * Sends given SinkChunkResult instance as JMS message with JSON payload to processor queue destination
     *
     * @param sinkChunkResult SinkChunkResult instance to be inserted as message payload
     *
     * @throws NullPointerException when given null-valued argument
     * @throws SinkException when unable to send given SinkChunkResult to destination
     */
    public void send(SinkChunkResult sinkChunkResult) throws NullPointerException, SinkException {
        LOGGER.info("Sending SinkChunkResult {} for job {}", sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
        try (JMSContext context = processorQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, sinkChunkResult);
            context.createProducer().send(processorQueue, message);
        } catch (JsonException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending SinkChunkResult %s for job %s",
                    sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
            throw new SinkException(errorMessage, e);
        }
    }

    /**
     * Sends each SinkChunkResult instance contained in given list as JMS message with JSON payload
     * to processor queue destination
     *
     * @param sinkChunkResults list of SinkChunkResult instances
     *
     * @throws NullPointerException when given null-valued argument or when given list contains null valued entries
     * @throws SinkException when unable to send SinkChunkResult to destination
     */
    public void sendAll(List<SinkChunkResult> sinkChunkResults) throws NullPointerException, SinkException {
        try (JMSContext context = processorQueueConnectionFactory.createContext()) {
            final JMSProducer producer = context.createProducer();
            for (SinkChunkResult sinkChunkResult : sinkChunkResults) {
                try {
                    LOGGER.info("Sending SinkChunkResult {} for job {}", sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
                    final TextMessage message = createMessage(context, sinkChunkResult);
                    producer.send(processorQueue, message);
                }catch (JsonException | JMSException e) {
                    final String errorMessage = String.format("Exception caught while sending SinkChunkResult %s for job %s",
                            sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
                    throw new SinkException(errorMessage, e);
                }
            }
        }
    }

    /**
     * Creates new TextMessage with given SinkChunkResult instance as JSON payload with
     * header properties '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SOURCE_PROPERTY_NAME}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SINK_SOURCE_VALUE}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SINK_RESULT_PAYLOAD_TYPE}' respectively.
     *
     * @param context active JMS context
     * @param sinkChunkResult SinkChunkResult instance to be added as payload
     *
     * @return TextMessage instance
     *
     * @throws JsonException when unable to marshall NewJob instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, SinkChunkResult sinkChunkResult) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(sinkChunkResult));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.SINK_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE);
        return message;
    }
}
