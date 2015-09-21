/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.ejb;

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
