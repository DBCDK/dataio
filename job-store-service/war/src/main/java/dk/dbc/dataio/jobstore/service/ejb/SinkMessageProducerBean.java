package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.types.jms.MessageIdentifiers;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.time.Duration;

/**
 * This Enterprise Java Bean (EJB) functions as JMS message producer for
 * communication going to the sinks
 */
@LocalBean
@Stateless
public class SinkMessageProducerBean implements MessageIdentifiers {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkMessageProducerBean.class);
    private final RetryPolicy<?> retryPolicy;


    @Inject
    @ConfigProperty(name = "ARTEMIS_MQ_HOST")
    private String artemisHost;
    ConnectionFactory connectionFactory;

    JSONBContext jsonbContext = new JSONBContext();

    public SinkMessageProducerBean() {
        retryPolicy = new RetryPolicy<>().withDelay(Duration.ofSeconds(30)).withMaxRetries(10)
                .onFailedAttempt(attempt -> LOGGER.warn("Unable to send message to sink", attempt.getLastFailure()));
    }

    public SinkMessageProducerBean(RetryPolicy<?> retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    @PostConstruct
    public void init() {
        connectionFactory = new ActiveMQXAConnectionFactory("tcp://" + artemisHost + ":61616");
    }

    public void sendAbort(JobEntity job) throws JobStoreException {
        String queue = job.getSinkQueue();
        try(JMSContext context = connectionFactory.createContext()) {
            TextMessage message = context.createTextMessage();
            JMSHeader.payload.addHeader(message, JMSHeader.ABORT_PAYLOAD_TYPE);
            JMSHeader.jobId.addHeader(message, job.getId());
            LOGGER.warn("Sending abort for job {} to queue {}", job.getId(), queue);
            send(context, message, job, 9);
        } catch (JMSException e) {
            throw new JobStoreException("Unable to send job abort for " + job.getId() +  " to queue " + queue, e);
        }
    }

    /**
     * Sends given processed chunk as JMS message with JSON payload to sink queue destination
     *
     * @param chunk    processed chunk to be inserted as JSON string message payload
     * @param job      job to which the chunk belongs
     * @param priority message priority
     * @throws JobStoreException    when unable to send chunk to destination
     */
    public void send(Chunk chunk, JobEntity job, int priority) throws JobStoreException {
        FlowStoreReferences flowStoreReferences = job.getFlowStoreReferences();
        try(JMSContext context = connectionFactory.createContext()) {
            TextMessage message = createMessage(context, chunk, flowStoreReferences);
            LOGGER.info("Sending chunk {}/{} to queue {} with unique id {}", chunk.getJobId(), chunk.getChunkId(), job.getSinkQueue(), chunk.getTrackingId());

            send(context, message, job, priority);
        } catch (JSONBException | JMSException e) {
            String errorMessage = String.format(
                    "Exception caught while sending processed chunk %d in job %s with trackingId %s",
                    chunk.getChunkId(),
                    chunk.getJobId(),
                    chunk.getTrackingId());
            throw new JobStoreException(errorMessage, e);
        }
    }

    private void send(JMSContext context, TextMessage message, JobEntity job, int priority) {
        String qname = job.getSinkQueue();
        Queue queue = context.createQueue(qname.contains("::") ? qname : qname + "::" + qname);
        JMSProducer producer = context.createProducer();
        producer.setPriority(priority);
        Failsafe.with(retryPolicy).run(() -> producer.send(queue, message));
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
    public TextMessage createMessage(JMSContext context, Chunk chunk, FlowStoreReferences flowStoreReferences) throws JMSException, JSONBException {
        FlowStoreReference sinkReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        TextMessage message = context.createTextMessage(jsonbContext.marshall(chunk));
        JMSHeader.payload.addHeader(message, JMSHeader.CHUNK_PAYLOAD_TYPE);
        JMSHeader.sinkId.addHeader(message, sinkReference.getId());
        JMSHeader.sinkVersion.addHeader(message, sinkReference.getVersion());
        addIdentifiers(message, chunk);
        // if the execution is towards the diff sink during an acceptance test run
        if (flowBinderReference != null) {
            JMSHeader.flowBinderId.addHeader(message, flowBinderReference.getId());
            JMSHeader.flowBinderVersion.addHeader(message, flowBinderReference.getVersion());
        }
        return message;
    }
}
