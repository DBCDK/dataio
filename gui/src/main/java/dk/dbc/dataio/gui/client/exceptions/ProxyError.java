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

package dk.dbc.dataio.gui.client.exceptions;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum ProxyError implements IsSerializable {
    SERVICE_NOT_FOUND,
    BAD_REQUEST,                // invalid data content
    NOT_ACCEPTABLE,             // violation of unique key contraints
    ENTITY_NOT_FOUND,
    CONFLICT_ERROR,             // Concurrent Update Error
    INTERNAL_SERVER_ERROR,
    MODEL_MAPPER_INVALID_FIELD_VALUE,
    PRECONDITION_FAILED,        // Referenced objects could not be located
    SUBVERSION_LOOKUP_FAILED,   // Error retrieving java scripts
    ERROR_UNKNOWN,              // If the connector throw an unexpected exception
    NO_CONTENT,                 // Successful transaction, but no content results
    FORBIDDEN_SINK_TYPE_TICKLE, // Sink type is invalid for rerun only failed items
    NAMING_ERROR,               // Naming error
    FTP_CONNECTION_ERROR        // Ftp connection error
}