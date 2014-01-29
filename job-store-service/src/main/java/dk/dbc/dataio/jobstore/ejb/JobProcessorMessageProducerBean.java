package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;
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

@LocalBean
@Stateless
public class JobProcessorMessageProducerBean {
    public static final String SOURCE_PROPERTY_NAME = "source";
    public static final String SOURCE_PROPERTY_VALUE = "jobstore";
    public static final String PAYLOAD_PROPERTY_NAME = "payload";
    public static final String PAYLOAD_PROPERTY_VALUE = "NewJob";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobProcessorMessageProducerBean.class);

    @Resource
    ConnectionFactory processorQueueConnectionFactory;

    @Resource(name = "processorJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue processorQueue;

    /**
     * Sends given NewJob instance as JMS message with JSON payload to processor queue destination
     *
     * @param newJob NewJob instance to be inserted as message payload
     *
     * @throws NullPointerException when given null-valued argument
     * @throws JobStoreException when unable to send given NewJob to destination
     */
    public void send(NewJob newJob) throws NullPointerException, JobStoreException {
        LOGGER.info("Sending NewJob for job {}", newJob.getJobId());
        try (JMSContext context = processorQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, newJob);
            context.createProducer().send(processorQueue, message);
        } catch (JsonException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending NewJob for job %s",
                    newJob.getJobId());
            throw new JobStoreException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given NewJob instance as JSON payload with
     * header properties '{@value #SOURCE_PROPERTY_NAME}' and '{@value #PAYLOAD_PROPERTY_NAME}'
     * set to '{@value #SOURCE_PROPERTY_VALUE}' and '{@value #PAYLOAD_PROPERTY_VALUE}' respectively.
     *
     * @param context active JMS context
     * @param newJob NewJob instance to be added as payload
     *
     * @return TextMessage instance
     *
     * @throws JsonException when unable to marshall NewJob instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, NewJob newJob) throws JsonException, JMSException {
        final TextMessage message = context.createTextMessage(JsonUtil.toJson(newJob));
        message.setStringProperty(SOURCE_PROPERTY_NAME, SOURCE_PROPERTY_VALUE);
        message.setStringProperty(PAYLOAD_PROPERTY_NAME, PAYLOAD_PROPERTY_VALUE);
        return message;
    }
}
