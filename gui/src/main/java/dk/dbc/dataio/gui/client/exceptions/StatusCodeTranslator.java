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

import javax.ws.rs.core.Response;

public class StatusCodeTranslator {

    public static ProxyError toProxyError(int statusCode) throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(statusCode);

        final ProxyError errorCode;
        switch (status){
            case NOT_FOUND: errorCode = ProxyError.ENTITY_NOT_FOUND;
                break;
            case CONFLICT: errorCode = ProxyError.CONFLICT_ERROR;
                break;
            case NOT_ACCEPTABLE: errorCode = ProxyError.NOT_ACCEPTABLE;
                break;
            case PRECONDITION_FAILED: errorCode = ProxyError.PRECONDITION_FAILED;
                break;
            case BAD_REQUEST: errorCode = ProxyError.BAD_REQUEST;
                break;
            default:
                errorCode = ProxyError.INTERNAL_SERVER_ERROR;
        }
        return errorCode;
    }
}
