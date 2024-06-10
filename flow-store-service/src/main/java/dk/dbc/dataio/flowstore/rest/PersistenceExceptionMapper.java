package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@SuppressWarnings("java:S2259")
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
            case "23503":    // foreign_key_violation
                return interpretForeignKeyViolation(e);
            case "23505":    // unique_violation
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
                return Optional.ofNullable(serverErrorMessage).map(ServerErrorMessage::getSQLState);
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
