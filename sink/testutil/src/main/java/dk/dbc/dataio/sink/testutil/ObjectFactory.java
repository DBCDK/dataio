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

package dk.dbc.dataio.sink.testutil;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import java.util.HashMap;
import java.util.Map;

public class ObjectFactory {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private ObjectFactory() {}

    public static ConsumedMessage createConsumedMessage(Chunk chunk) {
        try {
            final Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            return new ConsumedMessage("messageId", headers, JSONB_CONTEXT.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ConsumedMessage createConsumedMessage(Chunk chunk, Priority priority) {
        try {
            final Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            return new ConsumedMessage("messageId", headers,
                    JSONB_CONTEXT.marshall(chunk), priority);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
