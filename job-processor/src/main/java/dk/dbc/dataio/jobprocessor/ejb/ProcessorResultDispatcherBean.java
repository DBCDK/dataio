package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkResult;
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
public class ProcessorResultDispatcherBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorResultDispatcherBean.class);

    @Resource
    ConnectionFactory sinkQueueConnectionFactory;

    @Resource(name="sinkJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue sinkQueue;

    @Resource
    ConnectionFactory jobStoreQueueConnectionFactory;

    @Resource(name="jobStoreJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue jobStoreQueue;

    public void dispatch(ChunkResult processorChunkResult) {
        LOGGER.info("Dispatching ChunkResult for chunk {} in job {}", processorChunkResult.getChunkId(), processorChunkResult.getJobId());
        dispatchToJobStore(processorChunkResult);
        dispatchToSink(processorChunkResult);
    }

    private void dispatchToSink(ChunkResult processorChunkResult) {
        try (JMSContext context = sinkQueueConnectionFactory.createContext()) {
            final TextMessage message = context.createTextMessage(JsonUtil.toJson(processorChunkResult));
            message.setStringProperty("chunkResultSource", "processor"); // todo: US#232 Get these values from configuration
            //message.setStringProperty("resource", sink.getContent().getResource()); todo: US#232 get sink from somewhere
            context.createProducer().send(sinkQueue, message);
        } catch (JsonException | JMSException e) {
            throw new EJBException(e);
        }
    }

    private void dispatchToJobStore(ChunkResult processorChunkResult) {
        try (JMSContext context = jobStoreQueueConnectionFactory.createContext()) {
            final TextMessage message = context.createTextMessage(JsonUtil.toJson(processorChunkResult));
            message.setStringProperty("chunkResultSource", "processor"); // todo: US#232 Get these values from configuration
            context.createProducer().send(jobStoreQueue, message);
        } catch (JsonException | JMSException e) {
            throw new EJBException(e);
        }
    }
}
