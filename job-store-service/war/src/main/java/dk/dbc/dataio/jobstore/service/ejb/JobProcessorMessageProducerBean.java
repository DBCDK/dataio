package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.util.ProcessorShard;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;

@LocalBean
@Stateless
public class JobProcessorMessageProducerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobProcessorMessageProducerBean.class);

    @Resource
    ConnectionFactory processorQueueConnectionFactory;

    @Resource(lookup = "jms/dataio/processor")
    Queue processorQueue;

    JSONBContext jsonbContext = new JSONBContext();

    /**
     * Sends given Chunk instance as JMS message with JSON payload to processor queue destination
     * @param chunk chunk instance to be inserted as message payload
     * @param jobEntity instance to deduct which processor shard should be inserted as message payload
     * @param priority message priority
     * @throws NullPointerException when given null-valued argument
     * @throws JobStoreException when unable to send given chunk to destination
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void send(Chunk chunk, JobEntity jobEntity, int priority) throws NullPointerException, JobStoreException {
        LOGGER.info("Sending chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
        try (JMSContext context = processorQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, chunk, jobEntity);
            final JMSProducer producer = context.createProducer();
            producer.setPriority(priority);
            producer.send(processorQueue, message);
        } catch (JSONBException | JMSException e) {
            final String errorMessage = String.format("Exception caught while queueing chunk %s for job %s", chunk.getChunkId(), chunk.getJobId());
            throw new JobStoreException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given chunk instance as JSON payload
     * with '{@value JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value JmsConstants#JOB_STORE_SOURCE_VALUE}'
     * and '{@value JmsConstants#CHUNK_PAYLOAD_TYPE}' respectively.
     * @param context active JMS context
     * @param chunk chunk instance to be added as payload
     * @param jobEntity to where the chunk instance belongs
     * @return TextMessage instance
     * @throws JSONBException when unable to marshall chunk instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, Chunk chunk, JobEntity jobEntity) throws JMSException, JSONBException {
        final TextMessage message = context.createTextMessage(jsonbContext.marshall(chunk));
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        message.setStringProperty(JmsConstants.PROCESSOR_SHARD_PROPERTY_NAME, resolveProcessorShard(jobEntity).toString());

        final FlowStoreReference flowReference = jobEntity.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW);
        message.setLongProperty(JmsConstants.FLOW_ID_PROPERTY_NAME, flowReference.getId());
        message.setLongProperty(JmsConstants.FLOW_VERSION_PROPERTY_NAME, flowReference.getVersion());

        message.setStringProperty(JmsConstants.ADDITIONAL_ARGS, resolveAdditionalArgs(jobEntity));
        return message;
    }


    /**
     * Deciphers whether the given jobEntity is of type acceptance test or business
     * @param jobEntity current jobEntity
     * @return ProcessorShard for the given jobEntity
     */
    private ProcessorShard resolveProcessorShard(JobEntity jobEntity) {
        if(jobEntity.getSpecification().getType() == JobSpecification.Type.ACCTEST) {
            return new ProcessorShard(ProcessorShard.Type.ACCTEST);
        } else {
            return new ProcessorShard(ProcessorShard.Type.BUSINESS);
        }
    }

    /**
     * Builds a json String containing the format and the submitter number
     * @param jobEntity containing the jobSpecification
     * @return jsonString
     */
    private String resolveAdditionalArgs(JobEntity jobEntity) {
        final JobSpecification jobSpecification = jobEntity.getSpecification();
        return String.format("{\"format\":\"%s\",\"submitter\":%s}", jobSpecification.getFormat(), jobSpecification.getSubmitterId());
    }
}
