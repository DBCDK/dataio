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

package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
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

/**
 * This Enterprise Java Bean (EJB) functions as JMS message producer for
 * communication going to the sinks
 */
@LocalBean
@Stateless
public class SinkMessageProducerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkMessageProducerBean.class);

    @Resource
    ConnectionFactory sinksQueueConnectionFactory;

    @Resource(name="sinksJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue sinksQueue;

    JSONBContext jsonbContext = new JSONBContext();

    /**
     * Sends given processor result instance as JMS message with JSON payload to sink queue destination
     *
     * @param processedChunk processor result instance to be inserted as JSON string message payload
     * @param destination Sink instance for sink target
     *
     * @throws NullPointerException when given null-valued argument
     * @throws JobProcessorException when unable to send given processor result to destination
     */
    public void send(ExternalChunk processedChunk, Sink destination) throws NullPointerException, JobProcessorException {
        LOGGER.info("Sending processor for chunk {} in job {} to sink {}",
                processedChunk.getChunkId(), processedChunk.getJobId(), destination.getContent().getName());
        try (JMSContext context = sinksQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, processedChunk, destination);
            context.createProducer().send(sinksQueue, message);
        } catch (JSONBException | JMSException e) {
            final String errorMessage = String.format("Exception caught while sending processor result for chunk %d in job %s",
                    processedChunk.getChunkId(), processedChunk.getJobId());
            throw new JobProcessorException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given processor result instance as JSON payload with
     * header properties '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#SOURCE_PROPERTY_NAME}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#PROCESSOR_SOURCE_VALUE}'
     * and '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#CHUNK_PAYLOAD_TYPE}' respectively,
     * and header property '{@value dk.dbc.dataio.commons.types.jms.JmsConstants#RESOURCE_PROPERTY_NAME}'
     * to the resource value contained in given Sink instance.
     *
     * @param context active JMS context
     * @param processedChunk processor result instance to be added as JSON string payload
     * @param destination Sink instance for sink target
     *
     * @return TextMessage instance
     *
     * @throws JSONBException when unable to marshall processor result instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, ExternalChunk processedChunk, Sink destination) throws JMSException, JSONBException {
        final TextMessage message = context.createTextMessage(jsonbContext.marshall(processedChunk));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.PROCESSOR_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        message.setStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME, destination.getContent().getResource());
        return message;
    }
}
