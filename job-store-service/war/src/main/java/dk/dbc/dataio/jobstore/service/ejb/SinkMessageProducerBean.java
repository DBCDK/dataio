package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * This Enterprise Java Bean (EJB) functions as JMS message producer for
 * communication going to the sinks
 */
@LocalBean
@Stateless
public class SinkMessageProducerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkMessageProducerBean.class);

    @Inject
    @JMSConnectionFactory("jms/artemisConnectionFactory")
    JMSContext context;

    JSONBContext jsonbContext = new JSONBContext();
    private final Map<String, Queue> queues = new HashMap<>();

    /**
     * Sends given processed chunk as JMS message with JSON payload to sink queue destination
     *
     * @param chunk    processed chunk to be inserted as JSON string message payload
     * @param job      job to which the chunk belongs
     * @param priority message priority
     * @throws NullPointerException when given null-valued argument
     * @throws JobStoreException    when unable to send chunk to destination
     */
    public void send(Chunk chunk, JobEntity job, int priority) throws NullPointerException, JobStoreException {
        final Sink destination = job.getCachedSink().getSink();
        final FlowStoreReferences flowStoreReferences = job.getFlowStoreReferences();

        LOGGER.info("Sending chunk {}/{} to sink {}", chunk.getJobId(), chunk.getChunkId(),
                destination.getContent().getName());

        try {
            Queue queue = queues.computeIfAbsent(destination.getContent().getQueue(), context::createQueue);
            final TextMessage message = createMessage(context, chunk, destination, flowStoreReferences);
            final JMSProducer producer = context.createProducer();
            producer.setPriority(priority);
            producer.send(queue, message);
        } catch (JSONBException | JMSException e) {
            final String errorMessage = String.format(
                    "Exception caught while sending processed chunk %d in job %s",
                    chunk.getChunkId(),
                    chunk.getJobId());
            throw new JobStoreException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given processor result instance as JSON payload with
     * header properties:
     * <pre>
     *   {@value JmsConstants#PAYLOAD_PROPERTY_NAME}={@value JmsConstants#CHUNK_PAYLOAD_TYPE}
     *   {@value JmsConstants#RESOURCE_PROPERTY_NAME}=[the resource value contained in given Sink instance]
     *   {@value JmsConstants#SINK_ID_PROPERTY_NAME}=[sink ID]
     *   {@value JmsConstants#SINK_VERSION_PROPERTY_NAME}=[sink version]
     *   {@value JmsConstants#FLOW_BINDER_ID_PROPERTY_NAME}=[flow-binder ID]
     *   {@value JmsConstants#FLOW_BINDER_VERSION_PROPERTY_NAME}=[flow-binder version]
     * </pre>
     *
     * @param context             active JMS context
     * @param chunk               processed chunk to be added as JSON string payload
     * @param destination         Sink instance for sink target
     * @param flowStoreReferences flow-store references for the job to which the given chunk belongs
     * @return TextMessage instance
     * @throws JSONBException when unable to marshall processor result instance to JSON
     * @throws JMSException   when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, Chunk chunk, Sink destination, FlowStoreReferences flowStoreReferences)
            throws JMSException, JSONBException {
        final FlowStoreReference sinkReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        final FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        final TextMessage message = context.createTextMessage(jsonbContext.marshall(chunk));
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        message.setStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME, destination.getContent().getResource());
        message.setLongProperty(JmsConstants.SINK_ID_PROPERTY_NAME, sinkReference.getId());
        message.setLongProperty(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkReference.getVersion());
        // if the execution is towards the diff sink during an acceptance test run
        if (flowBinderReference != null) {
            message.setLongProperty(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME, flowBinderReference.getId());
            message.setLongProperty(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME, flowBinderReference.getVersion());
        }
        return message;
    }
}
