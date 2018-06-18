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

package dk.dbc.dataio.commons.types;

import java.util.Map;

public class ConsumedMessage {
    private final String messageId;
    private final Map<String, Object> headers;
    private final String messagePayload;
    private final Priority priority;

    public ConsumedMessage(String messageId, Map<String, Object> headers, String messagePayload) {
        this(messageId, headers, messagePayload, Priority.NORMAL);
    }

    public ConsumedMessage(String messageId, Map<String, Object> headers,
                           String messagePayload, Priority priority) {
        this.messageId = messageId;
        this.headers = headers;
        this.messagePayload = messagePayload;
        this.priority = priority;
    }

    public String getMessageId() {
        return messageId;
    }

    @SuppressWarnings("unchecked")
    public <T> T getHeaderValue(String headerName, Class<T> returnTypeClass) {
        return returnTypeClass.cast(headers.get(headerName));
    }

    public String getMessagePayload() {
        return messagePayload;
    }

    public Priority getPriority() {
        return priority;
    }
}
