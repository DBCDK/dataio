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

package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    public static final String UNIQUE_CONSTRAINT_VIOLATION = "duplicate key value violates unique constraint";

    private static final Logger log = LoggerFactory.getLogger(PersistenceExceptionMapper.class);

    @Override
    public Response toResponse(PersistenceException e) {

        log.error("Mapping persistence exception", e);

        if (e instanceof OptimisticLockException) {

            return ServiceUtil.buildResponse(Response.Status.CONFLICT, ServiceUtil.asJsonError(e));

        } else if (e.getMessage() != null) {

            final String message = e.getMessage().toLowerCase();

            if (message.contains(UNIQUE_CONSTRAINT_VIOLATION)) {

                return ServiceUtil.buildResponse(Response.Status.NOT_ACCEPTABLE, ServiceUtil.asJsonError(e));
            }

            if (message.contains("violates foreign key constraint")) {

                return ServiceUtil.buildResponse(Response.Status.CONFLICT, ServiceUtil.asJsonError(e));
            }
        }

        // Running through ends up with 500 / Internal Server Error
        return ServiceUtil.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ServiceUtil.asJsonError(e));
    }

}
