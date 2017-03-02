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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.util.ProcessorShard;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
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
     * @throws NullPointerException when given null-valued argument
     * @throws JobStoreException when unable to send given chunk to destination
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void send(Chunk chunk, JobEntity jobEntity) throws NullPointerException, JobStoreException {
        LOGGER.info("Sending Chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
        final ProcessorShard processorShard = resolveProcessorShard(jobEntity);
        try (JMSContext context = processorQueueConnectionFactory.createContext()) {
            final TextMessage message = createMessage(context, chunk, processorShard);
            context.createProducer().send(processorQueue, message);
        } catch (JSONBException | JMSException e) {
            final String errorMessage = String.format("Exception caught while queueing chunk %s for job %s", chunk.getChunkId(), chunk.getJobId());
            throw new JobStoreException(errorMessage, e);
        }
    }

    /**
     * Creates new TextMessage with given chunk instance as JSON payload with
     * header properties '{@value JmsConstants#SOURCE_PROPERTY_NAME}'
     * and '{@value JmsConstants#PAYLOAD_PROPERTY_NAME}'
     * set to '{@value JmsConstants#JOB_STORE_SOURCE_VALUE}'
     * and '{@value JmsConstants#CHUNK_PAYLOAD_TYPE}' respectively.
     * @param context active JMS context
     * @param chunk chunk instance to be added as payload
     * @param processorShard containing information regarding type of job (acceptance test or business)
     * @return TextMessage instance
     * @throws JSONBException when unable to marshall chunk instance to JSON
     * @throws JMSException when unable to create JMS message
     */
    public TextMessage createMessage(JMSContext context, Chunk chunk, ProcessorShard processorShard) throws JMSException, JSONBException {
        final TextMessage message = context.createTextMessage(jsonbContext.marshall(chunk));
        message.setStringProperty(JmsConstants.SOURCE_PROPERTY_NAME, JmsConstants.JOB_STORE_SOURCE_VALUE);
        message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        message.setStringProperty(JmsConstants.PROCESSOR_SHARD_PROPERTY_NAME, processorShard.toString());
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
}
