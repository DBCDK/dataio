package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.types.jms.MessageIdentifiers;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.Optional;

@LocalBean
@Stateless
public class JobProcessorMessageProducerBean implements MessageIdentifiers {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobProcessorMessageProducerBean.class);

    JSONBContext jsonbContext = new JSONBContext();

    @Inject
    @ConfigProperty(name = "ARTEMIS_MQ_HOST")
    private String artemisHost;
    private ConnectionFactory connectionFactory;

    @PostConstruct
    public void init() {
        connectionFactory = new ActiveMQXAConnectionFactory("tcp://" + artemisHost + "61616");
    }

    /**
     * Sends given Chunk instance as JMS message with JSON payload to processor queue destination
     *
     * @param chunk     chunk instance to be inserted as message payload
     * @param jobEntity instance to deduct which processor shard should be inserted as message payload
     * @param priority  message priority
     * @throws NullPointerException when given null-valued argument
     * @throws JobStoreException    when unable to send given chunk to destination
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void send(Chunk chunk, JobEntity jobEntity, int priority) throws NullPointerException, JobStoreException {
        LOGGER.info("Sending chunk {}/{} with trackingId {}", chunk.getJobId(), chunk.getChunkId(), chunk.getTrackingId());
        try(JMSContext context = connectionFactory.createContext(JMSContext.SESSION_TRANSACTED)) {
            TextMessage message = createMessage(context, chunk, jobEntity);
            JMSProducer producer = context.createProducer();
            producer.setPriority(priority);
            String queueName = jobEntity.getSpecification().getType().processorQueue;
            Queue queue = context.createQueue(queueName);
            producer.send(queue, message);
        } catch (JSONBException | JMSException e) {
            String errorMessage = String.format("Exception caught while queueing chunk %s for job %s with trackingId %s", chunk.getChunkId(), chunk.getJobId(), chunk.getTrackingId());
            throw new JobStoreException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given chunk instance as JSON payload
     *
     * @param context   active JMS context
     * @param chunk     chunk instance to be added as payload
     * @param jobEntity to where the chunk instance belongs
     * @param uuid Unique identifier to ease message tracking across our servers
     * @return TextMessage instance
     * @throws JSONBException when unable to marshall chunk instance to JSON
     * @throws JMSException   when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, Chunk chunk, JobEntity jobEntity) throws JMSException, JSONBException {
        TextMessage message = context.createTextMessage(jsonbContext.marshall(chunk));
        JMSHeader.payload.addHeader(message, JMSHeader.CHUNK_PAYLOAD_TYPE);
        String sink = Optional.ofNullable(jobEntity.getCachedSink()).map(SinkCacheEntity::getSink).map(Sink::getContent).map(SinkContent::getName).orElse(null);
        if(sink != null) JMSHeader.sink.addHeader(message, sink);

        FlowStoreReference flowReference = jobEntity.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW);
        addIdentifiers(message, chunk);
        JMSHeader.flowId.addHeader(message, flowReference.getId());
        JMSHeader.flowVersion.addHeader(message, flowReference.getVersion());
        JMSHeader.additionalArgs.addHeader(message, resolveAdditionalArgs(jobEntity));

        return message;
    }

    /**
     * Builds a json String containing the format and the submitter number
     *
     * @param jobEntity containing the jobSpecification
     * @return jsonString
     */
    private String resolveAdditionalArgs(JobEntity jobEntity) {
        JobSpecification jobSpecification = jobEntity.getSpecification();
        return String.format("{\"format\":\"%s\",\"submitter\":%s}", jobSpecification.getFormat(), jobSpecification.getSubmitterId());
    }
}
