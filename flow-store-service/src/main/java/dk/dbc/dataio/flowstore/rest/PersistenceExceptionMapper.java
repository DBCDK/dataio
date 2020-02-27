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
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Optional;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceExceptionMapper.class);

    @Override
    public Response toResponse(PersistenceException e) {
        LOGGER.error("Mapping persistence exception", e);

        if (e instanceof OptimisticLockException) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ServiceUtil.asJsonError(e))
                    .build();
        }

        switch (getErrorCode(e).orElse("empty")) {
            case "23503": 	// foreign_key_violation
                return interpretForeignKeyViolation(e);
            case "23505":	// unique_violation
                return Response.status(Response.Status.NOT_ACCEPTABLE)
                        .entity(ServiceUtil.asJsonError(e))
                        .build();
        }

        // Pass through ends up as Internal Server Error 500
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ServiceUtil.asJsonError(e))
                .build();
    }

    private Optional<String> getErrorCode(PersistenceException persistenceException) {
        for (Throwable throwable = persistenceException; throwable != null; throwable = throwable.getCause()) {
            if (throwable instanceof PSQLException) {
                final PSQLException psqlException = (PSQLException) throwable;
                final ServerErrorMessage serverErrorMessage = psqlException.getServerErrorMessage();
                return Optional.ofNullable(serverErrorMessage.getSQLState());
            }
        }
        return Optional.empty();
    }

    private Response interpretForeignKeyViolation(PersistenceException e) {
        if (e.getMessage() != null) {
            if (e.getMessage().contains("is still referenced")) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(ServiceUtil.asJsonError(e))
                        .build();
            } else {
                return Response.status(Response.Status.PRECONDITION_FAILED)
                        .entity(ServiceUtil.asJsonError(e))
                        .build();
            }
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ServiceUtil.asJsonError(e))
                .build();
    }
}
