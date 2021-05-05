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

package dk.dbc.dataio.sink.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;

import javax.ejb.EJB;

public abstract class AbstractSinkMessageConsumerBean extends AbstractMessageConsumerBean {
    private final JSONBContext jsonbContext = new JSONBContext();

    @EJB public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    /**
     * Unmarshalls payload from given consumed message into ChunkResult
     * @param consumedMessage consumed message
     * @return consumed message payload as ChunkResult
     * @throws NullPointerException if given null-valued consumedMessage
     * @throws InvalidMessageException if message payload type differs from Chunk,
     * if message payload can not be unmarshalled, or if resulting chunk contains no items.
     */
    protected Chunk unmarshallPayload(ConsumedMessage consumedMessage) throws NullPointerException, InvalidMessageException {
        String payloadType = consumedMessage.getHeaderValue(JmsConstants.PAYLOAD_PROPERTY_NAME, String.class);
        if (!JmsConstants.CHUNK_PAYLOAD_TYPE.equals(payloadType)) {
            throw new InvalidMessageException(String.format("Message.headers<%s> payload type %s != %s",
                    consumedMessage.getMessageId(), payloadType, JmsConstants.CHUNK_PAYLOAD_TYPE));
        }
        Chunk processedChunk;
        try {
            processedChunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type",
                    consumedMessage.getMessageId()), e);
        }
        if (processedChunk.isEmpty()) {
            throw new InvalidMessageException(String.format("Message<%s> processed chunk payload contains no results",
                    consumedMessage.getMessageId()));
        }
        confirmLegalChunkTypeOrThrow(processedChunk, Chunk.Type.PROCESSED);
        return processedChunk;
    }

    protected void uploadChunk(Chunk chunk) throws SinkException {
        final JobStoreServiceConnector jobStoreServiceConnector = jobStoreServiceConnectorBean.getConnector();
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (Exception e) {
            String message = String.format("Error in communication with job-store for chunk %d/%d",
                    chunk.getJobId(), chunk.getChunkId());
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    message += ": job-store returned error '" + jobError.getDescription() + "'";
                }
            }
            throw new SinkException(message, e);
        }
    }
}
