package dk.dbc.dataio.jobstore.service.ejb.monitoring;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import java.util.Enumeration;
import java.util.Random;

/**
 * Created by ThomasBerg on 24/08/15.
 */
@Stateless
public class JmsEmptyQueuesBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmsEmptyQueuesBean.class);

    @Inject
    private JMSContext ctx;

    @Resource(lookup = JndiConstants.JMS_QUEUE_PROCESSOR)
    private Queue processorQueue;

    @Resource(lookup = JndiConstants.JMS_QUEUE_SINK)
    private Queue sinkQueue;

    public void emptyQueues() {
        LOGGER.info("Emptying queues...");

        final int processorQueueSize = this.getQueueSize(this.processorQueue);

        if(processorQueueSize > 0) {
            this.emptyQueue(processorQueue);
            this.getQueueSize(processorQueue);
        }

        final int sinkQueueSize = this.getQueueSize(this.sinkQueue);
        if(sinkQueueSize > 0) {
            this.emptyQueue(sinkQueue);
            this.getQueueSize(sinkQueue);
        }

        LOGGER.info("Queues are now empty!");
    }
    private void emptyQueue(Queue queueToEmpty) {

        try (QueueBrowser queueBrowser = ctx.createBrowser(queueToEmpty)) {
            LOGGER.info("<- Emptying queue: " + queueToEmpty.getQueueName());

            final JMSConsumer consumer = ctx.createConsumer(queueToEmpty);
            final Enumeration enumeration = queueBrowser.getEnumeration();

            while (enumeration.hasMoreElements()) {
                enumeration.nextElement();
                consumer.receive(1000);
            }
            queueBrowser.close();
        } catch (JMSException e) {
            throw new EJBException("Unable to empty queue.", e);
        }
        LOGGER.info("<- Queues are now empty.");
    }

    private int getQueueSize(Queue queue) {

        try (QueueBrowser queueBrowser = ctx.createBrowser(queue)) {

            int size = 0;
            Enumeration enumeration = queueBrowser.getEnumeration();
            while (enumeration.hasMoreElements()) {
                enumeration.nextElement();
                size++;
            }
            queueBrowser.close();
            LOGGER.info(String.format("Queue named %s has %s messages", queue.getQueueName(), size));
            return size;
        } catch (JMSException e) {
            throw new EJBException(e);
        }
    }


    // Used for test purposes!
    public void addToQueue() {
        LOGGER.info("-> adding 1 message to each queue.");

        int randomNumber = new Random().nextInt(100);
        ctx.createProducer().send(processorQueue, "PROCESSOR message: " + (randomNumber));
        ctx.createProducer().send(sinkQueue, "SINK message: " + (randomNumber));

        this.getQueueSize(processorQueue);
        this.getQueueSize(sinkQueue);
    }
}
