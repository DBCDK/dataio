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

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;

/**
 * Handles Chunk messages received from the job-store
 */
@MessageDriven
public class JobStoreMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageConsumerBean.class);

    @EJB JobStoreServiceConnectorBean jobStoreServiceConnector;
    @EJB ChunkProcessorBean chunkProcessor;
    @EJB CapacityBean capacityBean;

    JSONBContext jsonbContext = new JSONBContext();

    /**
     * Processes Chunk received in consumed message
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if message payload can not be unmarshalled to chunk instance
     * @throws JobProcessorException on general handling error
     */
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws JobProcessorException, InvalidMessageException {
        try {
            final Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            LOGGER.info("Received chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
            confirmLegalChunkTypeOrThrow(chunk, Chunk.Type.PARTITIONED);
            process(chunk);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), consumedMessage.getHeaderValue(JmsConstants.CHUNK_PAYLOAD_TYPE, String.class)), e);
        }
    }

    private void process(Chunk chunk) throws JobProcessorException {
        final ResourceBundle resourceBundle = getResourceBundle(chunk);
        final Chunk result = callProcessor(chunk, resourceBundle.getFlow(), resourceBundle.getSupplementaryProcessData());
        try {
            jobStoreServiceConnector.getConnector().addChunkIgnoreDuplicates(result, result.getJobId(), result.getChunkId());
        } catch(JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new EJBException(e);
        }
    }

    private Chunk callProcessor(Chunk chunk, Flow flow, SupplementaryProcessData supplementaryProcessData) {
        final StopWatch stopWatch = new StopWatch();
        final Chunk result = chunkProcessor.process(chunk, flow, supplementaryProcessData);
        if (stopWatch.getElapsedTime() > CapacityBean.MAXIMUM_TIME_TO_PROCESS_IN_MILLISECONDS) {
            LOGGER.error("This processor has exceeded its maximum capacity");
            capacityBean.signalCapacityExceeded();
        }
        return result;
    }

    private ResourceBundle getResourceBundle(Chunk chunk) throws JobProcessorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStoreServiceConnector.getConnector().getResourceBundle((int) chunk.getJobId());
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching resources for job %s", chunk.getJobId()), e);
        } finally {
            LOGGER.debug("Fetching resource bundle took {} milliseconds", stopWatch.getElapsedTime());
        }
    }
}
