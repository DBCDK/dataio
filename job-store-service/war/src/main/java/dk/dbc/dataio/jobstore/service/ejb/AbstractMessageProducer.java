package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public abstract class AbstractMessageProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageProducer.class);
    private final Function<JobEntity, String> queueNameFromJob;
    protected ConnectionFactory connectionFactory;

    protected AbstractMessageProducer(Function<JobEntity, String> queueNameFromJob) {
        this.queueNameFromJob = queueNameFromJob;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendAbort(JobEntity job) throws JobStoreException {
        String queueName = queueNameFromJob.apply(job);
        try(JMSContext context = connectionFactory.createContext()) {
            TextMessage message = context.createTextMessage();
            JMSHeader.payload.addHeader(message, JMSHeader.ABORT_PAYLOAD_TYPE);
            JMSHeader.abortId.addHeader(message, job.getId());
            LOGGER.warn("Sending abort for job {} to queue {}", job.getId(), queueName);
            send(context, message, job, 9);
        } catch (JMSException e) {
            throw new JobStoreException("Unable to send job abort for " + job.getId() +  " to queue " + queueName, e);
        }
    }

    protected void send(JMSContext context, TextMessage message, JobEntity job, int priority) {
        String qname = queueNameFromJob.apply(job);
        Queue queue = context.createQueue(qname.contains("::") ? qname : qname + "::" + qname);
        JMSProducer producer = context.createProducer();
        producer.setPriority(priority);
        producer.send(queue, message);
    }
}
