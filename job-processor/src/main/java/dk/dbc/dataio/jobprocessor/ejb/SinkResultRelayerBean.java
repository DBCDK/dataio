package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;

@LocalBean
public class SinkResultRelayerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkResultRelayerBean.class);

    public static final String CHUNK_RESULT_SOURCE_PROPERTY = "processor";

    @Resource
    ConnectionFactory jobStoreQueueConnectionFactory;

    @Resource(name="jobStoreJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue jobStoreQueue;

    public void relay(SinkChunkResult sinkChunkResult) {
        LOGGER.info("Relaying SinkChunkResult for chunk {} in job {}", sinkChunkResult.getChunkId(), sinkChunkResult.getJobId());
        try (JMSContext context = jobStoreQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, sinkChunkResult);
            context.createProducer().send(jobStoreQueue, message);
        } catch (JsonException | JMSException e) {
            throw new EJBException(e);
        }
    }

    TextMessage createMessage(JMSContext context, SinkChunkResult sinkChunkResult) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(sinkChunkResult));
        message.setStringProperty("chunkResultSource", CHUNK_RESULT_SOURCE_PROPERTY); // todo: US#232 Get these values from configuration
        return message;
    }
}
