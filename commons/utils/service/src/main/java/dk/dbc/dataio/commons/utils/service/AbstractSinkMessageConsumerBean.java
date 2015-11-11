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

package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

public abstract class AbstractSinkMessageConsumerBean extends AbstractMessageConsumerBean {
    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Unmarshalls payload from given consumed message into ChunkResult
     * @param consumedMessage consumed message
     * @return consumed message payload as ChunkResult
     * @throws NullPointerException if given null-valued consumedMessage
     * @throws InvalidMessageException if message payload type differs from ExternalChunk,
     * if message payload can not be unmarshalled, or if resulting chunk contains no items.
     */
    protected ExternalChunk unmarshallPayload(ConsumedMessage consumedMessage) throws NullPointerException, InvalidMessageException {
        String payloadType = consumedMessage.getHeaderValue(JmsConstants.PAYLOAD_PROPERTY_NAME, String.class);
        if (!JmsConstants.CHUNK_PAYLOAD_TYPE.equals(payloadType)) {
            throw new InvalidMessageException(String.format("Message.headers<%s> payload type %s != %s",
                    consumedMessage.getMessageId(), payloadType, JmsConstants.CHUNK_PAYLOAD_TYPE));
        }
        ExternalChunk processedChunk;
        try {
            processedChunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), ExternalChunk.class);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid ExternalChunk type",
                    consumedMessage.getMessageId()), e);
        }
        if (processedChunk.isEmpty()) {
            throw new InvalidMessageException(String.format("Message<%s> processed chunk payload contains no results",
                    consumedMessage.getMessageId()));
        }
        confirmLegalChunkTypeOrThrow(processedChunk, ExternalChunk.Type.PROCESSED);
        return processedChunk;
    }
}
